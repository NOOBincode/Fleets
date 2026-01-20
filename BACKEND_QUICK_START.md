# 后端新功能快速启动指南

## ✅ 已完成的修复和新增

### 1. 修复统一错误处理 ✅
- 修复了 `BusinessException` 构造函数问题
- 添加了缺失的 `ErrorCode` 枚举值
- 所有编译错误已解决

### 2. 新增功能骨架 ✅

#### Phase 1 - 好友申请和分组（高优先级）
- ✅ 好友申请审核接口
- ✅ 好友分组管理接口
- ✅ 所有 VO 类已创建
- ✅ Service 和 Controller 已实现

#### Phase 2 - 表情包系统（中优先级）
- ✅ 完整模块结构已创建
- ✅ 基础 CRUD 接口已实现
- ⏳ 文件上传功能待完善

---

## 🚀 快速启动步骤

### 1. 数据库迁移

执行表情包表创建脚本：
```sql
-- 位置: src/main/resources/db/migration/V3__create_expression_table.sql
-- 或者手动执行以下SQL：

CREATE TABLE IF NOT EXISTS `expression` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NULL COMMENT '用户ID，NULL表示系统表情',
    `name` VARCHAR(50) NOT NULL COMMENT '表情名称',
    `url` VARCHAR(500) NOT NULL COMMENT '表情图片URL',
    `category` VARCHAR(50) DEFAULT 'default' COMMENT '分类',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 启动后端服务

```bash
cd Fleets
mvn clean install -DskipTests
mvn spring-boot:run
```

### 3. 测试新接口

#### 测试好友申请审核
```bash
# 获取待审核数量
curl -X GET http://localhost:8080/api/friendship/requests/count \
  -H "satoken: YOUR_TOKEN"

# 获取待审核列表
curl -X GET http://localhost:8080/api/friendship/requests \
  -H "satoken: YOUR_TOKEN"
```

#### 测试好友分组
```bash
# 按分组获取好友
curl -X GET http://localhost:8080/api/friendship/list/grouped \
  -H "satoken: YOUR_TOKEN"

# 获取分组列表
curl -X GET http://localhost:8080/api/friendship/groups \
  -H "satoken: YOUR_TOKEN"
```

#### 测试表情包
```bash
# 获取表情包列表
curl -X GET http://localhost:8080/api/expression/list \
  -H "satoken: YOUR_TOKEN"
```

---

## 📋 新增接口清单

### 好友模块（/api/friendship）

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/requests` | GET | 获取待审核列表 |
| `/requests/count` | GET | 获取待审核数量 |
| `/list/grouped` | GET | 按分组获取好友 |
| `/groups` | GET | 获取分组列表 |

### 表情包模块（/api/expression）

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/list` | GET | 获取表情包列表 |
| `/upload` | POST | 上传表情包（待完善） |
| `/{id}` | DELETE | 删除表情包 |

---

## 🔍 验证清单

- [ ] 后端服务启动成功
- [ ] 数据库表创建成功
- [ ] 好友申请接口可访问
- [ ] 好友分组接口可访问
- [ ] 表情包接口可访问
- [ ] 前端可以正常调用新接口

---

## 📚 相关文档

- [详细实现总结](./docs/BACKEND_IMPLEMENTATION_SUMMARY.md)
- [新功能实现计划](./docs/NEW_FEATURES_PLAN.md)
- [好友关系工作流](./docs/FRIENDSHIP_WORKFLOW.md)

---

**更新时间**: 2025-01-20  
**状态**: 基础骨架已完成，可以启动测试
