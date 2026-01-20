# Fleets - Java IM 即时通讯系统

## 项目简介

Fleets 是一个基于 Spring Boot 的分布式即时通讯系统，支持单聊、群聊、文件传输等功能。

## 技术栈

### 后端技术
- **Spring Boot 2.6.13** - 核心框架
- **Spring Security** - 安全认证
- **JWT** - Token 认证
- **MyBatis-Plus** - ORM 框架
- **WebSocket** - 实时通信

### 中间件
- **MySQL 8.0** - 关系型数据库（用户、群组、好友关系）
- **MongoDB 4.4** - 文档数据库（消息存储）
- **Redis 6.2** - 缓存（会话、在线状态）
- **RocketMQ 4.9.4** - 消息队列（消息投递）
- **OpenResty** - 网关层（鉴权、限流）

## 核心功能

### 用户模块
- 用户注册/登录
- 用户信息管理
- 头像上传
- 密码修改

### 好友模块
- 添加/删除好友
- 好友备注
- 好友分组
- 拉黑/取消拉黑

### 群组模块
- 创建/解散群组
- 加入/退出群组
- 群成员管理
- 群公告管理

### 消息模块
- 单聊消息
- 群聊消息
- 消息撤回
- 消息已读/未读
- 离线消息
- 消息历史查询

### 文件模块
- 文件上传
- 图片/语音/视频消息
- 文件存储管理

## 项目结构

```
src/main/java/org/example/fleets/
├── cache/          # 缓存模块
├── common/         # 公共模块（工具类、配置）
├── connector/      # 连接管理模块（WebSocket）
├── file/           # 文件服务模块
├── gateway/        # 网关模块
├── group/          # 群组模块
├── mailbox/        # 邮箱模块（离线消息）
├── message/        # 消息模块
├── openresty/      # OpenResty 配置
├── protocol/       # 协议定义
├── storage/        # 存储模块
└── user/           # 用户模块
```

## 快速开始

### 环境要求
- JDK 1.8+
- Maven 3.6+
- Docker & Docker Compose

### 启动步骤

1. 克隆项目
```bash
git clone <repository-url>
cd Fleets
```

2. 启动中间件（使用 Docker Compose）
```bash
cd src/main/java/docker
docker-compose up -d
```

3. 配置环境变量（可选，用于生产环境）
```bash
export JWT_SECRET=your-secret-key
export JWT_EXPIRATION=604800
```

4. 启动应用
```bash
mvn spring-boot:run
```

5. 访问应用
- 应用地址：http://localhost:8080
- RocketMQ Dashboard：http://localhost:8080

## API 文档

### 用户相关
- POST `/api/user/register` - 用户注册
- POST `/api/user/login` - 用户登录
- GET `/api/user/info` - 获取用户信息
- PUT `/api/user/update` - 更新用户信息

### 好友相关
- POST `/api/friend/add` - 添加好友
- DELETE `/api/friend/{friendId}` - 删除好友
- GET `/api/friend/list` - 获取好友列表

### 群组相关
- POST `/api/group/create` - 创建群组
- POST `/api/group/{groupId}/join` - 加入群组
- GET `/api/group/list` - 获取群组列表

### 消息相关
- POST `/api/message/send` - 发送消息
- GET `/api/message/chat/{targetUserId}` - 获取单聊历史
- GET `/api/message/group/{groupId}` - 获取群聊历史

## 数据库设计

### MySQL 表
- `user` - 用户表
- `friendship` - 好友关系表
- `group` - 群组表
- `group_member` - 群成员表
- `file` - 文件元数据表

### MongoDB 集合
- `message` - 消息表
- `offline_messages` - 离线消息表
- `mailboxes` - 邮箱表

## 配置说明

### JWT 配置
```properties
# 使用环境变量配置（推荐）
jwt.secret=${JWT_SECRET:default-secret}
jwt.expiration=${JWT_EXPIRATION:604800}
```

### 数据库配置
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/fleets
spring.datasource.username=root
spring.datasource.password=root
```

## 开发计划

- [ ] 完善消息推送机制
- [ ] 实现消息加密
- [ ] 添加敏感词过滤
- [ ] 实现音视频通话
- [ ] 添加朋友圈功能
- [ ] 完善监控和日志

## 致谢

### 前端框架
本项目的前端基于 [Chatterbox](https://github.com/gmingchen/chatterbox) 项目进行适配和修改。

感谢原作者 **Slipper (gmingchen)** 提供的优秀前端框架。

### 开源组件
感谢以下开源项目和社区：
- [Spring Boot](https://spring.io/projects/spring-boot) - 后端核心框架
- [Vue 3](https://github.com/vuejs/core) - 前端框架
- [Element Plus](https://github.com/element-plus/element-plus) - UI 组件库
- [MyBatis Plus](https://github.com/baomidou/mybatis-plus) - ORM 框架
- [Sa-Token](https://github.com/dromara/sa-token) - 权限认证框架

完整的开源声明和致谢请查看：
- 前端: `fleets-web/chatterbox/ATTRIBUTION.md`
- 许可证合规指南: `fleets-web/chatterbox/LICENSE_COMPLIANCE.md`

## 免责声明

1. 本项目仅供学习和研究使用
2. 前端部分基于 Chatterbox 项目，商业使用请联系原作者获取授权
3. 使用者需自行承担使用本项目的风险
4. 如有侵权，请联系我们立即删除

## 许可证

MIT License
