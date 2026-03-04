---
name: fleets-mongodb-persistence
description: Fleets 项目 MongoDB 持久层约定，包括 Repository 命名、实体与 DTO、同步与 Reactive 使用场景。在编写或修改 MongoDB 相关实体、Repository、Service 时使用。
---

# Fleets MongoDB 持久层约定

本技能描述 Fleets 后端中 **MongoDB** 的用法约定（Repository、实体、同步/响应式），供 AI 生成或修改代码时遵循，与现有 mailbox、message 等模块风格一致。

## When to Use

- 新增或修改 MongoDB 集合对应的实体、Repository。
- 在 Service 中编写 MongoDB 查询、保存、删除逻辑。
- 决定使用同步（MongoTemplate / MongoRepository）还是响应式（ReactiveMongoRepository）时。

触发场景示例：**「用 MongoDB 存这个」**、**「加一个消息表」**、**「查信箱」**、**「Mongo Repository」**。

---

## 一、技术栈与配置

- 项目同时引入 **spring-boot-starter-data-mongodb**（同步）与 **spring-boot-starter-data-mongodb-reactive**（响应式）。
- [MongoConfig](src/main/java/org/example/fleets/storage/mongodb/MongoConfig.java) 仅做必要配置说明；**MongoTemplate**、**ReactiveMongoTemplate** 由 Spring Boot 自动配置，需要时直接注入即可。

---

## 二、实体（Document）约定

- 使用 `@Document(collection = "集合名")` 指定集合名；集合名使用 **小写+下划线**，如 `mailbox_message`、`user_mailbox`、`message`。
- 主键：使用 `String` 类型 id，对应 MongoDB `_id`（可配合 `@Id`）。与现有 [MailboxMessage](src/main/java/org/example/fleets/mailbox/model/entity/MailboxMessage.java)、[Message](src/main/java/org/example/fleets/message/model/entity/Message.java) 等保持一致。
- 实体放在各模块的 `model/entity` 包下，例如：`org.example.fleets.mailbox.model.entity`、`org.example.fleets.message.model.entity`。
- 不在实体中写业务逻辑；复杂校验与业务规则放在 Service 或领域层。

---

## 三、Repository 约定

### 1. 接口命名与包路径

- 接口名：**XxxRepository**，继承 `MongoRepository<Entity, String>` 或 `ReactiveMongoRepository<Entity, String>`。
- 包路径：各模块下的 `repository` 包，例如 `org.example.fleets.mailbox.repository`、`org.example.fleets.message.repository`。
- 使用 `@Repository` 标注。

### 2. 同步 vs 响应式

- **同步**：`MongoRepository<Entity, String>`，返回 `List`、`Optional`、`void` 等。用于信箱、用户信箱等「请求-响应」式读写，例如 [MailboxMessageRepository](src/main/java/org/example/fleets/mailbox/repository/MailboxMessageRepository.java)、[UserMailboxRepository](src/main/java/org/example/fleets/mailbox/repository/UserMailboxRepository.java)。
- **响应式**：`ReactiveMongoRepository<Entity, String>`，返回 `Mono`/`Flux`。用于消息流、高并发读等场景，例如 [MessageRepository](src/main/java/org/example/fleets/message/repository/MessageRepository.java)。
- 同一模块内尽量统一：要么全同步，要么全响应式；若混用（如 Service 既用同步 Repository 又用 ReactiveMongoTemplate），需在代码中明确区分并写好注释。

### 3. 方法命名

- 遵循 Spring Data MongoDB 的**派生查询**：`findByXxxAndYyy`、`countByXxx`、`deleteByXxx` 等；复杂查询可使用 `@Query`。
- 分页：同步用 `Pageable` 参数，返回 `List<Entity>` 或 `Page<Entity>`；响应式用 `Pageable` 时注意返回类型为 `Flux`/`Mono` 与框架支持。

### 4. 批量与复杂操作

- 简单 CRUD 用 Repository 方法即可。
- 批量删除、复杂聚合等可注入 **MongoTemplate**（同步）或 **ReactiveMongoTemplate**（响应式）实现，例如 [MailboxServiceImpl](src/main/java/org/example/fleets/mailbox/service/impl/MailboxServiceImpl.java) 中的批量删除注释「需要使用 MongoTemplate 进行批量删除」。

---

## 四、Service 层与 DTO

- **不**在 Controller 层直接暴露 MongoDB 实体；使用 **DTO/VO** 与前端交互，实体与 DTO 的转换放在 Service 或独立 Converter（如 MapStruct）中。
- Service 内：先做参数与权限校验（使用项目 [Assert](src/main/java/org/example/fleets/common/util/Assert.java) 与 [BusinessException](src/main/java/org/example/fleets/common/exception/BusinessException.java)），再调用 Repository 或 Template；查不到时用 `Assert.notNull(entity, ErrorCode.XXX)` 或抛 `BusinessException`，与项目异常规范一致。
- 分页：统一使用项目分页结果封装（如 [PageResult](src/main/java/org/example/fleets/common/util/PageResult.java)），避免直接返回 Spring 的 `Page` 给前端。

---

## 五、RocketMQ 与消息（简要）

- 消息发送/消费的包路径与命名保持与现有模块一致；错误处理统一抛 `BusinessException` 或记录日志后返回统一错误码，不在消息回调里引入新的异常体系。

---

## 六、小结

| 内容           | 约定 |
|----------------|------|
| 集合名         | 小写+下划线，`@Document(collection = "xxx")` |
| 主键           | String id，对应 _id |
| 同步           | MongoRepository + MongoTemplate，返回 List/Optional/void |
| 响应式         | ReactiveMongoRepository + ReactiveMongoTemplate，返回 Mono/Flux |
| Repository 位置 | 各模块 `repository` 包，XxxRepository |
| 实体位置       | 各模块 `model/entity` 包 |
| 对外           | 使用 DTO/VO，不直接暴露实体；异常用 ErrorCode + BusinessException |

遵循上述约定可与现有 mailbox、message 等模块风格一致，便于维护和扩展。
