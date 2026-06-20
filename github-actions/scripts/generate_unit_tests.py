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
- Use only APIs that exist in the related types provided in the prompt.
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


def gather_package_context(source: Path) -> str:
    blocks: list[str] = []
    for related in sorted(source.parent.glob("*.java")):
        if related == source:
            continue
        blocks.append(f"--- {related.name} ---\n{related.read_text(encoding='utf-8')}")
    return "\n\n".join(blocks) if blocks else "(no other types in this package)"


def build_user_prompt(
    source_path: Path,
    source_code: str,
    test_path: Path,
    pom_excerpt: str,
    package_context: str,
) -> str:
    return f"""Generate a JUnit 5 unit test file for the Java class below.

Source file: {source_path.as_posix()}
Target test file: {test_path.as_posix()}

Project context (pom.xml excerpt):
{pom_excerpt}

Related types in the same package (use exact method and field names from these sources):
{package_context}

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
            package_context = gather_package_context(source)
            prompt = build_user_prompt(
                source, source_code, test_path, pom_excerpt, package_context
            )

            print(f"Generating tests for {source.relative_to(repo_root)} ...")

            try:
                if provider == "copilot":
                    token = resolve_github_token()
                    if not token:
                        raise RuntimeError("GitHub token required for Copilot provider")
                    raw = await generate_with_copilot(
                        prompt,
                        token=token,
                        working_directory=str(repo_root),
                        model=copilot_model,
                        timeout_seconds=args.timeout,
                    )
                else:
                    api_key = os.environ.get("OPENAI_API_KEY")
                    if not api_key:
                        raise RuntimeError("OPENAI_API_KEY is required for OpenAI provider")
                    raw = generate_with_openai(
                        prompt,
                        api_key=api_key,
                        model=openai_model,
                        base_url=openai_base_url,
                    )
            except Exception as exc:
                exit_code = fail(f"failed to generate tests for {source}: {exc}", source=source)
                break

            agent_written = False
            test_code = extract_java_source(raw)
            if not test_code:
                written = read_agent_written_test(candidate_test_paths(source, java_app_dir))
                if written:
                    test_path, test_code = written
                    agent_written = True
                    print(f"Using test file written by Copilot agent: {test_path}")
                else:
                    print(raw[:1000], file=sys.stderr)
                    exit_code = fail(
                        f"model response for {source} does not look like Java source",
                        source=source,
                    )
                    break

            if not agent_written:
                write_test_file(test_path, test_code, args.dry_run)
            else:
                print(f"Keeping agent-written file at {test_path}")

            if args.verify_compile and not args.dry_run:
                print("Verifying generated tests compile...")
                compile_ok, compile_output = verify_test_compile(java_app_dir)
                if not compile_ok:
                    print(compile_output[-4000:], file=sys.stderr)
                    exit_code = fail(
                        f"generated tests do not compile for {source}. "
                        "Run locally with: python github-actions/scripts/generate_unit_tests.py "
                        f"--source {source.relative_to(repo_root)} && mvn -f java-app test-compile",
                        source=source,
                    )
                    summary["compile_errors"] = compile_output[-4000:]
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
