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
- All user-facing documentation should be written in Japanese
- This includes: README.md, guides, tutorials, API documentation, and feature descriptions

### Exceptions: English for AI/Tool Integration
- AI assistant configuration files (e.g., CLAUDE.md) may be written in English
- Tool integration documentation targeting international developer tools may be in English
- Technical specifications intended for automated processing may be in English

### Internationalization (i18n)
- Future internationalization will be handled through machine translation
- We do not currently maintain parallel English documentation
- Contributors are not required to provide translations
- Community-contributed translations are welcome but not required

### Contributing Documentation
When contributing documentation:
1. **New documentation**: Write in Japanese unless it falls under the exceptions above
2. **Existing documentation**: Follow the language of the existing document
3. **Code comments**: English or Japanese is acceptable
4. **Commit messages**: Follow Conventional Commits in English (as per standard practice)

### Rationale
This policy reflects the project's primary user base and development team composition while keeping maintenance costs manageable. Machine translation technology has advanced sufficiently to provide adequate internationalization when needed.

## Pull Request Process

1. Ensure your code follows the style guidelines of this project
2. Update the README.md or documentation with details of changes if appropriate
3. The PR should work with the existing tests and include new tests if adding functionality
4. PRs require approval from at least one maintainer before being merged
