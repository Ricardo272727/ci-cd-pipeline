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
import sys
from pathlib import Path

SYSTEM_PROMPT = """You are a senior Java engineer writing unit tests.

Rules:
- Use JUnit 5 (org.junit.jupiter.api) and Spring Boot Test when the class is a Spring component.
- Use Mockito only when mocking is clearly needed.
- Output ONLY the complete Java source for the test class — no markdown fences, no explanation.
- Match the package of the source file.
- Name the test class {ClassName}Test.
- Cover public methods with meaningful assertions, not only contextLoads().
- Do not modify or reference files other than the requested test class.
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


def build_user_prompt(
    source_path: Path,
    source_code: str,
    test_path: Path,
    pom_excerpt: str,
) -> str:
    return f"""Generate a JUnit 5 unit test file for the Java class below.

Source file: {source_path.as_posix()}
Target test file: {test_path.as_posix()}

Project context (pom.xml excerpt):
{pom_excerpt}

Source code:
{source_code}
"""


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
    model: str,
    timeout_seconds: int,
) -> str:
    from copilot import CopilotClient
    from copilot.session import PermissionHandler
    from copilot.session_events import AssistantMessageData, SessionIdleData

    messages: list[str] = []
    done = asyncio.Event()

    def on_event(event) -> None:
        match event.data:
            case AssistantMessageData() as data:
                if data.content:
                    messages.append(data.content)
            case SessionIdleData():
                done.set()

    async with CopilotClient(
        github_token=token,
        working_directory=working_directory,
        use_logged_in_user=False,
    ) as client:
        async with await client.create_session(
            on_permission_request=PermissionHandler.approve_all,
            model=model,
            infinite_sessions={"enabled": False},
        ) as session:
            session.on(on_event)
            await session.send(f"{SYSTEM_PROMPT}\n\n{prompt}")
            await asyncio.wait_for(done.wait(), timeout=timeout_seconds)

    return "\n".join(messages)


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


def resolve_github_token() -> str | None:
    return (
        os.environ.get("COPILOT_GITHUB_TOKEN")
        or os.environ.get("GITHUB_TOKEN")
        or os.environ.get("GH_TOKEN")
    )


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
    return parser.parse_args(argv)


def select_provider(requested: str) -> str:
    if requested != "auto":
        return requested

    if resolve_github_token():
        return "copilot"
    if os.environ.get("OPENAI_API_KEY"):
        return "openai"

    raise RuntimeError(
        "No AI provider configured. Set GITHUB_TOKEN / COPILOT_GITHUB_TOKEN "
        "(Copilot SDK) or OPENAI_API_KEY (OpenAI fallback)."
    )


async def async_main(argv: list[str]) -> int:
    args = parse_args(argv)
    repo_root = Path.cwd().resolve()
    java_app_dir = (repo_root / args.java_app_dir).resolve()

    if not java_app_dir.is_dir():
        print(f"error: java-app directory not found: {java_app_dir}", file=sys.stderr)
        return 1

    provider = select_provider(args.provider)
    print(f"Using provider: {provider}")

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
    generated: list[str] = []
    skipped: list[str] = []

    copilot_model = os.environ.get("COPILOT_MODEL", "gpt-4.1")
    openai_model = os.environ.get("OPENAI_MODEL", "gpt-4.1-mini")
    openai_base_url = os.environ.get("OPENAI_BASE_URL")

    for source in sources:
        if not source.is_file():
            print(f"warning: skipping missing source {source}", file=sys.stderr)
            continue

        test_path = source_to_test_path(source, java_app_dir)
        if test_path.exists() and not args.force:
            print(f"Skipping {source.name}: test already exists at {test_path}")
            skipped.append(str(test_path))
            continue

        source_code = source.read_text(encoding="utf-8")
        prompt = build_user_prompt(source, source_code, test_path, pom_excerpt)

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
            print(f"error: failed to generate tests for {source}: {exc}", file=sys.stderr)
            return 1

        test_code = strip_code_fences(raw)
        if "class " not in test_code or "package " not in test_code:
            print(
                f"error: model response for {source} does not look like Java source",
                file=sys.stderr,
            )
            print(raw[:500], file=sys.stderr)
            return 1

        write_test_file(test_path, test_code, args.dry_run)
        generated.append(str(test_path.relative_to(repo_root)))

    summary = {
        "provider": provider,
        "generated": generated,
        "skipped": skipped,
        "changed": len(generated) > 0,
    }

    if args.output_summary:
        summary_path = Path(args.output_summary)
        summary_path.parent.mkdir(parents=True, exist_ok=True)
        summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    print(json.dumps(summary))
    return 0


def main() -> None:
    raise SystemExit(asyncio.run(async_main(sys.argv[1:])))


if __name__ == "__main__":
    main()
