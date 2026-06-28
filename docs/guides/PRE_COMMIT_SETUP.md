# Pre-commit Hook Setup Guide

## Overview

This project includes a comprehensive pre-commit hook that automatically enforces code quality standards before each commit. The hook performs formatting, compilation checks, testing, and common issue detection.

## What the Pre-commit Hook Does

### ✅ Automated Checks

1. **Code Formatting** (Spotless)
   - Automatically formats Java code according to project standards
   - Applies fixes and stages the formatted files
   - Uses Google Java Style Guide conventions

2. **Compilation Verification**
   - Ensures all Java source and test files compile successfully
   - Prevents broken code from being committed

3. **Quick Test Execution**
   - Runs all unit tests (excludes slow integration tests)
   - Ensures no regressions are introduced
   - Reports test failures with helpful messages

4. **Code Quality Checks**
   - Detects TODO/FIXME comments (warns but doesn't block)
   - Identifies console output statements in non-test files
   - Suggests using proper logging instead

5. **Documentation Generation**
   - Runs Javadoc syntax check when Java files are modified（ファイル生成はCIが行う。詳細は [JAVADOC_MANAGEMENT.md](../reference/JAVADOC_MANAGEMENT.md)）

## Installation

The pre-commit hook is automatically available in this repository. It's located at:
```
.git/hooks/pre-commit
```

### Verification

To verify the hook is installed and executable:
```bash
ls -la .git/hooks/pre-commit
```

You should see executable permissions (`-rwxr-xr-x`).

## Usage

### Normal Workflow

The pre-commit hook runs automatically before each commit:

```bash
# Make your changes
git add src/main/java/com/example/MyClass.java

# Commit (hook runs automatically)
git commit -m "feat: add new feature"
```

### Hook Output Example

```
🔍 Running pre-commit checks...
📝 Checking code formatting...
✓ Code formatting is already correct
🔨 Checking compilation...
✓ Compilation successful
🧪 Running quick tests...
✓ All tests passed
🔍 Checking for common issues...
⚠ Found TODO/FIXME comments in staged files
Consider creating issues for these items
📚 Generating Javadoc...
✓ Javadoc generation successful

✓ Pre-commit checks completed successfully!
🚀 Ready to commit
```

### Handling Failures

If the hook fails, you'll see detailed error messages:

#### Compilation Failure
```
✗ Compilation failed
Please fix compilation errors before committing
```

#### Test Failure
```
✗ Tests failed
Please fix failing tests before committing
Run './gradlew test' for detailed test results
```

#### Code Formatting Issues
```
⚠ Code formatting issues found, applying fixes...
✓ Code formatting applied successfully
📦 Adding formatted files to staging area...
✓ Formatted files added to commit
```

## Configuration

### Disabling the Hook Temporarily

For emergency commits, you can bypass the hook:
```bash
git commit --no-verify -m "emergency: fix critical bug"
```

**⚠️ Use sparingly** - This should only be used in genuine emergencies.

### Customizing the Hook

The hook script is located at `.git/hooks/pre-commit`. You can modify it to:
- Add additional checks
- Change timeout values
- Modify warning thresholds

### Performance Tuning

The hook is optimized for speed:
- Compilation check uses Gradle's incremental compilation
- Tests exclude slow integration tests (`-x pitest`)
- Javadoc generation only runs when Java files change

## Troubleshooting

### Hook Not Running

1. **Check permissions**:
   ```bash
   chmod +x .git/hooks/pre-commit
   ```

2. **Verify hook exists**:
   ```bash
   ls -la .git/hooks/pre-commit
   ```

### Hook Running Too Slowly

1. **Clean Gradle cache**:
   ```bash
   ./gradlew clean
   ```

2. **Increase Gradle daemon memory** (in `gradle.properties`):
   ```
   org.gradle.jvmargs=-Xmx2048m
   ```

### False Positives

1. **TODO/FIXME warnings**: These are informational only and don't block commits
2. **Console output warnings**: Consider if proper logging should be used instead

## Integration with IDEs

### IntelliJ IDEA

1. Enable Spotless formatting on save:
   - Settings → Tools → Actions on Save
   - Check "Reformat code" and "Optimize imports"

2. Configure automatic test running:
   - Settings → Build → Build Tools → Gradle
   - Check "Run tests using: Gradle"

### VS Code

1. Install Java Extension Pack
2. Configure format on save in `settings.json`:
   ```json
   {
     "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
     "[java]": {
       "editor.formatOnSave": true
     }
   }
   ```

## Best Practices

1. **Run tests locally** before committing:
   ```bash
   ./gradlew test
   ```

2. **Format code regularly** during development:
   ```bash
   ./gradlew spotlessApply
   ```

3. **Address warnings proactively**:
   - Replace console output with proper logging
   - Create issues for TODO items
   - Update Javadoc when changing public APIs

4. **Commit frequently** with small, focused changes:
   - Faster hook execution
   - Easier debugging when issues arise
   - Better Git history

## Related Commands

```bash
# Manual formatting
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck

# Run all tests
./gradlew test

# Generate Javadoc
./gradlew javadoc

# Full build with all checks
./gradlew build
```

