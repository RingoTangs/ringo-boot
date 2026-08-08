# Git Commit Message Guidelines

## Purpose

Generate clear, concise, and meaningful Git commit messages based on the current code changes.

Commit messages should help reviewers and future maintainers understand:

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

feat(user): 增加用户资料更新接口

支持用户修改头像、昵称等个人信息，并增加参数校验和异常处理。

Closes #123

---

## Commit Type

Use one of the following types:

- feat: 新功能或新能力
- fix: Bug 修复
- refactor: 代码重构，不改变现有行为
- perf: 性能优化
- docs: 文档修改
- test: 测试相关修改
- build: 构建系统或依赖修改
- ci: CI/CD 配置修改
- chore: 日常维护任务
- style: 代码格式或样式调整
- revert: 回滚之前的提交

---

## Subject Rules

The subject line must:

- Be written in Chinese
- Start with a lowercase type prefix
- Use concise engineering language
- Describe the main purpose of the change
- Not end with a period
- Be no longer than 72 characters

Good examples:

feat(auth): 增加 OAuth 登录支持

fix(api): 修复空参数导致接口异常

refactor(order): 简化订单创建流程

Bad examples:

Added login feature

Fix bug.

update code

修改了一些代码

---

## Scope Rules

The scope should represent the affected module, service, or domain.

Use meaningful scopes:

Examples:

feat(auth): 增加手机号登录能力

fix(payment): 修复支付状态同步问题

refactor(database): 优化查询逻辑

Common scopes:

- auth
- user
- order
- payment
- database
- api
- ui
- config
- build

Avoid meaningless scopes:

- update
- change
- modify
- code

If the change affects multiple unrelated areas, omit the scope.

---

## Language Rules

- Commit messages should be written in Chinese
- Keep technical keywords, framework names, API names, and library names in English
- Use concise and professional engineering language
- Do not use emojis

Examples:

Good:

feat(user): 增加 JWT 登录认证

fix(api): 修复 REST API 参数校验问题

Bad:

feat(user): 增加用户相关功能

fix(api): 优化代码

---

## Body Rules

The body is optional.

Only generate a body when the diff contains meaningful context.

When included:

- Explain why the change was made
- Describe important implementation decisions
- Mention behavior changes or side effects
- Do not simply repeat the subject
- Wrap lines at approximately 100 characters

Good example:

修复 Token 过期后无法自动刷新的问题。

通过增加 refresh token 校验逻辑，避免用户在正常使用过程中
被强制退出登录。

Avoid generic descriptions:

- 优化代码质量
- 提升系统稳定性
- 完善功能
- 重构代码

unless these are explicitly shown in the diff.

---

## Footer Rules

Use footer when necessary.

Examples:

Issue reference:

Closes #123

Breaking change:

BREAKING CHANGE:
修改用户接口返回结构，旧版本客户端需要同步升级。

---

## Breaking Changes

For backward incompatible changes:

Use:

<type>(<scope>): <subject>

BREAKING CHANGE:
Describe the incompatible change.

Example:

feat(api): 修改用户查询接口返回结构

BREAKING CHANGE:
The "name" field has been replaced by "username".

---

## Diff Analysis Rules

Before generating a commit message:

1. Analyze the complete git diff
2. Identify the primary purpose of the change
3. Focus on the user's intent, not individual code changes
4. Ignore generated files and formatting-only changes unless they are the main purpose
5. Prefer the smallest accurate description
6. Do not invent changes that are not present in the diff

---

## Commit Message Quality Rules

The generated commit message should:

- Describe the actual change in the diff
- Avoid vague descriptions
- Avoid unnecessary details
- Avoid repeating file names or implementation details
- Prefer business impact or behavior changes over code-level changes

---

## Output Rules

When generating a commit message:

- Output only the commit message
- Do not add Markdown formatting
- Do not add explanations
- Do not add prefixes like "Commit message:"
- Do not include quotes
