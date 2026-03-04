# Fleets 项目 Agent Skills

本目录为 **Fleets** 工作区的项目级 Agent Skills，仅在本仓库内生效。Agent 会根据描述自动匹配并应用对应技能。

## 本项目技能

| 技能目录 | 说明 |
|----------|------|
| [fleets-auth-and-exception](./fleets-auth-and-exception/) | Sa-Token 鉴权约定、统一异常与错误码（ErrorCode、BusinessException、Assert、GlobalExceptionHandler） |
| [fleets-mongodb-persistence](./fleets-mongodb-persistence/) | MongoDB 持久层约定：Repository 命名、实体与 DTO、同步/Reactive 使用场景 |

## 外部技能来源（推荐自行安装）

需要 **Spring Boot 通用**、**Vue**、**测试/代码审查** 等技能时，可从以下渠道获取并安装到本目录或全局 `~/.cursor/skills/`：

### 1. Agent Skills 市场（首选）

- **网址**: https://agentskillsrepo.com/
- **用法**: 语义/关键词搜索（如 `java`、`spring boot`、`REST API`、`testing`、`vue`、`vue3`），按分类浏览（Development、Web & API 等）。技能为开放 SKILL.md 格式，复制到 `.cursor/skills/<技能名>/SKILL.md` 即可。

### 2. Antigravity Codes 精选

- **网址**: https://antigravity.codes/agent-skills
- **推荐**: [spring-boot-crud-patterns](https://antigravity.codes/agent-skills/backend/spring-boot-crud-patterns)（REST 分层、DTO、事务与校验思路通用；本项目为 MongoDB 非 JPA，可借鉴架构部分）。同页有 Spring Boot Java Expert、REST API Design 等 Rules 可一并参考。
- **安装**: 页面提供 Download 或 `npx skills add <author/repo>` 时，按说明安装到 `.cursor/skills/` 或 `~/.cursor/skills/`。

### 安装方式（Cursor）

- **项目级**：在本仓库根目录下创建 `.cursor/skills/<技能名>/SKILL.md`，将技能内容放入即可。
- **全局**：放入 `~/.cursor/skills/<作者或分类>/<技能名>/SKILL.md`（Windows: `C:\Users\<用户>\.cursor\skills\...`）。
