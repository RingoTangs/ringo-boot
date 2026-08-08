# Git Commit Message Guidelines

## Purpose

Generate clear, concise, and meaningful Git commit messages based on the current code changes.

Commit messages must help reviewers and future maintainers understand:

- What changed
- Why the change was made
- The impact of the change

---

## Commit Message Format

Use Conventional Commits format:

<type>(<scope>): <subject>

<body>

<footer>

Example:

feat(user): add user profile update API

Allow users to update their profile information through the REST API.
Add validation for required fields and improve error handling.

Closes #123

---

## Commit Type

Use one of the following types:

- feat: A new feature or capability
- fix: A bug fix
- refactor: Code restructuring without behavior changes
- perf: Performance improvement
- docs: Documentation changes
- test: Adding or updating tests
- build: Build system or dependency changes
- ci: CI/CD configuration changes
- chore: Maintenance tasks
- style: Code formatting or style-only changes
- revert: Revert a previous commit

---

## Subject Rules

The subject line must:

- Be written in English
- Use imperative mood
- Start with a lowercase letter
- Not end with a period
- Be no longer than 72 characters
- Clearly describe the main change

Good examples:

feat(auth): add oauth login support

fix(api): handle empty request parameters

refactor(order): simplify payment validation flow

Bad examples:

Added login feature

Fix bug.

update code

---

## Body Rules

The body is optional.

When included:

- Explain WHY the change was made
- Explain important implementation decisions
- Mention limitations or side effects if needed
- Do not simply repeat the subject
- Wrap lines at approximately 100 characters

Example:

Improve token refresh handling to prevent users from being logged out
when access tokens expire during active sessions.

---

## Scope Rules

Use a meaningful scope based on the affected module.

Examples:

feat(auth): ...
fix(payment): ...
refactor(database): ...
docs(readme): ...

If the change affects multiple areas, omit the scope.

---

## Language Rules

- Subject must be written in Chinese
- Keep technical keywords in English
- Use concise engineering language

Example:

feat(用户中心): 增加手机号登录能力

支持手机号验证码登录流程，并完善异常处理。

---

## Diff Analysis Rules

Before generating a commit message:

1. Analyze the complete git diff
2. Identify the primary purpose of the change
3. Ignore generated files and formatting-only changes unless they are the main purpose
4. Prefer the smallest accurate description
5. Do not invent changes that are not present in the diff

---

## Output Rules

When generating a commit message:

- Output only the commit message
- Do not add Markdown formatting
- Do not add explanations
- Do not include quotes
