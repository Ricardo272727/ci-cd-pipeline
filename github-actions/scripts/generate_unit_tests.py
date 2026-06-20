#!/usr/bin/env python3
"""Generate JUnit 5 unit tests for Java sources in java-app.

Primary provider: GitHub Copilot SDK (same agent runtime as Copilot CLI).
Fallback provider: OpenAI Chat Completions API.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

SYSTEM_PROMPT = """You are a senior Java engineer writing unit tests.

Rules:
- Use JUnit 5 (org.junit.jupiter.api) and Spring Boot Test when the class is a Spring component.
- Use Mockito only when mocking is clearly needed.
- Reply with the complete Java test class source in your message.
- Do NOT use edit_file, write, or any other tools — paste the code directly in the response.
- Do NOT add markdown fences or explanations before or after the code.
- Match the package of the source file.
- Name the test class {ClassName}Test.
- Cover public methods with meaningful assertions, not only contextLoads().
- Java records: accessor methods use the exact field names (e.g. cruiseFuelLiters(), not cruiseFuel()).
- Java enums: use enum constants (Airport.JFK), never instantiate enums with new.
- Use only APIs that exist in the API reference section. Do not invent enum constants, method names, or fields.
"""


def find_java_sources(java_app_dir: Path) -> list[Path]:
    main_root = java_app_dir / "src" / "main" / "java"
    if not main_root.is_dir():
        raise FileNotFoundError(f"Java sources not found: {main_root}")
    return sorted(main_root.rglob("*.java"))


def source_to_test_path(source: Path, java_app_dir: Path) -> Path:
    rel = source.relative_to(java_app_dir / "src" / "main" / "java")
    return java_app_dir / "src" / "test" / "java" / rel.parent / f"{rel.stem}Test.java"


def strip_code_fences(text: str) -> str:
    fenced = re.search(r"```(?:java)?\s*\n(.*?)```", text, re.DOTALL | re.IGNORECASE)
    if fenced:
        return fenced.group(1).strip()
    return text.strip()


def looks_like_java_source(text: str) -> bool:
    return "package " in text and "class " in text


def extract_java_source(text: str) -> str | None:
    candidates: list[str] = []

    fenced = re.findall(r"```(?:java)?\s*\n(.*?)```", text, re.DOTALL | re.IGNORECASE)
    candidates.extend(fenced)

    stripped = text.strip()
    if stripped:
        candidates.append(stripped)

    for block in re.findall(
        r"(package\s+[\w.]+;\s*(?:import\s+[\w.*]+;\s*)*.*?class\s+\w+.*)",
        text,
        re.DOTALL,
    ):
        candidates.append(block)

    for candidate in candidates:
        cleaned = candidate.strip()
        if looks_like_java_source(cleaned):
            return cleaned
    return None


def candidate_test_paths(source: Path, java_app_dir: Path) -> list[Path]:
    rel = source.relative_to(java_app_dir / "src" / "main" / "java")
    test_dir = java_app_dir / "src" / "test" / "java" / rel.parent
    stem = rel.stem
    return [
        test_dir / f"{stem}Test.java",
        test_dir / f"{stem}Tests.java",
    ]


def read_agent_written_test(paths: list[Path]) -> tuple[Path, str] | None:
    for path in paths:
        if not path.is_file():
            continue
        content = path.read_text(encoding="utf-8")
        extracted = extract_java_source(content) or content
        if looks_like_java_source(extracted):
            return path, extracted
    return None


def extract_enum_constants(java_source: str, type_name: str) -> list[str]:
    enum_block = re.search(
        rf"(?:public\s+)?enum\s+{re.escape(type_name)}\s*\{{(.*?)^\}}",
        java_source,
        re.DOTALL | re.MULTILINE,
    )
    if not enum_block:
        return []
    constant_section = enum_block.group(1).split(";")[0]
    constants: list[str] = []
    for line in constant_section.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("//"):
            continue
        if stripped.startswith(("private", "public", "protected", "@", "}", ",")):
            continue
        match = re.match(r"(\w+)\s*\(", stripped)
        if match and match.group(1) != type_name:
            constants.append(match.group(1))
    return constants


def extract_record_accessors(java_source: str, type_name: str) -> list[str]:
    match = re.search(
        rf"record\s+{re.escape(type_name)}\s*\((.*?)\)",
        java_source,
        re.DOTALL,
    )
    if not match:
        return []
    fields: list[str] = []
    for part in match.group(1).split(","):
        cleaned = part.strip()
        if not cleaned:
            continue
        fields.append(cleaned.split()[-1])
    return [f"{field}()" for field in fields]


def extract_public_methods(java_source: str, type_name: str) -> list[str]:
    if not re.search(rf"\b(?:class|@Service)\b", java_source) or type_name not in java_source:
        pass
    methods: list[str] = []
    for match in re.finditer(
        r"public\s+(?!class|static\s+class|enum|record|interface)([\w<>,\s\[\]?]+)\s+(\w+)\s*\(",
        java_source,
    ):
        return_type = " ".join(match.group(1).split())
        name = match.group(2)
        if name == type_name:
            continue
        methods.append(f"{return_type} {name}(...)")
    return methods


def describe_type_api(java_path: Path) -> str:
    content = java_path.read_text(encoding="utf-8")
    type_name = java_path.stem
    lines = [f"{type_name} ({java_path.as_posix()}):"]

    if re.search(rf"\benum\s+{re.escape(type_name)}\b", content):
        constants = extract_enum_constants(content, type_name)
        if constants:
            lines.append(f"  enum constants: {', '.join(constants)}")
        lines.append("  usage: Airport.JFK (never invent constants like MIA if not listed)")

    accessors = extract_record_accessors(content, type_name)
    if accessors:
        lines.append(f"  record accessors: {', '.join(accessors)}")

    methods = extract_public_methods(content, type_name)
    if methods:
        lines.append("  public methods:")
        lines.extend(f"    - {method}" for method in methods)

    return "\n".join(lines)


def gather_dependency_sources(source: Path, java_app_dir: Path) -> list[Path]:
    main_root = java_app_dir / "src" / "main" / "java"
    deps: dict[Path, None] = {}

    for related in source.parent.glob("*.java"):
        if related != source:
            deps[related.resolve()] = None

    source_content = source.read_text(encoding="utf-8")
    source_package = ".".join(source.relative_to(main_root).parent.parts)

    for imported in re.findall(r"^import\s+(?:static\s+)?([\w.]+);", source_content, re.MULTILINE):
        if imported.startswith(("java.", "javax.", "jakarta.", "org.junit", "org.springframework")):
            continue
        candidate = main_root / Path(imported.replace(".", "/") + ".java")
        if candidate.is_file():
            deps[candidate.resolve()] = None

    for simple_name in re.findall(r"\b([A-Z][A-Za-z0-9_]*)\b", source_content):
        candidate = source.parent / f"{simple_name}.java"
        if candidate.is_file() and candidate.resolve() != source.resolve():
            deps[candidate.resolve()] = None

    return sorted(deps.keys())


def build_api_reference(dependency_sources: list[Path]) -> str:
    if not dependency_sources:
        return "(no project dependencies detected)"
    return "\n\n".join(describe_type_api(path) for path in dependency_sources)


def gather_dependency_context(dependency_sources: list[Path]) -> str:
    blocks: list[str] = []
    for path in dependency_sources:
        blocks.append(f"--- {path.name} ---\n{path.read_text(encoding='utf-8')}")
    return "\n\n".join(blocks) if blocks else "(no dependency sources)"


def build_user_prompt(
    source_path: Path,
    source_code: str,
    test_path: Path,
    pom_excerpt: str,
    api_reference: str,
    dependency_context: str,
    *,
    compile_errors: str | None = None,
    current_test: str | None = None,
) -> str:
    fix_section = ""
    if compile_errors and current_test:
        fix_section = f"""
The previous test file does not compile. Fix it and return the full corrected Java source.

Maven compilation errors:
{compile_errors}

Broken test file:
{current_test}
"""

    return f"""Generate a JUnit 5 unit test file for the Java class below.

Source file: {source_path.as_posix()}
Target test file: {test_path.as_posix()}

Project context (pom.xml excerpt):
{pom_excerpt}

API reference (use ONLY these symbols; do not invent enum values or method names):
{api_reference}

Dependency source files:
{dependency_context}
{fix_section}
Class under test:
{source_code}
"""


def resolve_maven_executable() -> str:
    override = os.environ.get("MVN_CMD") or os.environ.get("MAVEN_CMD")
    if override:
        return override

    for candidate in ("mvn", "mvn.cmd", "mvn.bat"):
        found = shutil.which(candidate)
        if found:
            return found

    raise FileNotFoundError(
        "Maven (mvn) not found in PATH. Install Maven, add it to PATH, "
        "set MVN_CMD to the full executable path, or use --no-verify-compile."
    )


def verify_test_compile(java_app_dir: Path) -> tuple[bool, str]:
    pom = java_app_dir / "pom.xml"
    try:
        mvn = resolve_maven_executable()
    except FileNotFoundError as exc:
        return False, str(exc)

    result = subprocess.run(
        [mvn, "-B", "test-compile", "-f", str(pom)],
        capture_output=True,
        text=True,
        check=False,
    )
    output = "\n".join(part for part in (result.stdout, result.stderr) if part).strip()
    return result.returncode == 0, output


def read_pom_excerpt(java_app_dir: Path, max_chars: int = 2000) -> str:
    pom = java_app_dir / "pom.xml"
    if not pom.is_file():
        return "(pom.xml not found)"
    content = pom.read_text(encoding="utf-8")
    return content[:max_chars]


async def generate_with_copilot(
    prompt: str,
    *,
    token: str,
    working_directory: str,
    model: str | None,
    timeout_seconds: int,
) -> str:
    from copilot import CopilotClient
    from copilot.session import PermissionHandler
    from copilot.session_events import AssistantMessageData, SessionIdleData

    messages: list[str] = []
    errors: list[str] = []
    done = asyncio.Event()

    def on_event(event) -> None:
        if getattr(event, "type", "") in {"session.error", "error"}:
            errors.append(str(getattr(event, "data", event)))
        match event.data:
            case AssistantMessageData() as data:
                if data.content:
                    messages.append(data.content)
            case SessionIdleData():
                done.set()

    session_kwargs: dict = {
        "on_permission_request": PermissionHandler.approve_all,
        "infinite_sessions": {"enabled": False},
    }
    if model:
        session_kwargs["model"] = model

    async with CopilotClient(
        github_token=token,
        working_directory=working_directory,
        use_logged_in_user=False,
    ) as client:
        async with await client.create_session(**session_kwargs) as session:
            session.on(on_event)
            await session.send(f"{SYSTEM_PROMPT}\n\n{prompt}")
            await asyncio.wait_for(done.wait(), timeout=timeout_seconds)

    if errors:
        raise RuntimeError("; ".join(errors))

    response = "\n".join(messages).strip()
    if not response:
        raise RuntimeError(
            "Copilot returned an empty response. "
            "For personal repositories, set COPILOT_GITHUB_TOKEN to a fine-grained PAT "
            "with Copilot Requests permission (GITHUB_TOKEN alone is usually not enough)."
        )
    return response


def generate_with_openai(
    prompt: str,
    *,
    api_key: str,
    model: str,
    base_url: str | None,
) -> str:
    try:
        from openai import OpenAI
    except ImportError as exc:
        raise RuntimeError(
            "openai package is required for --provider openai. "
            "Install with: pip install openai"
        ) from exc

    client_kwargs: dict[str, str] = {"api_key": api_key}
    if base_url:
        client_kwargs["base_url"] = base_url

    client = OpenAI(**client_kwargs)
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": prompt},
        ],
        temperature=0.2,
    )
    content = response.choices[0].message.content
    return content or ""


def resolve_copilot_token() -> str | None:
    """User PAT for Copilot; required for personal repos in GitHub Actions."""
    return os.environ.get("COPILOT_GITHUB_TOKEN")


def resolve_github_token() -> str | None:
    return (
        resolve_copilot_token()
        or os.environ.get("GITHUB_TOKEN")
        or os.environ.get("GH_TOKEN")
    )


def write_summary(summary_path: Path | None, summary: dict) -> None:
    if not summary_path:
        return
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")


def write_test_file(test_path: Path, content: str, dry_run: bool) -> None:
    test_path.parent.mkdir(parents=True, exist_ok=True)
    if dry_run:
        print(f"[dry-run] Would write {test_path}")
        return
    test_path.write_text(content if content.endswith("\n") else content + "\n", encoding="utf-8")
    print(f"Wrote {test_path}")


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate JUnit 5 unit tests for java-app sources."
    )
    parser.add_argument(
        "--java-app-dir",
        default="java-app",
        help="Path to the Maven Java application (default: java-app)",
    )
    parser.add_argument(
        "--provider",
        choices=("copilot", "openai", "auto"),
        default="auto",
        help="AI provider: copilot (GitHub Copilot SDK), openai, or auto (default)",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Regenerate tests even when the test file already exists",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print actions without writing files",
    )
    parser.add_argument(
        "--source",
        action="append",
        dest="sources",
        help="Generate tests only for this source file (repeatable)",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=300,
        help="Copilot session timeout in seconds (default: 300)",
    )
    parser.add_argument(
        "--output-summary",
        help="Write a JSON summary of generated files to this path",
    )
    parser.add_argument(
        "--verify-compile",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Run mvn test-compile after generating tests (default: enabled)",
    )
    parser.add_argument(
        "--max-retries",
        type=int,
        default=2,
        help="Retries after Maven compile failures (default: 2)",
    )
    return parser.parse_args(argv)


def select_provider(requested: str) -> str:
    if requested != "auto":
        return requested

    if resolve_copilot_token():
        return "copilot"
    if os.environ.get("OPENAI_API_KEY"):
        return "openai"
    if os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN"):
        return "copilot"

    raise RuntimeError(
        "No AI provider configured. Set COPILOT_GITHUB_TOKEN "
        "(recommended for personal repos), OPENAI_API_KEY, or rely on org "
        "Copilot billing via GITHUB_TOKEN with copilot-requests: write."
    )


async def request_generated_source(
    prompt: str,
    *,
    provider: str,
    repo_root: Path,
    copilot_model: str | None,
    openai_model: str,
    openai_base_url: str | None,
    timeout_seconds: int,
) -> str:
    if provider == "copilot":
        token = resolve_github_token()
        if not token:
            raise RuntimeError("GitHub token required for Copilot provider")
        return await generate_with_copilot(
            prompt,
            token=token,
            working_directory=str(repo_root),
            model=copilot_model,
            timeout_seconds=timeout_seconds,
        )

    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is required for OpenAI provider")
    return generate_with_openai(
        prompt,
        api_key=api_key,
        model=openai_model,
        base_url=openai_base_url,
    )


def resolve_generated_test(
    raw: str,
    source: Path,
    java_app_dir: Path,
    test_path: Path,
) -> tuple[Path, str, bool]:
    test_code = extract_java_source(raw)
    if test_code:
        return test_path, test_code, False

    written = read_agent_written_test(candidate_test_paths(source, java_app_dir))
    if written:
        path, code = written
        print(f"Using test file written by Copilot agent: {path}")
        return path, code, True

    raise ValueError("model response does not look like Java source")


async def async_main(argv: list[str]) -> int:
    args = parse_args(argv)
    repo_root = Path.cwd().resolve()
    java_app_dir = (repo_root / args.java_app_dir).resolve()
    summary_path = Path(args.output_summary).resolve() if args.output_summary else None
    summary: dict = {
        "provider": None,
        "generated": [],
        "skipped": [],
        "errors": [],
        "changed": False,
    }
    exit_code = 0

    def fail(message: str, *, source: Path | None = None) -> int:
        entry = {"message": message}
        if source is not None:
            entry["source"] = str(source)
        summary["errors"].append(entry)
        print(f"error: {message}", file=sys.stderr)
        return 1

    try:
        if not java_app_dir.is_dir():
            return fail(f"java-app directory not found: {java_app_dir}")

        provider = select_provider(args.provider)
        summary["provider"] = provider
        print(f"Using provider: {provider}")

        if provider == "copilot" and not resolve_copilot_token():
            print(
                "warning: COPILOT_GITHUB_TOKEN is not set; falling back to GITHUB_TOKEN. "
                "Personal repositories usually need a user PAT with Copilot Requests.",
                file=sys.stderr,
            )

        if args.sources:
            sources = []
            for item in args.sources:
                path = Path(item)
                if not path.is_absolute():
                    path = repo_root / path
                sources.append(path.resolve())
        else:
            sources = find_java_sources(java_app_dir)

        pom_excerpt = read_pom_excerpt(java_app_dir)
        copilot_model = os.environ.get("COPILOT_MODEL", "").strip() or None
        openai_model = os.environ.get("OPENAI_MODEL", "gpt-4.1-mini")
        openai_base_url = os.environ.get("OPENAI_BASE_URL")

        for source in sources:
            if not source.is_file():
                print(f"warning: skipping missing source {source}", file=sys.stderr)
                continue

            test_path = source_to_test_path(source, java_app_dir)
            if test_path.exists() and not args.force:
                print(f"Skipping {source.name}: test already exists at {test_path}")
                summary["skipped"].append(str(test_path.relative_to(repo_root)))
                continue

            source_code = source.read_text(encoding="utf-8")
            dependency_sources = gather_dependency_sources(source, java_app_dir)
            api_reference = build_api_reference(dependency_sources)
            dependency_context = gather_dependency_context(dependency_sources)

            print(f"Generating tests for {source.relative_to(repo_root)} ...")
            print(f"Included {len(dependency_sources)} dependency source(s) in prompt")

            compile_errors: str | None = None
            current_test: str | None = None
            agent_written = False
            test_code = ""

            source_failed = False

            for attempt in range(args.max_retries + 1):
                if attempt > 0:
                    print(f"Retrying generation after compile errors (attempt {attempt + 1})...")

                prompt = build_user_prompt(
                    source,
                    source_code,
                    test_path,
                    pom_excerpt,
                    api_reference,
                    dependency_context,
                    compile_errors=compile_errors,
                    current_test=current_test,
                )

                raw = ""
                try:
                    raw = await request_generated_source(
                        prompt,
                        provider=provider,
                        repo_root=repo_root,
                        copilot_model=copilot_model,
                        openai_model=openai_model,
                        openai_base_url=openai_base_url,
                        timeout_seconds=args.timeout,
                    )
                    test_path, test_code, agent_written = resolve_generated_test(
                        raw, source, java_app_dir, test_path
                    )
                except ValueError:
                    if raw:
                        print(raw[:1000], file=sys.stderr)
                    exit_code = fail(
                        f"model response for {source} does not look like Java source",
                        source=source,
                    )
                    source_failed = True
                    break
                except Exception as exc:
                    exit_code = fail(f"failed to generate tests for {source}: {exc}", source=source)
                    source_failed = True
                    break

                if not agent_written:
                    write_test_file(test_path, test_code, args.dry_run)
                else:
                    print(f"Keeping agent-written file at {test_path}")

                if not args.verify_compile or args.dry_run:
                    break

                print("Verifying generated tests compile...")
                compile_ok, compile_output = verify_test_compile(java_app_dir)
                if compile_ok:
                    break

                compile_errors = compile_output[-4000:]
                current_test = test_code
                if attempt >= args.max_retries:
                    print(compile_output[-4000:], file=sys.stderr)
                    exit_code = fail(
                        f"generated tests do not compile for {source} after "
                        f"{args.max_retries + 1} attempt(s).",
                        source=source,
                    )
                    summary["compile_errors"] = compile_errors
                    source_failed = True
                    break

            if source_failed:
                break

            summary["generated"].append(str(test_path.relative_to(repo_root)))

        summary["changed"] = len(summary["generated"]) > 0
    except Exception as exc:
        exit_code = fail(str(exc))
    finally:
        write_summary(summary_path, summary)
        print(json.dumps(summary))

    return exit_code


def main() -> None:
    raise SystemExit(asyncio.run(async_main(sys.argv[1:])))


if __name__ == "__main__":
    main()
