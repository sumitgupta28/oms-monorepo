---
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git branch:*), Bash(git push:*), Bash(git checkout:*), Bash(gh pr create:*), Bash(gh pr view:*)
description: Create a pull request for the current branch
argument-hint: "[optional PR title]"
---

## Context

- Current branch: !`git branch --show-current`
- Git status: !`git status`
- Commits ahead of main: !`git log main..HEAD --oneline 2>/dev/null || git log origin/main..HEAD --oneline 2>/dev/null || echo "could not determine"`
- Diff vs main: !`git diff main...HEAD --stat 2>/dev/null || git diff origin/main...HEAD --stat 2>/dev/null || echo "could not determine"`
- Recent commit messages: !`git log main..HEAD --format="%s%n%b" 2>/dev/null | head -40`

## Your task

Create a pull request for the current branch. Follow these steps **in a single message**:

1. **Guard rails** — if the current branch is `main`, stop and tell the user to create a feature branch first. Do not proceed.

2. **Push** — push the current branch to origin (`git push -u origin HEAD`). If the push fails, report the error and stop.

3. **Compose the PR** using all commits ahead of `main`:
   - **Title**: one concise line (≤ 70 chars) summarising *what* changes. Use the argument `$ARGUMENTS` as the title if provided.
   - **Body** (HEREDOC):
     ```
     ## Summary
     • <bullet 1>
     • <bullet 2>
     ...

     ## Affected services / modules
     <list only the Gradle subprojects or react-ui directories that changed>

     ## Test plan
     - [ ] <relevant check 1>
     - [ ] <relevant check 2>

     🤖 Generated with [Claude Code](https://claude.com/claude-code)
     ```
   - Keep the affected services list accurate — check the diff stat to determine which subprojects changed (`order-service`, `payment-service`, `inventory-service`, `product-service`, `agent-service`, `notification-service`, `gateway`, `shared-events`, `react-ui`).

4. **Create the PR** with `gh pr create --title "..." --body "$(cat <<'EOF' ... EOF)"`.

5. **Print the PR URL** returned by `gh pr create`.

Do all of the above in a single message. Do not ask for confirmation unless the branch is `main`.
