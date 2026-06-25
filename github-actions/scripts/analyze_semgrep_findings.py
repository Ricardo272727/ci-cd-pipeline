#!/usr/bin/env python3
"""Analyze Semgrep JSON findings with GitHub Copilot SDK or OpenAI."""

from __future__ import annotations

import argparse
import asyncio
import json
import os
import re
import sys
from pathlib import Path

SYSTEM_PROMPT = """You are an application security engineer reviewing Semgrep results for a Java Spring Boot application.

The input may include:
- Code findings (SAST) from Semgrep Code
- Supply Chain findings (SCA) for vulnerable Maven dependencies, including CVEs, versions, and reachability

Provide a structured security review with these sections:

## Executive summary
- Total findings and breakdown by severity
- Separate counts for code vs dependency (Supply Chain) findings
- Overall risk assessment (low / medium / high)

## Finding analysis
For each finding (or grouped by rule/CVE when repetitive):
- Rule ID, CVE (if any), and location or dependency coordinates
- For Supply Chain: package, version, reachability, and recommended fix version
- Likely true positive vs false positive, with reasoning
- Security impact if exploited
- Concrete remediation steps (bump dependency, code change, etc.)

## Prioritized action items
Numbered list ordered by severity and exploitability. Prioritize reachable dependency CVEs.

Be concise and actionable. Reference specific rule IDs, CVEs, file paths, and line numbers from the input.
If there are no findings, confirm the scan is clean and suggest optional hardening checks.
"""


def load_semgrep_results(path: Path) -> dict:
    if not path.is_file():
        raise FileNotFoundError(f"Semgrep results file not found: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def _metadata_cves(metadata: object) -> list[str]:
    if not isinstance(metadata, dict):
        return []
    cve = metadata.get("cve")
    if isinstance(cve, str):
        return [cve]
    if isinstance(cve, list):
        return [str(item) for item in cve if item]
    return []


def _compact_finding(item: dict) -> dict:
    extra = item.get("extra") or {}
    metadata = extra.get("metadata") or {}
    severity = str(extra.get("severity") or "UNKNOWN").upper()
    sca_info = extra.get("sca_info") or {}
    finding_type = "supply_chain" if sca_info else "code"

    compact: dict = {
        "type": finding_type,
        "check_id": item.get("check_id"),
        "path": item.get("path"),
        "start_line": (item.get("start") or {}).get("line"),
        "end_line": (item.get("end") or {}).get("line"),
        "severity": severity,
        "message": extra.get("message"),
        "cves": _metadata_cves(metadata),
    }

    if finding_type == "code":
        compact["lines"] = extra.get("lines")
    else:
        dependency_match = sca_info.get("dependency_match") or {}
        found = dependency_match.get("found_dependency") or {}
        compact.update(
            {
                "package": found.get("package"),
                "version": found.get("version"),
                "ecosystem": found.get("ecosystem"),
                "transitivity": found.get("transitivity"),
                "lockfile": dependency_match.get("lockfile"),
                "reachable": sca_info.get("reachable"),
                "reachability_rule": sca_info.get("reachability_rule"),
            }
        )
        fix_versions = metadata.get("sca-fix-versions")
        if fix_versions:
            compact["fix_versions"] = fix_versions

    return compact


def summarize_findings(data: dict, max_findings: int) -> dict:
    results = data.get("results") or []
    by_severity: dict[str, int] = {}
    by_type: dict[str, int] = {"code": 0, "supply_chain": 0}
    compact: list[dict] = []

    for item in results:
        extra = item.get("extra") or {}
        severity = str(extra.get("severity") or "UNKNOWN").upper()
        by_severity[severity] = by_severity.get(severity, 0) + 1
        finding_type = "supply_chain" if extra.get("sca_info") else "code"
        by_type[finding_type] = by_type.get(finding_type, 0) + 1

    for item in results[:max_findings]:
        compact.append(_compact_finding(item))

    return {
        "total_findings": len(results),
        "included_in_prompt": len(compact),
        "truncated": len(results) > max_findings,
        "by_severity": by_severity,
        "by_type": by_type,
        "findings": compact,
        "scan_errors": data.get("errors") or [],
        "supply_chain_stats": data.get("supply_chain_stats"),
    }


def format_findings_text(data: dict) -> str:
    summary = summarize_findings(data, max_findings=10_000)
    lines: list[str] = []

    if summary["total_findings"] == 0:
        return "No Semgrep findings."

    lines.append(
        f"Total: {summary['total_findings']} "
        f"(code: {summary['by_type'].get('code', 0)}, "
        f"supply chain: {summary['by_type'].get('supply_chain', 0)})"
    )
    lines.append("")

    for index, finding in enumerate(summary["findings"], start=1):
        header = f"[{index}] {finding['severity']} | {finding['type']} | {finding.get('check_id')}"
        lines.append(header)

        if finding["type"] == "supply_chain":
            pkg = finding.get("package") or "unknown"
            version = finding.get("version") or "unknown"
            lines.append(f"    dependency: {pkg}@{version}")
            if finding.get("cves"):
                lines.append(f"    cve: {', '.join(finding['cves'])}")
            if finding.get("reachable") is not None:
                lines.append(f"    reachable: {finding['reachable']}")
            if finding.get("lockfile"):
                lines.append(f"    lockfile: {finding['lockfile']}")
            if finding.get("fix_versions"):
                lines.append(f"    fix versions: {finding['fix_versions']}")
        else:
            lines.append(f"    path: {finding.get('path')}:{finding.get('start_line')}")

        message = finding.get("message")
        if message:
            lines.append(f"    message: {message}")
        lines.append("")

    if summary["truncated"]:
        lines.append(
            f"(showing first {summary['included_in_prompt']} of {summary['total_findings']} findings)"
        )

    return "\n".join(lines).rstrip()


LOG4J_CORE_PATTERN = re.compile(
    r"(<groupId>org\.apache\.logging\.log4j</groupId>\s*"
    r"<artifactId>log4j-core</artifactId>\s*"
    r"<version>)([^<]+)(</version>)",
    re.MULTILINE,
)
DEMO_LOG4J_COMMENT = re.compile(
    r"\s*<!-- Intentionally vulnerable for Semgrep demo.*?-->\s*",
    re.DOTALL,
)
GENERATE_UNIT_TESTS_OLD = """        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          COPILOT_MODEL: ${{ vars.COPILOT_MODEL }}
          OPENAI_MODEL: ${{ vars.OPENAI_MODEL || 'gpt-4.1-mini' }}
        run: |
          set -euo pipefail

          FORCE_FLAG=""
          if [ "${{ github.event_name }}" = "workflow_dispatch" ] && [ "${{ inputs.force }}" = "true" ]; then
            FORCE_FLAG="--force"
          fi

          PROVIDER="${{ github.event_name == 'workflow_dispatch' && inputs.provider || 'auto' }}"
"""
GENERATE_UNIT_TESTS_NEW = """        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          COPILOT_MODEL: ${{ vars.COPILOT_MODEL }}
          OPENAI_MODEL: ${{ vars.OPENAI_MODEL || 'gpt-4.1-mini' }}
          EVENT_NAME: ${{ github.event_name }}
          INPUT_FORCE: ${{ inputs.force }}
          INPUT_PROVIDER: ${{ github.event_name == 'workflow_dispatch' && inputs.provider || 'auto' }}
        run: |
          set -euo pipefail

          FORCE_FLAG=""
          if [ "${EVENT_NAME}" = "workflow_dispatch" ] && [ "${INPUT_FORCE}" = "true" ]; then
            FORCE_FLAG="--force"
          fi

          PROVIDER="${INPUT_PROVIDER}"
"""
SEMGREP_ANALYZE_OLD = """        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          COPILOT_MODEL: ${{ vars.COPILOT_MODEL }}
          OPENAI_MODEL: ${{ vars.OPENAI_MODEL || 'gpt-4.1-mini' }}
        run: |
          set -euo pipefail

          PROVIDER="${{ github.event_name == 'workflow_dispatch' && inputs.provider || 'auto' }}"
          RESULTS_JSON="${RUNNER_TEMP}/semgrep/semgrep-results.json"
          SUMMARY_JSON="${RUNNER_TEMP}/analysis-summary.json"

          echo "Semgrep findings from previous job: ${{ needs.semgrep-scan.outputs.finding_count }}"
          echo "Supply Chain findings: ${{ needs.semgrep-scan.outputs.supply_chain_count }}"
          echo "Has findings: ${{ needs.semgrep-scan.outputs.has_findings }}"

          set +o pipefail
          python github-actions/scripts/analyze_semgrep_findings.py \\
            --semgrep-results "${RESULTS_JSON}" \\
            --java-app-dir "${JAVA_APP_DIR}" \\
            --provider "${PROVIDER}" \\
            --output-summary "${SUMMARY_JSON}" \\
            | tee "${RUNNER_TEMP}/analysis.log"
"""
SEMGREP_ANALYZE_NEW = """        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          COPILOT_GITHUB_TOKEN: ${{ secrets.COPILOT_GITHUB_TOKEN }}
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          COPILOT_MODEL: ${{ vars.COPILOT_MODEL }}
          OPENAI_MODEL: ${{ vars.OPENAI_MODEL || 'gpt-4.1-mini' }}
          INPUT_PROVIDER: ${{ github.event_name == 'workflow_dispatch' && inputs.provider || 'auto' }}
          SEMGREP_FINDING_COUNT: ${{ needs.semgrep-scan.outputs.finding_count }}
          SEMGREP_SUPPLY_CHAIN_COUNT: ${{ needs.semgrep-scan.outputs.supply_chain_count }}
          SEMGREP_HAS_FINDINGS: ${{ needs.semgrep-scan.outputs.has_findings }}
        run: |
          set -euo pipefail

          PROVIDER="${INPUT_PROVIDER}"
          RESULTS_JSON="${RUNNER_TEMP}/semgrep/semgrep-results.json"
          SUMMARY_JSON="${RUNNER_TEMP}/analysis-summary.json"

          echo "Semgrep findings from previous job: ${SEMGREP_FINDING_COUNT}"
          echo "Supply Chain findings: ${SEMGREP_SUPPLY_CHAIN_COUNT}"
          echo "Has findings: ${SEMGREP_HAS_FINDINGS}"

          set +o pipefail
          python github-actions/scripts/analyze_semgrep_findings.py \\
            --semgrep-results "${RESULTS_JSON}" \\
            --java-app-dir "${JAVA_APP_DIR}" \\
            --provider "${PROVIDER}" \\
            --apply-fixes \\
            --output-summary "${SUMMARY_JSON}" \\
            | tee "${RUNNER_TEMP}/analysis.log"
"""


def _pick_log4j_fix_version(findings: list[dict]) -> str:
    for finding in findings:
        if finding.get("package") != "log4j-core":
            continue
        fix_versions = finding.get("fix_versions")
        if isinstance(fix_versions, list) and fix_versions:
            return str(fix_versions[0])
        if isinstance(fix_versions, dict):
            for versions in fix_versions.values():
                if isinstance(versions, list) and versions:
                    return str(versions[0])
    return "2.17.2"


def _needs_log4j_fix(findings: list[dict]) -> bool:
    for finding in findings:
        if finding.get("type") != "supply_chain":
            continue
        if finding.get("package") == "log4j-core":
            return True
    return False


def _needs_workflow_shell_fix(findings: list[dict], relative_path: str) -> bool:
    normalized = relative_path.replace("\\", "/")
    for finding in findings:
        if finding.get("type") != "code":
            continue
        check_id = str(finding.get("check_id") or "")
        path = str(finding.get("path") or "").replace("\\", "/")
        if "run-shell-injection" not in check_id:
            continue
        if path == normalized or path.endswith("/" + normalized):
            return True
    return False


def fix_log4j_in_pom(pom_path: Path, target_version: str, *, dry_run: bool) -> dict | None:
    if not pom_path.is_file():
        return None
    content = pom_path.read_text(encoding="utf-8")
    if not LOG4J_CORE_PATTERN.search(content):
        return None

    updated = DEMO_LOG4J_COMMENT.sub("\n", content)
    updated, count = LOG4J_CORE_PATTERN.subn(
        rf"\g<1>{target_version}\g<3>",
        updated,
        count=1,
    )
    if count == 0 or updated == content:
        return None

    if not dry_run:
        pom_path.write_text(updated, encoding="utf-8")

    return {
        "file": str(pom_path),
        "action": "upgrade_log4j_core",
        "detail": f"log4j-core -> {target_version}",
    }


def fix_workflow_shell_injection(
    workflow_path: Path,
    repo_root: Path,
    findings: list[dict],
    *,
    dry_run: bool,
) -> list[dict]:
    try:
        relative_path = workflow_path.relative_to(repo_root).as_posix()
    except ValueError:
        return []

    if workflow_path.name == "generate-unit-tests.yml":
        if not _needs_workflow_shell_fix(findings, relative_path):
            return []
        content = workflow_path.read_text(encoding="utf-8")
        if GENERATE_UNIT_TESTS_OLD not in content:
            return []
        updated = content.replace(GENERATE_UNIT_TESTS_OLD, GENERATE_UNIT_TESTS_NEW, 1)
        if updated == content:
            return []
        if not dry_run:
            workflow_path.write_text(updated, encoding="utf-8")
        return [
            {
                "file": str(workflow_path),
                "action": "workflow_shell_injection",
                "detail": "route GitHub context through env vars in generate step",
            }
        ]

    if workflow_path.name == "semgrep-scan.yml":
        if not _needs_workflow_shell_fix(findings, relative_path):
            return []
        content = workflow_path.read_text(encoding="utf-8")
        if SEMGREP_ANALYZE_OLD not in content:
            return []
        updated = content.replace(SEMGREP_ANALYZE_OLD, SEMGREP_ANALYZE_NEW, 1)
        if updated == content:
            return []
        if not dry_run:
            workflow_path.write_text(updated, encoding="utf-8")
        return [
            {
                "file": str(workflow_path),
                "action": "workflow_shell_injection",
                "detail": "route GitHub context through env vars in analyze step",
            }
        ]

    return []


def apply_deterministic_fixes(
    data: dict,
    repo_root: Path,
    *,
    java_app_dir: str,
    dry_run: bool,
) -> list[dict]:
    summary = summarize_findings(data, max_findings=10_000)
    findings = summary["findings"]
    if not findings:
        return []

    applied: list[dict] = []
    seen: set[tuple[str, str]] = set()

    if _needs_log4j_fix(findings):
        pom_path = repo_root / java_app_dir / "pom.xml"
        fix_version = _pick_log4j_fix_version(findings)
        result = fix_log4j_in_pom(pom_path, fix_version, dry_run=dry_run)
        if result:
            key = (result["file"], result["action"])
            if key not in seen:
                seen.add(key)
                applied.append(result)

    workflow_dir = repo_root / ".github" / "workflows"
    for workflow_name in ("generate-unit-tests.yml", "semgrep-scan.yml"):
        workflow_path = workflow_dir / workflow_name
        for fix in fix_workflow_shell_injection(
            workflow_path, repo_root, findings, dry_run=dry_run
        ):
            key = (fix["file"], fix["action"])
            if key not in seen:
                seen.add(key)
                applied.append(fix)

    return applied


def build_user_prompt(summary: dict, java_app_dir: str) -> str:
    payload = json.dumps(summary, indent=2)
    return (
        f"Semgrep scanned the `{java_app_dir}` Java application "
        f"(Semgrep Code + Supply Chain via `semgrep ci`).\n\n"
        f"Findings JSON (compact):\n{payload}\n\n"
        "Analyze these results for the development team."
    )


def resolve_copilot_token() -> str | None:
    return os.environ.get("COPILOT_GITHUB_TOKEN")


def resolve_github_token() -> str | None:
    return (
        resolve_copilot_token()
        or os.environ.get("GITHUB_TOKEN")
        or os.environ.get("GH_TOKEN")
    )


def select_provider(requested: str) -> str:
    if requested != "auto":
        return requested
    if resolve_copilot_token():
        return "copilot"
    if os.environ.get("OPENAI_API_KEY"):
        return "openai"
    if resolve_github_token():
        return "copilot"
    raise RuntimeError(
        "No AI provider configured. Set COPILOT_GITHUB_TOKEN, OPENAI_API_KEY, "
        "or rely on org Copilot billing via GITHUB_TOKEN with copilot-requests: write."
    )


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
            "with Copilot Requests permission."
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
    return response.choices[0].message.content or ""


async def request_analysis(
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


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Analyze Semgrep JSON findings with Copilot or OpenAI."
    )
    parser.add_argument(
        "--semgrep-results",
        required=True,
        help="Path to Semgrep JSON output file",
    )
    parser.add_argument(
        "--java-app-dir",
        default="java-app",
        help="Java application directory scanned by Semgrep (default: java-app)",
    )
    parser.add_argument(
        "--provider",
        choices=("copilot", "openai", "auto"),
        default="auto",
        help="AI provider (default: auto)",
    )
    parser.add_argument(
        "--max-findings",
        type=int,
        default=40,
        help="Maximum findings included in the AI prompt (default: 40)",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=300,
        help="Copilot session timeout in seconds (default: 300)",
    )
    parser.add_argument(
        "--print-findings",
        action="store_true",
        help="Print a human-readable Semgrep summary and exit (no AI analysis)",
    )
    parser.add_argument(
        "--apply-fixes",
        action="store_true",
        help="Apply deterministic fixes for supported Semgrep findings",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="With --apply-fixes, report planned fixes without writing files",
    )
    parser.add_argument(
        "--output-summary",
        help="Write analysis summary JSON to this path",
    )
    return parser.parse_args(argv)


async def async_main(argv: list[str]) -> int:
    args = parse_args(argv)
    repo_root = Path.cwd().resolve()
    results_path = Path(args.semgrep_results).resolve()
    summary_path = Path(args.output_summary).resolve() if args.output_summary else None

    summary: dict = {
        "provider": None,
        "finding_count": 0,
        "supply_chain_count": 0,
        "analysis": None,
        "fixes_applied": [],
        "files_modified": [],
        "changed": False,
        "error": None,
    }

    try:
        raw = load_semgrep_results(results_path)

        if args.print_findings:
            print(format_findings_text(raw))
            return 0

        compact = summarize_findings(raw, args.max_findings)
        summary["finding_count"] = compact["total_findings"]
        summary["supply_chain_count"] = compact["by_type"].get("supply_chain", 0)
        summary["by_severity"] = compact["by_severity"]
        summary["by_type"] = compact["by_type"]

        if args.apply_fixes:
            fixes = apply_deterministic_fixes(
                raw,
                repo_root,
                java_app_dir=args.java_app_dir,
                dry_run=args.dry_run,
            )
            summary["fixes_applied"] = fixes
            summary["files_modified"] = sorted({fix["file"] for fix in fixes})
            summary["changed"] = bool(fixes) and not args.dry_run
            if fixes:
                label = "Planned fixes" if args.dry_run else "Applied fixes"
                print(f"\n--- {label} ---\n")
                for fix in fixes:
                    print(f"- {fix['file']}: {fix['action']} ({fix['detail']})")
            else:
                print("No automatic fixes available for the reported findings.")

        if compact["total_findings"] == 0 and not summary["changed"]:
            print("No findings to analyze.")
            return 0

        provider = select_provider(args.provider)
        summary["provider"] = provider
        print(f"Using provider: {provider}")

        if provider == "copilot" and not resolve_copilot_token():
            print(
                "warning: COPILOT_GITHUB_TOKEN is not set; falling back to GITHUB_TOKEN.",
                file=sys.stderr,
            )

        prompt = build_user_prompt(compact, args.java_app_dir)
        analysis = await request_analysis(
            prompt,
            provider=provider,
            repo_root=repo_root,
            copilot_model=os.environ.get("COPILOT_MODEL", "").strip() or None,
            openai_model=os.environ.get("OPENAI_MODEL", "gpt-4.1-mini"),
            openai_base_url=os.environ.get("OPENAI_BASE_URL"),
            timeout_seconds=args.timeout,
        )
        summary["analysis"] = analysis
        print("\n--- Copilot security analysis ---\n")
        print(analysis)
    except Exception as exc:
        summary["error"] = str(exc)
        print(f"error: {exc}", file=sys.stderr)
        if summary_path:
            summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")
        return 1
    finally:
        if summary_path:
            summary_path.parent.mkdir(parents=True, exist_ok=True)
            summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    return 0


def main() -> None:
    raise SystemExit(asyncio.run(async_main(sys.argv[1:])))


if __name__ == "__main__":
    main()
