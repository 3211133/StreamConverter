# Scripts

This directory contains utility scripts for the StreamConverter project.

## commit-checks.sh

This script consolidates all commit-time checks into a single file. It is used by the Git hooks to ensure code quality and consistent commit messages.

### Usage

The script can be run in three modes:

1. **pre-commit mode**: Checks code style using Spotless
   ```
   ./scripts/commit-checks.sh pre-commit [commit_msg_file]
   ```

2. **commit-msg mode**: Checks commit message format using commitlint
   ```
   ./scripts/commit-checks.sh commit-msg <commit_msg_file>
   ```

3. **all mode**: Runs all checks
   ```
   ./scripts/commit-checks.sh all [commit_msg_file]
   ```

### Parameters

- `mode`: The mode to run the script in (`pre-commit`, `commit-msg`, or `all`)
- `commit_msg_file`: Path to the commit message file (optional for pre-commit mode, required for commit-msg mode)

### Exit Codes

- `0`: All checks passed
- `1`: One or more checks failed

### Integration with Git Hooks

This script is automatically called by the Git hooks in `.githooks/`:

- `.githooks/pre-commit` calls this script in pre-commit mode
- `.githooks/commit-msg` calls this script in commit-msg mode

### Manual Execution

You can also run this script manually to check your code before committing:

```
./scripts/commit-checks.sh all
```

This will run all checks without requiring a commit message file.
