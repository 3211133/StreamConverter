---
name: code-review
description: 作業完了後にcodexに実装内容をレビュー依頼する
---

You are about to help the user get their completed implementation reviewed by codex.

## Your Task

1. **Verify Current State and Identify Review Target**
   **CRITICAL**: Always start by checking the actual git state to avoid reviewing wrong content:

   ```bash
   # Step 1.1: Check current status
   git status

   # Step 1.2: Check recent commits
   git log --oneline -5

   # Step 1.3: Get unstaged/staged changes
   git diff HEAD
   ```

   **Determine what to review:**
   - If `git diff HEAD` shows changes → Review **current uncommitted changes**
   - If no changes but user provides diff → **VERIFY** it matches recent commits
   - If reviewing a specific commit → Use `git show <commit-hash>`

   **⚠️ WARNING**: If user provides a diff that doesn't match current state:
   - Check if it's from an already committed change: `git log --all --source -S"<unique-code-snippet>"`
   - If already committed, inform user and ask if they want to review:
     a) Current uncommitted changes, or
     b) A specific past commit (ask for commit hash)

2. **Collect Implementation Information**
   - Identify changed/new files from verified git state
   - Ask the user for a brief summary of what was implemented
   - Identify any specific areas they want reviewed
   - Confirm the review target with user before proceeding

3. **Prepare Code Review Request**
   Create a comprehensive review request in Japanese that includes:
   - **レビュー対象**: 未コミット変更 / コミット<hash> / その他
   - 実装した機能・修正内容の概要 (Summary of implemented features/fixes)
   - 変更したファイル一覧 (List of changed files)
   - 主要な変更内容の説明 (Description of key changes)
   - 重点的にレビューして欲しい箇所 (Areas requiring focused review)
   - テスト状況 (Testing status)

4. **Generate Review Package**
   - Use mktemp for secure temporary files (recommended) or use timestamped files
   - Save the **VERIFIED** git diff to a temporary file
   - Save the review request summary to a temporary file

5. **Consult with codex**
   Execute codex with the review request using the VERIFIED diff:
   ```bash
   # Use project-local tmp directory for better security
   mkdir -p .claude/tmp
   REVIEW_DIFF=$(mktemp .claude/tmp/code-review-XXXXXX.diff)
   REVIEW_REQUEST=$(mktemp .claude/tmp/code-review-XXXXXX.md)

   # Use verified diff source based on review target
   # For uncommitted changes:
   git diff HEAD --no-color > "${REVIEW_DIFF}"

   # OR for a specific commit:
   # git show <commit-hash> --no-color > "${REVIEW_DIFF}"

   # Build review request using heredoc (avoids command substitution issues)
   cat > "${REVIEW_REQUEST}" << 'EOF'
以下の実装内容についてコードレビューをお願いします。

[Your review summary here - include review target info]

特に以下の観点でレビューしてください：
1. コードの品質と可読性
2. 潜在的なバグやエッジケース
3. パフォーマンスへの影響
4. セキュリティ上の懸念
5. より良い実装方法の提案
6. テストカバレッジの十分性

## 変更内容の差分は別ファイルで提供します
EOF

   # Append diff to request (handles large diffs safely)
   printf "\n## 変更内容の差分:\n" >> "${REVIEW_REQUEST}"
   echo '```diff' >> "${REVIEW_REQUEST}"
   cat "${REVIEW_DIFF}" >> "${REVIEW_REQUEST}"
   echo '```' >> "${REVIEW_REQUEST}"

   # Execute codex with the combined request
   codex exec "$(cat "${REVIEW_REQUEST}")"

   # Cleanup
   rm -f "${REVIEW_DIFF}" "${REVIEW_REQUEST}"
   ```

6. **Verify Review Results Against Current Code**
   **CRITICAL**: After receiving codex feedback, verify against actual code:
   - For each issue codex identifies, check if it exists in current code
   - If issue is in old commit but already fixed, note: "✅ Already fixed in commit <hash>"
   - If issue exists in current code, verify by reading the actual file
   - Provide accurate status for each finding

7. **Present Review Results**
   - Show codex's review feedback to the user
   - **Mark each finding with current status:**
     - 🚨 **ACTIVE**: Critical issue in current code (must fix)
     - ✅ **FIXED**: Already fixed in later commit
     - ⚠️ **ACTIVE**: Important suggestion for current code
     - 💡 Nice-to-have improvements (optional)
   - Ask the user if they want to address any **ACTIVE** feedback before proceeding

8. **Follow-up Actions**
   - If **ACTIVE** issues are identified, offer to help fix them
   - If changes are made, offer to re-review with codex
   - Update the implementation based on feedback if requested

## Quality Standards

- Include complete diff output (not truncated)
- Provide context about the changes (why, not just what)
- Highlight any deviations from project conventions
- Include test results if available
- Be specific about areas of uncertainty

## Important Notes

- This review happens AFTER implementation is complete
- The goal is to catch bugs and improve code quality before merging
- codex may identify issues that were missed during development
- User should address critical issues before creating a PR
- Review feedback should be actionable and specific

## Error Handling

- If Codex CLI is not available, inform the user and suggest installation
- If git diff is too large (>10000 lines), ask user which files to focus on
- If Codex times out, retry with smaller chunks
- **If no changes detected**:
  - Notify user clearly: "No uncommitted changes found"
  - Ask if they want to review a specific commit instead
  - Exit gracefully if user declines
- **If user-provided diff doesn't match current state**:
  - Search for the diff in commit history
  - Inform user which commit it matches (if found)
  - Ask for clarification on what to review
- If temporary file creation fails, report the error clearly
- Temporary files are automatically cleaned up after the review
- If cleanup fails, inform the user of the file locations

## Security Considerations

**⚠️ Important**: Temporary files and diffs may contain sensitive information:
- Code diffs may include proprietary implementation details, API keys, or credentials
- Be cautious when working in shared environments
- **Recommended**: Use project-specific directory `.claude/tmp/` (already in `.gitignore`)
  - Avoids system-wide /tmp which may be shared
  - Easier to audit and cleanup
  - Better isolation for sensitive code
- Review diff content before sharing with external code review services
- Temporary files are automatically removed after review completion
