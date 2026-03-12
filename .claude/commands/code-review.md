---
name: code-review
description: 作業完了後にcodexに実装内容をレビュー依頼する
---

You are about to help the user get their completed implementation reviewed by codex.

## Your Task

1. **Verify Current State**
   Check the git state to understand what will be reviewed:
   ```bash
   git status
   git log --oneline -5
   git diff HEAD --stat
   ```

2. **Collect Context**
   - Identify changed files from git state
   - Note what was implemented (from the current conversation context)

3. **Invoke codex review**
   Use `codex review --uncommitted` so that codex fetches the diff itself.

   ```bash
   codex review --uncommitted
   ```

   > **Note**: `--uncommitted` フラグにより codex が自分で git の変更を取得する。
   > Claude側でdiffファイルを作成・受け渡しする必要はない。
   > `--uncommitted` はプロンプト引数と同時使用不可。プロンプトなしで実行すること。

4. **Present Review Results**
   - Show codex's review feedback to the user
   - Mark each finding with current status:
     - 🚨 **ACTIVE**: Critical issue in current code (must fix)
     - ⚠️ **ACTIVE**: Important suggestion
     - 💡 Nice-to-have (optional)
   - Ask the user if they want to address any ACTIVE feedback

5. **Follow-up Actions**
   - If ACTIVE issues are identified, offer to help fix them
   - If changes are made, offer to re-review with codex

## Error Handling

- If Codex CLI is not available, inform the user and suggest installation
- If no uncommitted changes detected (`git diff HEAD` is empty):
  - Notify user: "No uncommitted changes found"
  - Ask if they want to review a specific commit instead (use `git show <hash>` piped to codex)
- If codex times out, retry with a shorter prompt
