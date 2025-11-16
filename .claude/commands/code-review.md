---
name: code-review
description: 作業完了後にcodexに実装内容をレビュー依頼する
---

You are about to help the user get their completed implementation reviewed by codex.

## Your Task

1. **Collect Implementation Information**
   - Check git status to identify changed/new files
   - Get git diff to show all changes
   - Ask the user for a brief summary of what was implemented
   - Identify any specific areas they want reviewed

2. **Prepare Code Review Request**
   Create a comprehensive review request in Japanese that includes:
   - 実装した機能・修正内容の概要 (Summary of implemented features/fixes)
   - 変更したファイル一覧 (List of changed files)
   - 主要な変更内容の説明 (Description of key changes)
   - 重点的にレビューして欲しい箇所 (Areas requiring focused review)
   - テスト状況 (Testing status)

3. **Generate Review Package**
   - Use mktemp for secure temporary files (recommended) or use timestamped files
   - Save the git diff to a temporary file
   - Save the review request summary to a temporary file

4. **Consult with codex**
   Execute codex with the review request:
   ```bash
   REVIEW_DIFF=$(mktemp /tmp/code-review-XXXXXX.diff)
   REVIEW_MD=$(mktemp /tmp/code-review-XXXXXX.md)
   git diff --no-color > "${REVIEW_DIFF}"

   # Save review summary to temp file
   cat > "${REVIEW_MD}" << 'SUMMARY'
   [Your review summary here]
SUMMARY

   codex exec "以下の実装内容についてコードレビューをお願いします。

$(cat "${REVIEW_MD}")

## 変更内容の差分:

\`\`\`diff
$(cat "${REVIEW_DIFF}")
\`\`\`

特に以下の観点でレビューしてください：
1. コードの品質と可読性
2. 潜在的なバグやエッジケース
3. パフォーマンスへの影響
4. セキュリティ上の懸念
5. より良い実装方法の提案
6. テストカバレッジの十分性"
   ```

5. **Present Review Results**
   - Show codex's review feedback to the user
   - Highlight any critical issues or important suggestions
   - Categorize feedback:
     - 🚨 Critical issues (must fix)
     - ⚠️ Important suggestions (should consider)
     - 💡 Nice-to-have improvements (optional)
   - Ask the user if they want to address any feedback before proceeding

6. **Follow-up Actions**
   - If issues are identified, offer to help fix them
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
- If no changes detected, notify user and exit gracefully
- If temporary file creation fails, report the error clearly
- Clean up temporary files after use or inform the user of their location

## Security Considerations

**⚠️ Important**: Temporary files and diffs may contain sensitive information:
- Code diffs may include proprietary implementation details, API keys, or credentials
- Be cautious when working in shared environments
- Consider cleaning up temporary files after review: `rm /tmp/code-review-*.{diff,md}`
- Alternatively, use a project-specific directory like `.claude/tmp/` (add to `.gitignore`)
- Review diff content before sharing with external code review services
