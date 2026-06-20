# AI-Assisted Unit Test Generation

This document describes the **Generate Unit Tests** automation: a Python script and GitHub Actions workflow that create JUnit 5 tests for Java sources in `java-app`.

## Overview

```
java-app/src/main/java/     →  scan .java files
        ↓
generate_unit_tests.py      →  call Copilot SDK or OpenAI
        ↓
java-app/src/test/java/     →  write *Test.java files
        ↓
mvn test-compile + mvn test →  verify and retry on failure
        ↓
GitHub Actions              →  open PR with generated tests (optional)
```

The generator is designed for CI use but can also be run locally.

## Components

| File | Role |
|------|------|
| `github-actions/scripts/generate_unit_tests.py` | Main generator script |
| `github-actions/requirements-generate-tests.txt` | Python dependencies (`github-copilot-sdk`, `openai`) |
| `.github/workflows/generate-unit-tests.yml` | GitHub Actions workflow |

## How generation works

For each Java source file under `java-app/src/main/java/`:

1. **Skip** if a matching `*Test.java` already exists (unless `--force` is used).
2. **Collect dependencies** from the same package and referenced project types.
3. **Build an API reference** with:
   - Enum constants (e.g. `Airport.JFK`, `Airport.LAX` — not invented values like `MIA`)
   - Record accessors (e.g. `cruiseFuelLiters()`, not `cruiseFuel()`)
   - Public method signatures
4. **Send a structured prompt** to the AI provider with source code, dependency context, and strict test-writing rules.
5. **Extract Java** from the model response (or accept a file written by the Copilot agent).
6. **Verify** with `mvn test-compile` and `mvn test`.
7. **Retry** up to 2 times on compile or test failures, feeding Maven errors back to the model.

### Prompt rules (high level)

The script instructs the model to:

- Use JUnit 5 and Spring Boot Test where appropriate
- Paste complete Java source in the response (no file-editing tools)
- Use only symbols from the provided API reference
- Use enum constants, never `new` on enums
- Use record accessor names exactly as defined
- Avoid fragile floating-point equality; use `assertEquals(expected, actual, 0.02)` or behavioral assertions
- Not reimplement production rounding logic in tests

## AI providers

### GitHub Copilot SDK (default)

Uses the [GitHub Copilot SDK](https://github.com/github/copilot-sdk) — the same agent runtime as Copilot CLI.

| Environment | Authentication |
|-------------|----------------|
| **Personal repository** | `COPILOT_GITHUB_TOKEN` secret — fine-grained PAT with **Copilot Requests: Read** |
| **Organization repository** | `copilot-requests: write` in workflow permissions (org Copilot billing) |

`GITHUB_TOKEN` alone is **not** sufficient for Copilot inference on personal repos.

### OpenAI (fallback)

Set `OPENAI_API_KEY` as a repository secret and run the workflow with `provider: openai`.

## GitHub Actions workflow

**Workflow name:** Generate Unit Tests  
**File:** `.github/workflows/generate-unit-tests.yml`

### Triggers

- **Manual:** `workflow_dispatch` with inputs:
  - `provider` — `auto`, `copilot`, or `openai`
  - `force` — regenerate even if tests exist
  - `create_pr` — open a PR (or commit directly when `false`)
- **Push to `main`** when files under `java-app/src/main/java/**` change

### Required setup

1. **Secret:** `COPILOT_GITHUB_TOKEN` (personal repos) or org Copilot billing
2. **Optional secret:** `OPENAI_API_KEY`
3. **Repository setting:** Allow GitHub Actions to create and approve pull requests
4. **Optional variables:** `COPILOT_MODEL`, `OPENAI_MODEL`

### Creating the Copilot PAT

1. GitHub → **Settings → Developer settings → Fine-grained tokens**
2. Resource owner: your **personal account**
3. Repository access: this repository only
4. Permission: **Copilot Requests → Read**
5. Add as repository secret: `COPILOT_GITHUB_TOKEN`

## Running locally

### Prerequisites

- Python 3.11+
- Java 17 and Maven on `PATH` (or set `MVN_CMD` on Windows)
- `COPILOT_GITHUB_TOKEN` or `OPENAI_API_KEY`

### Install dependencies

```bash
pip install -r github-actions/requirements-generate-tests.txt
```

### Examples

```bash
# Dry run — preview without writing files
export COPILOT_GITHUB_TOKEN="ghp_..."
python github-actions/scripts/generate_unit_tests.py \
  --java-app-dir java-app \
  --provider copilot \
  --source java-app/src/main/java/com/example/aviation/FuelCalculationService.java \
  --dry-run

# Generate for one class
python github-actions/scripts/generate_unit_tests.py \
  --provider copilot \
  --source java-app/src/main/java/com/example/aviation/FuelCalculationService.java \
  --force

# Generate for all sources missing tests
python github-actions/scripts/generate_unit_tests.py --provider copilot
```

### Windows (PowerShell)

```powershell
$env:COPILOT_GITHUB_TOKEN = "ghp_..."
# Optional on Windows if mvn is not found automatically:
$env:MVN_CMD = "C:\path\to\maven\bin\mvn.cmd"

python github-actions/scripts/generate_unit_tests.py `
  --provider copilot `
  --source java-app/src/main/java/com/example/aviation/FuelCalculationService.java `
  --force
```

### CLI options

| Flag | Description |
|------|-------------|
| `--java-app-dir` | Path to Maven app (default: `java-app`) |
| `--provider` | `auto`, `copilot`, or `openai` |
| `--source` | Generate for specific file(s) only (repeatable) |
| `--force` | Overwrite existing test files |
| `--dry-run` | Do not write files |
| `--no-verify-compile` | Skip `mvn test-compile` |
| `--no-verify-tests` | Skip `mvn test` |
| `--max-retries` | Retries after Maven failures (default: 2) |

## Output

Generated tests are written to:

```
java-app/src/test/java/<package>/<ClassName>Test.java
```

Example: `FuelCalculationService.java` → `FuelCalculationServiceTest.java`

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Empty Copilot response | Set `COPILOT_GITHUB_TOKEN`; `GITHUB_TOKEN` is not enough on personal repos |
| `cannot find symbol` (e.g. `Airport.MIA`) | Re-run with latest script; API reference lists valid enum constants |
| `cannot find symbol` (e.g. `cruiseFuel()`) | Model invented accessor; retry or use `--force` after pulling latest prompt rules |
| `expected X but was Y` (doubles) | Fragile rounding assertion; script now prefers behavioral checks and runs `mvn test` with retry |
| `WinError 2` / Maven not found (Windows) | Set `MVN_CMD` to full path of `mvn.cmd`, or use `--no-verify-compile` |
| PR creation fails | Enable **Allow GitHub Actions to create and approve pull requests** in repo settings |
| Workflow not visible | Workflows must live in `.github/workflows/` at the repository root |

## Example: aviation module

The `com.example.aviation` package is a good candidate for test generation:

- `FuelCalculationService` — fuel calculations for flight departures
- `FlightDeparture` — record with validation
- `Airport`, `AirplaneModel` — enums with fixed constants
- `FuelCalculationResult` — record with fuel metrics

After generation, review assertions and run:

```bash
cd java-app
mvn test
```

## Security notes

- Never commit `COPILOT_GITHUB_TOKEN` or `OPENAI_API_KEY` to the repository.
- Store tokens as GitHub Actions secrets or local environment variables (`.env.local` is gitignored).
- Revoke and rotate tokens if they are exposed in logs or terminal history.
