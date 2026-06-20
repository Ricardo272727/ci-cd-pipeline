# CI/CD Pipeline Examples

A collection of **GitHub Actions** and **Jenkins** pipeline examples built around a small Spring Boot Java application. Use this repository to compare equivalent CI/CD patterns across both platforms and to experiment with automation such as AI-assisted unit test generation.

## Repository layout

```
ci-cd-pipeline/
├── java-app/                    # Spring Boot 3 demo application (Maven)
├── .github/workflows/           # Active GitHub Actions workflows
├── github-actions/              # Shared scripts, Python tooling, and reference assets
│   ├── scripts/
│   │   ├── deploy.sh
│   │   └── generate_unit_tests.py
│   └── requirements-generate-tests.txt
├── jenkins-pipeline/            # Jenkins equivalent of the GitHub Actions pipelines
│   ├── Jenkinsfile
│   ├── scripts/deploy.sh
│   └── config/
└── docs/
    └── ai-unit-test-generation.md
```

## Sample application (`java-app`)

A minimal **Spring Boot 3.2** app (Java 17, Maven) used as the build target for all pipelines.

| Package | Description |
|---------|-------------|
| `com.example` | Main application entry point and health endpoint |
| `com.example.aviation` | Demo domain: airports, aircraft models, flight departures, and fuel calculation |

Key commands:

```bash
cd java-app
mvn clean test
mvn clean package
```

## GitHub Actions (`.github/workflows/`)

| Workflow | File | Purpose |
|----------|------|---------|
| **Java CI/CD** | `java-ci-cd.yml` | Build, test, package JAR, optional deploy to dev/prod |
| **SonarQube Scan** | `sonarqube-scan.yml` | Run tests and publish analysis to SonarQube / SonarCloud |
| **Generate Unit Tests** | `generate-unit-tests.yml` | AI-assisted JUnit 5 test generation with optional PR |

Workflows are enabled from the repository root `.github/workflows/` directory.

### Required secrets (GitHub Actions)

| Secret | Used by | Description |
|--------|---------|-------------|
| `DEPLOY_SSH_KEY` | Java CI/CD | SSH private key for deployment |
| `DEPLOY_SSH_USER` | Java CI/CD | SSH user for deployment |
| `SONAR_TOKEN` | SonarQube Scan | Sonar authentication token |
| `SONAR_HOST_URL` | SonarQube Scan | Sonar server URL |
| `COPILOT_GITHUB_TOKEN` | Generate Unit Tests | Fine-grained PAT with **Copilot Requests** (personal repos) |
| `OPENAI_API_KEY` | Generate Unit Tests | Optional OpenAI fallback |

### Repository settings

For workflows that open pull requests, enable:

**Settings → Actions → General → Workflow permissions**

- Read and write permissions
- **Allow GitHub Actions to create and approve pull requests**

## Jenkins (`jenkins-pipeline/`)

The `Jenkinsfile` mirrors the GitHub Actions **Java CI/CD** workflow:

- Checkout, compile, test, package
- Archive JAR artifact
- Optional deploy to **dev** or **prod** via SSH

Copy or reference `jenkins-pipeline/Jenkinsfile` in your Jenkins job configuration. Configure Jenkins credentials for `deploy-ssh-user` and `deploy-ssh-key` as described in the file comments.

Environment-specific config files live under `jenkins-pipeline/config/`.

## AI-assisted unit test generation

This repository includes an experimental workflow that generates JUnit 5 tests for Java sources using the **GitHub Copilot SDK** (preferred) or **OpenAI** as a fallback.

See the dedicated guide:

**[docs/ai-unit-test-generation.md](docs/ai-unit-test-generation.md)**

Topics covered:

- How the Python generator works
- Copilot vs OpenAI authentication
- Running locally and in GitHub Actions
- Compile/test verification and automatic retries
- Troubleshooting common failures

## Getting started

### GitHub Actions

1. Push this repository to GitHub.
2. Confirm workflows appear under the **Actions** tab.
3. Add required secrets under **Settings → Secrets and variables → Actions**.
4. Run **Java CI/CD** or **Generate Unit Tests** manually from the Actions UI.

### Jenkins

1. Create a pipeline job pointing at `jenkins-pipeline/Jenkinsfile`.
2. Install JDK 17 and Maven 3.9 on the agent (or use Jenkins tool installers).
3. Configure deploy credentials in Jenkins.
4. Run the job with `DEPLOY_ENV=none` for build-only.

## License

Example code for learning and demonstration purposes.
