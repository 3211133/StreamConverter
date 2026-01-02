---
name: back-to-develop
description: 作業ブランチから origin/develop に戻る安全なワークフロー
---

You are helping the user safely return to the origin/develop branch from their current working branch.

## Your Task

### 1. **Current State Verification**

First, verify the current git state:

```bash
# Check current branch and status
git status

# Check current branch name
git branch --show-current

# Check if there are uncommitted changes
git diff --stat
git diff --cached --stat
```

### 2. **Handle Uncommitted Changes**

If there are uncommitted changes, present options to the user:

**Options:**
- **Commit changes** - Create a commit with the current changes
- **Stash changes** - Temporarily save changes for later retrieval
- **Discard changes** - Permanently discard all uncommitted changes (⚠️ DANGEROUS)
- **Abort** - Cancel the operation and stay on current branch

Ask the user which option they prefer.

#### Option A: Commit Changes
```bash
# Show what will be committed
git status
git diff --stat

# Add files and commit
git add .
git commit -m "作業中の変更を保存"

# Optional: Push to remote
git push origin <current-branch>
```

#### Option B: Stash Changes
```bash
# Stash with descriptive message (including untracked files)
git stash push -u -m "作業中の変更: $(git branch --show-current) - $(date +%Y%m%d-%H%M%S)"

# Verify stash was created
git stash list
```

#### Option C: Discard Changes
**⚠️ WARNING**: This will permanently delete all uncommitted changes and untracked files!

Ask for explicit confirmation before proceeding:
```bash
# Show what will be discarded (dry-run)
git status
git diff --stat
git clean -n -fd

# After explicit confirmation, discard changes
git reset --hard HEAD
git clean -fd
```

### 3. **Switch to Develop Branch**

Once the working directory is clean:

```bash
# Fetch latest changes from remote
git fetch origin

# Switch to develop branch
git checkout develop

# Update to latest from origin (fast-forward only)
git pull --ff-only origin develop

# Verify current state
git status
git log --oneline -3
```

### 4. **Optional Cleanup**

Ask the user if they want to clean up the old feature branch:

**⚠️ Important**: Only delete the branch if the work is:
- Already merged to develop, OR
- Pushed to remote (so it can be recovered), OR
- Intentionally being discarded

```bash
# Delete local branch (only if safe to do so)
git branch -d <old-branch-name>

# If the branch was not merged, use -D to force delete
git branch -D <old-branch-name>

# Delete remote branch (if desired)
git push origin --delete <old-branch-name>
```

### 5. **Post-Transition Verification**

Verify the transition was successful:

```bash
# Confirm on develop branch
git branch --show-current

# Verify clean working directory
git status

# Check latest commits
git log --oneline -5

# Verify tracking
git branch -vv
```

## Workflow Summary

Display a summary to the user:

```
✅ Successfully returned to develop branch

Current branch: develop
Latest commit: <commit-hash> <commit-message>
Working directory: clean
Tracking: origin/develop

Previous branch: <old-branch-name>
- Changes were: [committed/stashed/discarded]
- Branch still exists: [yes/no]
```

## Error Handling

### Merge Conflicts
If there are merge conflicts when pulling develop:
```bash
# Show conflicting files
git status

# Inform user they need to resolve conflicts
# Offer to abort the merge
git merge --abort
```

### Detached HEAD State
If currently in detached HEAD:
```bash
# Create a branch from current state (if desired)
git checkout -b rescue-branch

# Then proceed with normal workflow
```

### Uncommitted Changes During Switch
If `git checkout` fails due to uncommitted changes:
```bash
# Force stash creation
git stash push -m "Emergency stash before switching to develop"

# Retry checkout
git checkout develop
```

## Important Notes

- **Always verify the current state first** - Don't make assumptions
- **Never discard changes without user confirmation** - Data loss is permanent
- **Stash changes are recoverable** - Use `git stash list` and `git stash apply`
- **Committed changes are safer** - They're part of git history
- **Remote branches can be recovered** - Even if deleted locally

## Safety Checklist

Before executing any destructive operations:
- [ ] User explicitly confirmed the action
- [ ] Current state is clearly communicated
- [ ] Consequences are explained
- [ ] Alternative options were offered
- [ ] Recovery method is available

## Common Use Cases

### Case 1: Clean switch (no uncommitted changes)
```bash
git checkout develop
git pull origin develop
```

### Case 2: Switch with uncommitted work to save
```bash
git stash push -m "WIP: feature/my-feature"
git checkout develop
git pull origin develop
# Later: git stash pop to restore changes
```

### Case 3: Complete feature and switch
```bash
git add .
git commit -m "feat: complete feature implementation"
git push origin feature/my-feature
git checkout develop
git pull origin develop
# Create PR from the feature branch
```

### Case 4: Abandon work and switch
```bash
# ⚠️ After explicit user confirmation
git reset --hard HEAD
git clean -fd
git checkout develop
git pull origin develop
git branch -D feature/abandoned-feature
```

## Recovery Information

If the user needs to recover work:

**Stashed changes:**
```bash
git stash list
git stash show -p stash@{0}
git stash apply stash@{0}
```

**Recent commits (before branch deletion):**
```bash
git reflog
git checkout <commit-hash>
git checkout -b recovery-branch
```

**Deleted branches (if pushed to remote):**
```bash
git fetch origin
git checkout -b <branch-name> origin/<branch-name>
```
