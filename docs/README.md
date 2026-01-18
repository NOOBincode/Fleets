# Fleets 项目文档目录

## 📚 文档分类

### 🎯 开发指南（必读）

#### 1. [开发路线图](DEVELOPMENT_ROADMAP.md) ⭐⭐⭐
**用途**：核心功能开发的详细指南
- Mailbox模块实现步骤（10个方法）
- 消息模块实现步骤
- WebSocket模块实现步骤
- 每个方法的代码示例和注意事项
- 时间规划：9-10天

**何时使用**：开始实现任何模块前必读

---

#### 2. [快速参考卡](QUICK_REFERENCE_CARD.md) ⭐⭐⭐
**用途**：开发时的速查手册
- Reactive编程速查
- MongoDB查询速查
- 常用代码片段（可直接复制）
- 错误处理模板
- 测试模板
- 调试技巧

**何时使用**：编码时随时查阅

---

### 📐 架构设计

#### 3. [系统架构](SYSTEM_ARCHITECTURE.md)
**用途**：整体架构设计
- 技术栈选型
- 模块划分
- 数据流设计
- 部署架构

**何时使用**：了解系统整体设计

---

#### 4. [Mailbox设计](MAILBOX_DESIGN.md)
**用途**：Mailbox模块的详细设计
- 设计目标和原理
- 数据模型
- 核心流程
- 序列号机制

**何时使用**：实现Mailbox模块前阅读

---

#### 5. [Mailbox快速参考](MAILBOX_QUICK_REFERENCE.md)
**用途**：Mailbox模块的API速查
- 接口列表
- 参数说明
- 返回值说明

**何时使用**：调用Mailbox接口时查阅

---

#### 6. [好友关系工作流](FRIENDSHIP_WORKFLOW.md)
**用途**：好友关系的验证流程
- 好友请求流程
- 状态定义
- API说明

**何时使用**：实现好友相关功能时参考

---

### 🧪 测试指南

#### 7. [测试指南](TESTING_GUIDE.md)
**用途**：测试策略和实践
- 单元测试（JUnit + Mockito）
- 集成测试（Spring Boot Test）
- 性能测试（Locust + JMeter）
- 测试示例代码

**何时使用**：编写测试代码时参考

---

### 📊 可观测性

#### 8. [可观测性快速启动](OBSERVABILITY_QUICK_START.md) ⭐⭐
**用途**：监控系统的快速部署指南
- 5分钟部署Prometheus + Grafana + Loki
- 配置说明
- 使用指南

**何时使用**：答辩前2周部署监控系统

---

#### 9. [可观测性技术栈](OBSERVABILITY_STACK.md)
**用途**：监控系统的完整设计
- 架构设计
- 指标定义
- 告警规则
- 与其他项目集成

**何时使用**：深入了解监控系统设计

---

### 📝 决策记录

#### 10. [技术决策](TECH_DECISIONS.md)
**用途**：重要技术决策的记录
- 可观测性技术栈选型
- 测试策略
- 好友验证流程
- Mailbox模块设计

**何时使用**：了解为什么做某个技术选择

---

## 🗂️ 文档使用建议

### 开始开发前
1. 阅读 [系统架构](SYSTEM_ARCHITECTURE.md) - 了解整体设计
2. 阅读 [开发路线图](DEVELOPMENT_ROADMAP.md) - 了解开发顺序
3. 收藏 [快速参考卡](QUICK_REFERENCE_CARD.md) - 随时查阅

### 实现Mailbox模块
1. 阅读 [Mailbox设计](MAILBOX_DESIGN.md) - 理解设计原理
2. 参考 [开发路线图](DEVELOPMENT_ROADMAP.md) - 按步骤实现
3. 查阅 [快速参考卡](QUICK_REFERENCE_CARD.md) - 复制代码片段
4. 参考 [Mailbox快速参考](MAILBOX_QUICK_REFERENCE.md) - 查看API

### 编写测试
1. 阅读 [测试指南](TESTING_GUIDE.md) - 了解测试策略
2. 复制测试模板 - 从快速参考卡获取

### 部署监控
1. 阅读 [可观测性快速启动](OBSERVABILITY_QUICK_START.md) - 快速部署
2. 参考 [可观测性技术栈](OBSERVABILITY_STACK.md) - 深入配置

---

## 📌 核心文档优先级

### 必读（⭐⭐⭐）
- [开发路线图](DEVELOPMENT_ROADMAP.md)
- [快速参考卡](QUICK_REFERENCE_CARD.md)

### 重要（⭐⭐）
- [系统架构](SYSTEM_ARCHITECTURE.md)
- [Mailbox设计](MAILBOX_DESIGN.md)
- [可观测性快速启动](OBSERVABILITY_QUICK_START.md)

### 参考（⭐）
- [测试指南](TESTING_GUIDE.md)
- [技术决策](TECH_DECISIONS.md)
- [好友关系工作流](FRIENDSHIP_WORKFLOW.md)
- [Mailbox快速参考](MAILBOX_QUICK_REFERENCE.md)
- [可观测性技术栈](OBSERVABILITY_STACK.md)

---

## 🔄 文档更新记录

### 2025-01-14
- 清理过时文档（删除14个过程文档）
- 保留10个核心文档
- 创建文档目录

### 保留的文档
1. DEVELOPMENT_ROADMAP.md - 开发路线图
2. QUICK_REFERENCE_CARD.md - 快速参考卡
3. SYSTEM_ARCHITECTURE.md - 系统架构
4. MAILBOX_DESIGN.md - Mailbox设计
5. MAILBOX_QUICK_REFERENCE.md - Mailbox快速参考
6. FRIENDSHIP_WORKFLOW.md - 好友关系工作流
7. TESTING_GUIDE.md - 测试指南
8. OBSERVABILITY_QUICK_START.md - 可观测性快速启动
9. OBSERVABILITY_STACK.md - 可观测性技术栈
10. TECH_DECISIONS.md - 技术决策

### 删除的文档（过程文档）
- ARCHITECTURE_OPTIMIZATION_RECOMMENDATIONS.md
- CORE_MODULES_IMPLEMENTATION_GUIDE.md
- DATABASE_DESIGN_GUIDE.md
- DATABASE_ISSUES_SUMMARY.md
- DATABASE_OPTIMIZATION_GUIDE.md
- DATABASE_SYNC_SUMMARY.md
- ELK_DEPLOYMENT_RECOMMENDATION.md
- FINAL_CHECKLIST.md
- INFRASTRUCTURE_GUIDE.md
- MODERN_TESTING_TOOLS.md
- PROJECT_TIMELINE.md
- QUICK_START.md
- SERVICE_IMPLEMENTATION_GUIDE.md
- TESTING_STATUS.md

---

## 💡 提示

- 所有文档都是Markdown格式，可以在IDE中直接查看
- 代码片段可以直接复制使用
- 遇到问题先查文档，再问AI助手
- 文档会随着项目进展持续更新

---

## 📞 需要帮助？

如果文档中没有找到答案：
1. 检查 [快速参考卡](QUICK_REFERENCE_CARD.md) 的"常见错误"部分
2. 查看 [技术决策](TECH_DECISIONS.md) 了解设计原因
3. 向AI助手提问（提供具体的错误信息和代码）
