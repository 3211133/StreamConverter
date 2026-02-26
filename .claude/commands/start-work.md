---
name: start-work
description: gitを最新に同期し、issueを確認してブランチを作成し、作業開始状態にする
---

You are helping the user prepare their development environment to start working on a task. This includes syncing git to the latest state, reviewing open issues, and creating a feature branch.

## Your Task

### 1. **Sync Git to Latest State**

First, verify the current git state:

```bash
# Step 1.1: Check current state
git status
git branch --show-current
```

**Handle uncommitted changes if any:**

If there are uncommitted changes (staged, unstaged, or untracked), present options to the user:

#### Option A: コミットする (Recommended for completed work)
```bash
# Show what will be committed
git status
git diff --stat

# Ask user for commit message (Conventional Commits format)
git add .
git commit -m "<type>: <description>"

# Ask if user wants to push
git push origin <current-branch>
```

#### Option B: stashに保存する (Recommended for WIP)
```bash
# Create descriptive stash including untracked files
BRANCH_NAME=$(git branch --show-current)
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
git stash push -u -m "WIP: ${BRANCH_NAME} - ${TIMESTAMP}"

# Verify stash was created
git stash list
```

#### Option C: 破棄する (⚠️ DANGEROUS)
**⚠️ WARNING**: This permanently deletes all uncommitted work!

1. Show exactly what will be discarded (dry-run):
   ```bash
   git status
   git diff
   git clean -n -fd
   ```

2. Ask for EXPLICIT confirmation:
   - "本当に未コミットの変更を破棄してもよろしいですか？"

3. Only after confirmation:
   ```bash
   git reset --hard HEAD
   git clean -fd
   ```

#### Option D: 中止する
Cancel the operation and stay on current branch.

Wait for user's explicit choice before proceeding.

```bash
# Step 1.2: Fetch first, then switch to develop
git fetch origin

# Switch to develop (handles missing local branch)
git checkout develop || git checkout -B develop origin/develop

# Fast-forward to latest
git pull --ff-only origin develop

# Step 1.3: Verify sync succeeded
git log --oneline -5
git branch -vv | grep develop
```

**If pull --ff-only fails (diverged history):**
```bash
# Show the divergence
git log --oneline --left-right develop...origin/develop

# Offer options (ask user to choose):
# 1. git reset --hard origin/develop (⚠️ loses local-only commits - requires confirmation)
# 2. git rebase origin/develop (keep local commits)
#    - If rebase conflicts: git rebase --abort
# 3. Abort and ask user for guidance
```

### 2. **Review Open Issues**

**Preflight check for GitHub CLI:**
```bash
# Check gh authentication before listing issues
gh auth status
```

If `gh` is not authenticated:
- Inform user: "gh CLIの認証が必要です。`gh auth login` を実行してください。"
- Offer to continue without issue selection (manual task entry)

If authenticated, fetch and display open issues:

```bash
# List open issues
gh issue list --state open --limit 20
```

Present the issues to the user in a clear format:

```
📋 未対応のissue一覧:

#<number> <title>
#<number> <title>
...

どのissueに取り組みますか？番号を選択するか、新しいタスクの説明を入力してください。
```

**Options:**
- **既存のissueを選択** - User picks an issue number
- **新しいタスクを入力** - User describes a new task (no existing issue)
- **issueの詳細を確認** - User wants to read issue details before deciding

If user wants issue details:
```bash
gh issue view <issue-number>
```

### 3. **Create Feature Branch**

Based on the selected issue or task:

```bash
# Step 3.1: Determine branch name
# Follow docs/guides/BRANCH_STRATEGY.md naming conventions:
# Pattern: <type>/ISSUE-NUMBER-short-description
# Types: feature/, fix/, docs/, hotfix/, refactor/, chore/
```

**Branch naming rules (per BRANCH_STRATEGY.md):**
- Use lowercase, hyphen-separated words
- Include issue number directly after the type prefix (no `issue-` prefix)
- Keep it concise but descriptive
- Examples:
  - `feature/481-mdc-api-simplification`
  - `fix/146-null-pointer-exception`
  - `docs/427-test-checklist`
  - `hotfix/148-security-vulnerability` (from `main`, not `develop`)

**Base branch selection:**
- `feature/`, `fix/`, `docs/`, `refactor/`, `chore/` → branch from `develop`
- `hotfix/` → branch from `main` (sync `main` instead of `develop`)

Ask the user to confirm or modify the suggested branch name:

```
提案するブランチ名: feature/481-mdc-api-simplification

このブランチ名でよろしいですか？変更したい場合は別の名前を入力してください。
```

**Check if branch already exists before creating:**
```bash
# Exact match check (avoids partial matching)
git show-ref --verify refs/heads/<branch-name> 2>/dev/null

# Also check remote
git show-ref --verify refs/remotes/origin/<branch-name> 2>/dev/null
```

**If branch already exists:**
- Option 1: Switch to existing branch: `git checkout <branch-name>`
- Option 2: Use different name: `git checkout -b <branch-name>-v2`
- Option 3: Delete and recreate (⚠️ after confirmation, check for unpushed work first)

```bash
# Step 3.2: Create and switch to new branch
git checkout -b <branch-name>

# Step 3.3: Verify
git branch --show-current
git log --oneline -1
```

### 4. **Issue Context Setup** (if working on existing issue)

If an existing issue was selected, provide a summary of the issue context:

```bash
# Get full issue details
gh issue view <issue-number>

# Check for related PRs
gh pr list --search "issue:<issue-number>" --state all --limit 5

# Check for related branches
git branch -a | grep -i "<relevant-keywords>"
```

Present a work summary:

```
✅ 作業開始準備が完了しました

ブランチ: <branch-name>
ベース: develop (<latest-commit-hash>)
対象issue: #<number> <title>

📝 issue概要:
<brief summary of the issue>

関連PR: <related PRs if any>

作業を開始してください。完了したら /code-review でレビューを依頼できます。
```

If no existing issue was selected:

```
✅ 作業開始準備が完了しました

ブランチ: <branch-name>
ベース: develop (<latest-commit-hash>)
タスク: <user's task description>

作業を開始してください。完了したら /code-review でレビューを依頼できます。
```

## Error Handling

### git fetch/pull fails
```bash
# Check remote connectivity
git remote -v
git ls-remote origin --heads develop

# If SSH issues:
# Suggest: ssh -T git@github.com
# If HTTPS issues:
# Suggest: gh auth status
```

### gh CLI not available or not authenticated
```bash
# Check gh status (done as preflight in Step 2)
gh auth status

# If not authenticated:
# Inform user: "gh CLIの認証が必要です。gh auth login を実行してください。"
# Offer to continue without issue selection (manual task entry)
```

### Branch name already exists
```bash
# Use exact match (not partial grep)
git show-ref --verify refs/heads/<branch-name> 2>/dev/null

# If exists locally:
# Option 1: Switch to existing branch
git checkout <branch-name>

# Option 2: Use different name
git checkout -b <branch-name>-v2

# Option 3: Delete and recreate (⚠️ after confirmation)
# First check if branch has unpushed work
git log <branch-name> --not --remotes --oneline
# Only after explicit confirmation:
git branch -D <branch-name>
git checkout -b <branch-name>
```

### Pull --ff-only fails (diverged history)
```bash
# This replaces the old "conflict handling" section
# git pull --ff-only cannot produce merge conflicts
# If it fails, the history has diverged

# Show divergence
git log --oneline --left-right develop...origin/develop

# Options:
# 1. Reset to remote (⚠️ loses local commits, requires confirmation)
# 2. Rebase local commits (if rebase fails: git rebase --abort)
# 3. Ask user for guidance
```

## Communication Style

Use clear, concise Japanese:

```
🔍 現在の状態を確認中...
🔄 developブランチを最新に同期中...
📋 未対応のissueを取得中...
✅ 作業開始準備が完了しました
⚠️ 未コミットの変更があります
```

## Important Notes

- **Always sync to latest develop** (or `main` for hotfixes) before creating a new branch
- **Never skip issue review** - Present issues even if user has a task in mind (they might find a matching issue)
- **Branch names must follow BRANCH_STRATEGY.md** - Use `<type>/ISSUE-NUMBER-description` format
- **Confirm branch name with user** - Don't create without approval
- **Handle errors gracefully** - Always provide recovery options
- **Mention next steps** - Remind about `/plan-review` for complex tasks and `/code-review` for completion

## Integration with Other Skills

- Suggest `/plan-review` if the task is complex and would benefit from Codex consultation
- Remind about `/code-review` at the end for post-implementation review
- This skill handles the quick-start workflow; for advanced branch operations (cleanup, multi-branch switching, conflict resolution), use the `branch-manager` agent
- Uncommitted change handling follows the same patterns as `back-to-develop` and `branch-manager` for consistency
