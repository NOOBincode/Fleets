---
name: fleets-auth-and-exception
description: Fleets 项目鉴权（Sa-Token）与统一异常、错误码约定。在编写或修改登录校验、权限校验、业务异常与参数校验时使用。
---

# Fleets 鉴权与异常规范

本技能描述 Fleets 后端中 **Sa-Token 鉴权** 与 **统一异常/错误码** 的约定，供 AI 生成或修改代码时遵循，避免引入不一致的鉴权或异常写法。

## When to Use

- 新增或修改需要登录/权限校验的接口或拦截规则。
- 在 Service 或 Controller 中抛出业务异常、做参数校验。
- 新增错误码或统一响应格式时。

触发场景示例：**「加一个需要登录的接口」**、**「这里没权限要抛异常」**、**「参数校验」**、**「错误码」**。

---

## 一、Sa-Token 约定

### 1. 依赖与配置

- 项目使用 **Sa-Token**（`cn.dev33.satoken`），与 Redis 集成（`sa-token-redis-jackson`），会话存 Redis。
- 鉴权配置在 [SaTokenConfig](src/main/java/org/example/fleets/common/config/SaTokenConfig.java)：通过 `SaInterceptor` + `SaRouter` 做路径拦截，**登录校验** 使用 `StpUtil.checkLogin()`。

### 2. 拦截规则（当前）

- **默认**：`/**` 全部需登录。
- **排除（不需登录）**：`/api/user/register`、`/api/user/login`、`/ws/**`、`/error`、`/favicon.ico`。
- 新增白名单时在 `SaTokenConfig` 的 `SaRouter.notMatch(...)` 链上追加，不要在其他地方重复实现登录校验。

### 3. 在代码中获取当前用户

- **当前登录用户 ID**：`StpUtil.getLoginIdAsLong()`（项目约定为 Long 类型用户 ID）。
- 不要自行从 Header 或 Request 里解析 token；统一用 StpUtil。
- 未登录访问已拦截路径会抛出 Sa-Token 的 `NotLoginException`，由全局异常处理器统一返回 未授权（见下文）。

### 4. 不要做的事

- 不要新增一套与 Sa-Token 并行的鉴权（如手写 JWT 校验、自定义 Filter 做登录判断）。
- 不要在 Controller 里重复写「是否登录」判断；交给拦截器即可。Controller/Service 中只需在需要时调用 `StpUtil.getLoginIdAsLong()` 获取当前用户。

---

## 二、统一异常与错误码

### 1. 错误码枚举

- 所有业务错误码定义在 [ErrorCode](src/main/java/org/example/fleets/common/exception/ErrorCode.java)。
- 格式：`枚举常量(code, "中文描述")`；按模块分段（如 1xxx 通用、2xxx 用户、6xxx 群组等）。新增错误码时在对应段内添加，避免重复 code。

### 2. 业务异常

- 使用 [BusinessException](src/main/java/org/example/fleets/common/exception/BusinessException.java)：
  - `new BusinessException(ErrorCode.XXX)`：只带错误码；
  - `new BusinessException(ErrorCode.XXX, "补充说明")`：带错误码与自定义文案（支持 `String.format` 占位符）。
- 不要使用 `RuntimeException` 或自定义非 `BusinessException` 的业务异常，否则无法被统一处理器按 code 返回。

### 3. 参数与状态校验（Assert）

- 使用项目工具类 [Assert](src/main/java/org/example/fleets/common/util/Assert.java)（`org.example.fleets.common.util.Assert`），不要用 Spring 的 `Assert`。
- 常用写法：
  - `Assert.notNull(对象, "参数不能为空")` 或 `Assert.notNull(对象, ErrorCode.XXX)`；
  - `Assert.hasText(字符串, "xxx不能为空")` 或 `Assert.hasText(字符串, ErrorCode.XXX)`；
  - `Assert.isTrue(条件, "说明")` / `Assert.isFalse(条件, "说明")`；
  - `Assert.notEmpty(集合, "说明")`、`Assert.positive(数字, "说明")` 等。
- 校验失败会抛出 `BusinessException`，由全局异常处理器统一返回。

### 4. 全局异常处理

- [GlobalExceptionHandler](src/main/java/org/example/fleets/common/exception/GlobalExceptionHandler.java) 使用 `@RestControllerAdvice` 统一处理：
  - **NotLoginException**（Sa-Token 未登录）→ `ErrorCode.UNAUTHORIZED`，文案「请先登录」；
  - **BusinessException** → 返回 `code` + `message`（CommonResult.failed）；
  - **MethodArgumentNotValidException / BindException**（如 @Valid）→ `ErrorCode.VALIDATE_FAILED`；
  - **IllegalArgumentException** → `ErrorCode.VALIDATE_FAILED`；
  - **NullPointerException / Exception** → `ErrorCode.SYSTEM_ERROR`，对外统一为系统错误文案。
- 新增接口时无需再写 try-catch 转 CommonResult；直接抛 `BusinessException` 或使用 `Assert` 即可。

---

## 三、接口层约定（与鉴权、异常一致）

- Controller 层不捕获业务异常；需要「当前用户」时使用 `StpUtil.getLoginIdAsLong()`。
- 统一响应体使用 [CommonResult](src/main/java/org/example/fleets/common/api/CommonResult.java)：成功用 `CommonResult.success(data)`，失败由全局异常处理器返回 `CommonResult.failed(code, message)`。
- 新增需要登录的接口：只需保证路径未被 SaTokenConfig 排除；若需「仅部分角色可访问」，在 Service 内根据 `StpUtil.getLoginIdAsLong()` 查权限后抛 `BusinessException(ErrorCode.NO_PERMISSION, "说明")`。

---

## 四、小结

| 需求           | 做法 |
|----------------|------|
| 需登录         | 不排除路径，在 SaTokenConfig 保持默认拦截；取当前用户用 `StpUtil.getLoginIdAsLong()` |
| 不需登录       | 在 SaTokenConfig 的 `notMatch` 中增加路径 |
| 业务/参数错误  | 抛 `BusinessException(ErrorCode.XXX)` 或使用 `Assert.*` |
| 新增错误码     | 在 ErrorCode 枚举中按模块分段添加 |
| 统一响应       | 成功 Controller 返回 CommonResult.success；失败交给 GlobalExceptionHandler |

遵循上述约定可保持鉴权与异常风格一致，便于前端按 `code`/`message` 统一处理。
