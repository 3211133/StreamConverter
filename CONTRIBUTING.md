# Contributing to StreamConverter

Thank you for your interest in contributing to StreamConverter! This document outlines the guidelines for contributing to this project.

## Workflow

- Please start any changes or proposals by opening an Issue or a Merge Request.
- Leave discussions and decisions in the comments or discussion threads to keep a traceable history.

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for our commit messages. This leads to more readable messages that are easy to follow when looking through the project history.

### Commit Message Format

Each commit message consists of a **header**, a **body**, and a **footer**. The header has a special format that includes a **type**, an optional **scope**, and a **subject**:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

#### Type

Must be one of the following:

* **feat**: A new feature
* **fix**: A bug fix
* **docs**: Documentation only changes
* **style**: Changes that do not affect the meaning of the code (white-space, formatting, etc)
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **perf**: A code change that improves performance
* **test**: Adding missing tests or correcting existing tests
* **build**: Changes that affect the build system or external dependencies
* **ci**: Changes to our CI configuration files and scripts
* **chore**: Other changes that don't modify src or test files

#### Scope

The scope is optional and should be the name of the module affected (as perceived by the person reading the changelog).

#### Subject

The subject contains a succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize the first letter
* no dot (.) at the end

#### Body

The body should include the motivation for the change and contrast this with previous behavior.

#### Footer

The footer should contain any information about **Breaking Changes** and is also the place to reference GitHub issues that this commit **Closes**.

### Examples

```
feat(validator): add option to specify output format

Add a new option to the XML validator that allows specifying the output format.
This makes it easier to integrate with other tools.

Closes #123
```

```
fix(converter): handle UTF-8 BOM correctly

Previously, files with UTF-8 BOM would cause conversion errors.
This fix properly detects and handles the BOM.

Fixes #456
```

## Code Style

This project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format to maintain consistent code style. Before submitting a pull request, please run:

```
./gradlew spotlessApply
```

This will automatically format your code according to the project's style guidelines.

## Documentation Language Policy

This project adopts a **Japanese-first documentation strategy** with the following guidelines:

### Primary Language: Japanese (日本語)
- "User-facing documentation" includes (but is not limited to): README.md, usage guides, tutorials, API documentation, and feature descriptions
- It does **not** include developer-oriented files such as CONTRIBUTING.md, changelogs, or internal technical notes unless otherwise specified

### Exceptions: English for AI/Tool Integration
- AI assistant configuration files (e.g., CLAUDE.md) may be written in English
- Tool integration documentation targeting international developer tools may be in English
- Technical specifications intended for automated processing may be in English

### Internationalization (i18n)
- Future internationalization will be handled through machine translation
- We do not currently maintain parallel English documentation
- Contributors are not required to provide translations
- Community-contributed translations are welcome but not required

#### Machine Translation Implementation Details
- **Process**: Machine translation will be performed manually by project maintainers as needed (e.g., for major releases or significant documentation updates). There is currently no automated translation pipeline.
- **Responsibility**: The project maintainers are responsible for initiating and reviewing machine translations. Contributors may suggest or request translations, but are not obligated to provide them.
- **Triggers**: Translations may be generated when there is significant demand from the community, upon major documentation changes, or when requested by contributors or users.
- **Community Involvement**: Community-contributed translations (manual or via machine translation) are welcome. If you wish to contribute a translation, please open an Issue or Pull Request for discussion and review.
- **Review**: All translations, whether machine-generated or community-contributed, will be reviewed by maintainers for accuracy and clarity before being merged.

### Contributing Documentation
When contributing documentation:
1. **New documentation**: Write in Japanese unless it falls under the exceptions above
2. **Existing documentation**: Follow the language of the existing document
3. **Code comments**: English or Japanese is acceptable
4. **Commit messages**: Follow Conventional Commits in English (as per standard practice)

### Test Naming Convention
- **`@DisplayName` annotations**: Use Japanese for all test display names to match the project's primary language policy. Example: `@DisplayName("execute：有効な入力を処理して出力を返す")`
- **Method names**: Use English (e.g., `testExecuteWithValidInput()`), as Java identifiers are conventionally English
- When adding tests to a file that already has `@DisplayName` annotations in English, migrate them to Japanese as part of the change, or open a follow-up issue to track the migration

### Rationale
This policy reflects the project's primary user base and development team composition while keeping maintenance costs manageable. Machine translation technology has advanced sufficiently to provide adequate internationalization when needed.

## Implementing a New Command

When adding a new `IStreamCommand` implementation, you also need to register it with the
streaming contract test suite. See
[docs/reference/IMPLEMENTING_COMMANDS.md](docs/reference/IMPLEMENTING_COMMANDS.md)
for a step-by-step guide including a provider template and common pitfalls.

## Local Environment Setup

### Personal Ignore Settings

IDE- or editor-specific files that are personal to your environment should **not** be added to `.gitignore` in this repository. Instead, use one of the following mechanisms:

- **`.git/info/exclude`** — applies only to your local clone of this repository
- **`~/.gitignore_global`** — applies across all repositories on your machine; configure with:
  ```
  git config --global core.excludesfile ~/.gitignore_global
  ```

Example entries for a global ignore file:

```
# JetBrains IDEs
.idea/
*.iml

# macOS
.DS_Store

# Windows
Thumbs.db
```

### AI Assistant Files

This repository includes configuration files for AI coding assistants. The following are project assets and are tracked in git:

| File / Directory | Purpose |
|---|---|
| `CLAUDE.md` | Project conventions for Claude Code |
| `AGENTS.md` | Entry point for AI agents (points to CLAUDE.md) |
| `.claude/settings.json` | Shared Claude Code permission settings |
| `.claude/agents/` | Project-specific agent definitions |
| `.claude/commands/` | Project-specific slash command definitions |

Personal customizations (e.g., additional agents or commands for your own workflow) should be placed in `.claude/settings.local.json`, which is excluded from git via `.gitignore`.

## Pull Request Process

1. Ensure your code follows the style guidelines of this project
2. Update the README.md or documentation with details of changes if appropriate
3. The PR should work with the existing tests and include new tests if adding functionality
4. PRs require approval from at least one maintainer before being merged
