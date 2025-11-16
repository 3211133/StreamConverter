---
name: plan-review
description: 作業開始前にcodexと方針を相談して、技術的アプローチを確認する
---

You are about to help the user validate their implementation plan by consulting with codex before starting work.

## Your Task

1. **Gather Planning Information**
   - Ask the user to describe what they want to implement
   - Ask about their proposed technical approach
   - Identify any concerns or design decisions that need validation

2. **Prepare Review Request**
   Create a comprehensive review request in Japanese that includes:
   - 実装予定の機能・修正内容 (What will be implemented)
   - 提案する技術的アプローチ (Proposed technical approach)
   - 使用予定の技術・ライブラリ (Technologies/libraries to be used)
   - 検討事項・懸念点 (Considerations and concerns)
   - 代替案の有無 (Alternative approaches if any)

3. **Consult with Codex**
   - Use mktemp for secure temporary file (recommended)
   - Save the review request to a temporary file
   - Execute codex with the review request
   ```bash
   PLAN_FILE=$(mktemp /tmp/plan-review-XXXXXX.md)
   cat > "${PLAN_FILE}" << 'PLAN'
   [Your plan review request here]
PLAN

   codex exec "以下の実装計画についてレビューしてください。特に、技術的アプローチの妥当性、潜在的な問題点、より良い代替案があれば教えてください。

$(cat "${PLAN_FILE}")"
   ```
   - Wait for Codex's response

4. **Present Feedback**
   - Show Codex's feedback to the user
   - Highlight any concerns or suggestions raised by Codex
   - Ask the user if they want to proceed, adjust the approach, or have further discussion with Codex

## Error Handling

- If Codex CLI is not available, inform the user and suggest installation
- If the command times out, offer to retry with a shorter review request
- If temporary file creation fails, report the error clearly
- Clean up temporary files after use or inform the user of their location

## Security Considerations

**⚠️ Important**: Temporary files may contain sensitive information from your codebase:
- Review requests may include proprietary implementation details
- Be cautious when working in shared environments
- Consider cleaning up temporary files after review: `rm /tmp/plan-review-*.md`
- Alternatively, use a project-specific directory like `.claude/tmp/` (add to `.gitignore`)

## Quality Standards

- Use clear, professional technical Japanese when communicating with codex
- Be specific about technical details (versions, patterns, constraints)
- Include relevant context about the StreamConverter project architecture
- Reference existing code patterns when applicable
- Ask focused questions that will yield actionable feedback

## Important Notes

- This consultation happens BEFORE implementation begins
- The goal is to catch design issues early
- codex may suggest better approaches or identify risks
- User should feel confident about their approach before coding starts
- If codex raises concerns, discuss with user before proceeding
