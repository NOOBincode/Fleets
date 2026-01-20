# Fleets 后端适配指南

## 📌 适配目标
将 Chatterbox 前端项目对接到 Fleets 后端系统

## 🔧 第一步：修改 API 基础配置（30分钟）

### 1.1 修改 API 基础 URL
**文件**: `src/utils/request.js` 或 `src/config/index.js`

```javascript
// 找到类似这样的配置
const baseURL = 'http://原作者的后端地址'

// 改成你的后端地址
const baseURL = 'http://localhost:8080'
```

### 1.2 修改请求拦截器（Sa-Token 认证）
**文件**: `src/utils/request.js`

```javascript
// 找到请求拦截器
service.interceptors.request.use(
  config => {
    // 原来可能是这样
    // config.headers['Authorization'] = 'Bearer ' + token
    
    // 改成 Sa-Token 方式
    const token = localStorage.getItem('satoken') // 或从 store 获取
    if (token) {
      config.headers['satoken'] = token
    }
    return config
  }
)
```

### 1.3 修改响应拦截器
**文件**: `src/utils/request.js`

```javascript
// 适配你的 CommonResult 格式
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 你的后端返回格式：{ code, message, data }
    if (res.code !== 200) {
      // 错误处理
      ElMessage.error(res.message || 'Error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res.data // 返回 data 部分
  }
)
```

## 🔧 第二步：适配用户相关接口（1小时）

### 2.1 登录接口
**文件**: `src/api/user.js` 或 `src/api/auth.js`

```javascript
// 原来的登录接口
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 改成你的
export function login(data) {
  return request({
    url: '/user/login',  // 对应 UserController.login
    method: 'post',
    data: {
      username: data.username,
      password: data.password
    }
  })
}

// 注册接口
export function register(data) {
  return request({
    url: '/user/register',  // 对应 UserController.register
    method: 'post',
    data
  })
}

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/user/info',  // 对应 UserController.getUserInfo
    method: 'get'
  })
}
```

### 2.2 修改登录后的数据处理
**文件**: `src/store/modules/user.js` 或 `src/store/user.js`

```javascript
// 登录 action
async login({ commit }, userInfo) {
  const res = await login(userInfo)
  
  // 根据你的 UserLoginVO 结构处理
  const { token, userInfo: user } = res
  
  // 保存 token
  localStorage.setItem('satoken', token)
  
  // 保存用户信息
  commit('SET_USER_INFO', user)
  
  return res
}
```

## 🔧 第三步：适配好友相关接口（1小时）

### 3.1 好友列表接口
**文件**: `src/api/friend.js`

```javascript
// 获取好友列表
export function getFriendList() {
  return request({
    url: '/friendship/list',  // 对应 FriendshipController.getFriendList
    method: 'get'
  })
}

// 添加好友
export function addFriend(data) {
  return request({
    url: '/friendship/add',  // 对应 FriendshipController.addFriend
    method: 'post',
    data: {
      friendId: data.friendId,
      remark: data.remark
    }
  })
}

// 删除好友
export function deleteFriend(friendId) {
  return request({
    url: `/friendship/delete/${friendId}`,
    method: 'delete'
  })
}
```

### 3.2 数据格式转换
如果他们的好友数据结构和你的 FriendVO 不一致，需要写转换函数：

```javascript
// src/utils/adapter.js
export function adaptFriendList(backendData) {
  return backendData.map(friend => ({
    id: friend.friendId,
    name: friend.nickname,
    avatar: friend.avatar,
    remark: friend.remark,
    // ... 其他字段映射
  }))
}
```

## 🔧 第四步：适配消息相关接口（1.5小时）

### 4.1 会话列表
**文件**: `src/api/conversation.js`

```javascript
// 获取会话列表
export function getConversationList() {
  return request({
    url: '/conversation/list',  // 对应 ConversationController
    method: 'get'
  })
}
```

### 4.2 消息接口
**文件**: `src/api/message.js`

```javascript
// 发送消息
export function sendMessage(data) {
  return request({
    url: '/message/send',  // 对应 MessageController.sendMessage
    method: 'post',
    data: {
      receiverId: data.receiverId,
      content: data.content,
      contentType: data.contentType || 'TEXT'
    }
  })
}

// 获取历史消息
export function getMessageHistory(conversationId, params) {
  return request({
    url: `/message/history/${conversationId}`,
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.size || 20
    }
  })
}

// 消息已读
export function markMessageRead(data) {
  return request({
    url: '/message/ack',  // 对应 MessageAckController
    method: 'post',
    data
  })
}
```

### 4.3 Mailbox 同步接口
**文件**: `src/api/mailbox.js` (新建)

```javascript
import request from '@/utils/request'

// 同步消息
export function syncMessages(data) {
  return request({
    url: '/mailbox/sync',  // 对应 MailboxController.syncMessages
    method: 'post',
    data: {
      conversationId: data.conversationId,
      lastSequence: data.lastSequence || 0
    }
  })
}

// 获取未读数
export function getUnreadCount(conversationId) {
  return request({
    url: `/mailbox/unread/${conversationId}`,
    method: 'get'
  })
}

// 标记已读
export function markRead(data) {
  return request({
    url: '/mailbox/read',
    method: 'post',
    data
  })
}
```

## 🔧 第五步：适配 WebSocket（2小时）

### 5.1 修改 WebSocket 连接
**文件**: `src/utils/websocket.js` 或 `src/store/modules/websocket.js`

```javascript
class WebSocketClient {
  constructor() {
    this.ws = null
    this.reconnectTimer = null
  }
  
  connect() {
    const token = localStorage.getItem('satoken')
    
    // 连接你的 WebSocket 端点
    this.ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`)
    
    this.ws.onopen = () => {
      console.log('WebSocket 连接成功')
    }
    
    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      this.handleMessage(data)
    }
    
    this.ws.onerror = (error) => {
      console.error('WebSocket 错误:', error)
    }
    
    this.ws.onclose = () => {
      console.log('WebSocket 连接关闭')
      this.reconnect()
    }
  }
  
  handleMessage(data) {
    // 根据你的 WebSocket 消息格式处理
    switch(data.type) {
      case 'MESSAGE':
        // 新消息
        this.onNewMessage(data.data)
        break
      case 'ACK':
        // 消息确认
        this.onMessageAck(data.data)
        break
      case 'TYPING':
        // 输入状态
        this.onTyping(data.data)
        break
    }
  }
  
  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    }
  }
  
  reconnect() {
    if (this.reconnectTimer) return
    
    this.reconnectTimer = setTimeout(() => {
      console.log('尝试重新连接...')
      this.connect()
      this.reconnectTimer = null
    }, 3000)
  }
  
  close() {
    if (this.ws) {
      this.ws.close()
    }
  }
}

export default new WebSocketClient()
```

### 5.2 在登录后初始化 WebSocket
**文件**: `src/views/login/index.vue` 或登录成功的回调

```javascript
import websocket from '@/utils/websocket'

// 登录成功后
async handleLogin() {
  await this.$store.dispatch('user/login', this.loginForm)
  
  // 初始化 WebSocket 连接
  websocket.connect()
  
  // 跳转到聊天页面
  this.$router.push('/chat')
}
```

## 🔧 第六步：数据类型定义（1小时）

### 6.1 创建类型定义文件
**文件**: `src/types/backend.ts` (新建)

```typescript
// 直接复制你后端的 VO/DTO 结构

// 用户相关
export interface UserVO {
  id: string
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  status: number
  createTime: string
}

export interface UserLoginVO {
  token: string
  userInfo: UserVO
}

// 好友相关
export interface FriendVO {
  friendId: string
  nickname: string
  avatar: string
  remark: string
  status: number
}

// 消息相关
export interface MessageVO {
  id: string
  conversationId: string
  senderId: string
  receiverId: string
  content: string
  contentType: 'TEXT' | 'IMAGE' | 'FILE' | 'VIDEO' | 'AUDIO'
  status: 'SENT' | 'DELIVERED' | 'READ'
  timestamp: number
  createTime: string
}

export interface MessageSendDTO {
  receiverId: string
  content: string
  contentType: string
}

// 会话相关
export interface ConversationVO {
  id: string
  type: 'PRIVATE' | 'GROUP'
  targetId: string
  targetName: string
  targetAvatar: string
  lastMessage: string
  lastMessageTime: string
  unreadCount: number
}

// Mailbox 相关
export interface SyncMessageDTO {
  conversationId: string
  lastSequence: number
}

export interface SyncResult {
  messages: MessageVO[]
  hasMore: boolean
  nextSequence: number
}

export interface UnreadCountVO {
  conversationId: string
  count: number
}
```

## 📝 测试清单

完成上述修改后，按顺序测试：

- [ ] 1. 登录功能
  - 输入用户名密码
  - 检查 token 是否保存
  - 检查是否跳转到聊天页面

- [ ] 2. 好友列表
  - 是否能加载好友列表
  - 头像、昵称是否正确显示

- [ ] 3. 会话列表
  - 是否能加载会话列表
  - 未读数是否正确

- [ ] 4. 发送消息
  - 点击好友进入聊天
  - 发送文本消息
  - 检查消息是否显示

- [ ] 5. 接收消息
  - 用另一个账号发送消息
  - 检查是否实时收到
  - 检查未读数是否更新

- [ ] 6. WebSocket 连接
  - 检查浏览器控制台是否有连接成功日志
  - 刷新页面是否自动重连

## 🐛 常见问题

### 问题1：跨域错误
**解决**：在后端添加 CORS 配置，或在前端配置代理

```javascript
// vite.config.js 或 vue.config.js
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
}
```

### 问题2：Token 认证失败
**检查**：
1. Token 是否正确保存
2. 请求头是否正确设置
3. 后端是否正确解析 Sa-Token

### 问题3：WebSocket 连接失败
**检查**：
1. WebSocket URL 是否正确
2. Token 是否在 URL 参数中传递
3. 后端 WebSocket 配置是否正确

### 问题4：数据格式不匹配
**解决**：写适配器函数转换数据格式

```javascript
// src/utils/adapter.js
export function adaptMessageList(backendMessages) {
  return backendMessages.map(msg => ({
    // 字段映射
    id: msg.id,
    content: msg.content,
    // ...
  }))
}
```

## 📚 参考资料

- 你的后端 API 文档：查看 Controller 层的接口定义
- Postman Collection：`Fleets_User_API_Tests.postman_collection.json`
- 后端文档目录：`docs/`

## 🎯 优先级

如果时间紧张，按这个顺序实现：

1. **必须**：登录 + 好友列表 + 发送消息
2. **重要**：WebSocket 实时接收 + 会话列表
3. **可选**：文件上传 + 群聊 + 语音视频

## 💡 小技巧

1. **使用浏览器开发者工具**
   - Network 标签查看 API 请求
   - Console 查看错误日志
   - Application 查看 localStorage

2. **对比数据格式**
   - 在 Network 中查看原项目的请求格式
   - 对比你的后端返回格式
   - 写转换函数

3. **逐步调试**
   - 先让一个功能跑通
   - 再逐步添加其他功能
   - 不要一次改太多

祝你明天工作顺利！🚀
