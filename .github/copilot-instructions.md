# Git Commit Message Guidelines

## Purpose

Generate Git commit messages based on the current code changes.

The commit message must be:

- Accurate
- Concise
- Easy to understand
- Written for Chinese-speaking engineering teams

The commit message should explain:

- 修改了什么
- 为什么修改
- 对系统行为有什么影响

---

## Commit Message Format

Use Conventional Commits format:

<type>(<scope>): <subject>

<body>

<footer>

Format rules:

- type 必须使用英文
- scope 使用英文
- subject 必须使用中文
- body 必须使用中文
- footer 根据需要使用英文关键字（例如 Closes、BREAKING CHANGE）

Example:

feat(user): 增加用户资料更新接口

支持用户修改头像、昵称等个人信息，并增加参数校验和异常处理。

Closes #123

---

## Language Rules (IMPORTANT)

The commit message language rules are mandatory:

- Subject 必须使用中文
- Body 必须使用中文
- 禁止生成英文 subject
- 禁止生成英文 body
- 技术名称、框架名称、类名、API 名称可以保留英文

Allowed examples:

feat(auth): 增加 OAuth 登录支持

fix(api): 修复 REST API 参数校验异常

refactor(database): 优化 MySQL 查询逻辑

Not allowed:

feat(auth): add oauth login support

fix(api): fix parameter validation issue

refactor(database): improve query performance

---

## Commit Type

Allowed commit types:

- feat: 新功能
- fix: Bug 修复
- refactor: 重构代码
- perf: 性能优化
- docs: 文档修改
- test: 测试修改
- build: 构建相关修改
- ci: CI/CD 修改
- chore: 日常维护
- style: 格式调整
- revert: 回滚提交

---

## Subject Rules

Subject 必须满足：

- 使用中文描述
- 简洁描述主要变更
- 不超过 72 个字符
- 不以句号结尾
- 不包含无意义描述

推荐：

feat(user): 增加手机号登录功能

fix(order): 修复订单状态更新异常

refactor(cache): 优化 Redis 缓存逻辑

禁止：

feat(user): add user login

fix(order): fix bug

chore: update code

---

## Scope Rules

scope 表示受影响的模块、领域或服务。

推荐：

feat(auth): 增加 JWT 登录认证

fix(payment): 修复支付状态同步问题

refactor(order): 简化订单创建流程

常用 scope:

- auth
- user
- order
- payment
- database
- api
- ui
- config
- build

避免：

- update
- change
- modify
- code

---

## Body Rules

Body 是可选的。

只有当 diff 包含重要上下文时才生成 body。

Body 应说明：

- 修改原因
- 重要设计决策
- 行为变化
- 兼容性影响

不要生成：

- 优化代码质量
- 提升系统稳定性
- 完善功能
- 重构代码

除非 diff 明确体现这些目的。

示例：

修复 Token 过期后无法自动刷新的问题。

增加 refresh token 校验逻辑，避免用户在正常使用过程中
被强制退出登录。

---

## Breaking Changes

如果存在不兼容修改：

格式：

<type>(<scope>): <subject>

BREAKING CHANGE:
说明不兼容内容

示例：

feat(api): 修改用户查询接口返回结构

BREAKING CHANGE:
用户接口返回字段 name 已替换为 username。

---

## Diff Analysis Rules

生成 commit message 前：

1. 分析完整 git diff
2. 判断代码修改的主要目的
3. 优先描述业务行为变化
4. 忽略自动生成文件和纯格式修改
5. 不要编造 diff 中不存在的信息
6. 不要简单复制代码文件名或方法名

---

## Output Rules (IMPORTANT)

生成 commit message 时：

必须：

- 只输出 commit message
- 使用中文
- 保持 Conventional Commits 格式

禁止：

- 输出解释文字
- 输出 Markdown
- 输出代码块
- 输出 "Commit message:"
- 使用引号包裹内容
- 生成英文描述
