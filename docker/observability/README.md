# Fleets 可观测性技术栈部署指南

## 快速启动

### 1. 启动可观测性栈

```bash
cd docker/observability
docker-compose up -d
```

### 2. 验证服务状态

```bash
# 查看所有服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 3. 访问各个服务

| 服务 | 地址 | 用户名 | 密码 |
|-----|------|--------|------|
| Grafana | http://localhost:3000 | admin | admin123 |
| Prometheus | http://localhost:9090 | - | - |
| AlertManager | http://localhost:9093 | - | - |
| Loki | http://localhost:3100 | - | - |

---

## 配置Fleets应用

### 1. 添加Maven依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Prometheus监控 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    
    <!-- JSON日志格式 -->
    <dependency>
        <groupId>net.logstash.logback</groupId>
        <artifactId>logstash-logback-encoder</artifactId>
        <version>7.4</version>
    </dependency>
</dependencies>
```

### 2. 配置application.yml

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

### 3. 更新logback-spring.xml

已经配置好JSON格式输出，位于：`src/main/resources/logback-spring.xml`

需要添加JSON输出appender：

```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/fleets-json.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/fleets-json.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"app":"fleets","env":"dev"}</customFields>
    </encoder>
</appender>
```

---

## 验证监控是否正常

### 1. 检查Prometheus是否采集到指标

访问：http://localhost:9090/targets

应该看到 `fleets-app` 的状态为 `UP`

### 2. 查询指标

在Prometheus界面执行查询：

```promql
# JVM内存使用
jvm_memory_used_bytes{area="heap"}

# HTTP请求数
http_server_requests_seconds_count

# 自定义业务指标
fleets_user_register_total
```

### 3. 检查日志是否采集

访问Grafana：http://localhost:3000

1. 登录（admin/admin123）
2. 进入 Explore
3. 选择 Loki 数据源
4. 查询：`{app="fleets"}`

---

## Grafana Dashboard配置

### 导入预置Dashboard

1. 访问 http://localhost:3000
2. 点击左侧菜单 `Dashboards` → `Import`
3. 输入Dashboard ID：

| Dashboard | ID | 说明 |
|-----------|-----|------|
| JVM (Micrometer) | 4701 | JVM监控 |
| Spring Boot Statistics | 6756 | Spring Boot监控 |
| Loki Dashboard | 13639 | 日志查询 |

### 自定义Dashboard

参考文档：`docs/OBSERVABILITY_STACK.md`

---

## 告警配置

### 1. 配置钉钉机器人

1. 在钉钉群中添加自定义机器人
2. 获取Webhook地址
3. 编辑 `alertmanager/alertmanager.yml`
4. 替换 `YOUR_DINGTALK_TOKEN`
5. 重启AlertManager：

```bash
docker-compose restart alertmanager
```

### 2. 测试告警

停止Fleets应用，1分钟后应该收到告警通知。

---

## 常见问题

### Q1: Prometheus无法采集Fleets指标

**原因**：Docker容器无法访问宿主机的8080端口

**解决**：
- Windows/Mac：使用 `host.docker.internal:8080`（已配置）
- Linux：使用 `172.17.0.1:8080` 或 `--network host`

### Q2: Fluentd无法读取日志文件

**原因**：日志文件路径不正确

**解决**：
1. 检查 `docker-compose.yml` 中的卷挂载
2. 确保 `../../logs` 路径正确
3. 检查文件权限

### Q3: Grafana无法连接Prometheus

**原因**：数据源配置错误

**解决**：
1. 进入 Grafana → Configuration → Data Sources
2. 检查Prometheus URL是否为 `http://prometheus:9090`
3. 点击 `Save & Test`

---

## 性能优化

### 1. 减少指标采集频率

编辑 `prometheus/prometheus.yml`：

```yaml
scrape_configs:
  - job_name: 'fleets-app'
    scrape_interval: 30s  # 从15s改为30s
```

### 2. 减少日志保留时间

编辑 `loki/loki-config.yml`：

```yaml
table_manager:
  retention_period: 168h  # 从720h改为168h（7天）
```

### 3. 限制Prometheus内存

编辑 `docker-compose.yml`：

```yaml
prometheus:
  deploy:
    resources:
      limits:
        memory: 512M
```

---

## 停止和清理

### 停止所有服务

```bash
docker-compose down
```

### 清理数据（谨慎操作）

```bash
docker-compose down -v  # 删除所有数据卷
```

---

## 下一步

1. ✅ 启动可观测性栈
2. ✅ 配置Fleets应用
3. ✅ 验证指标采集
4. ✅ 验证日志采集
5. ⏳ 创建自定义Dashboard
6. ⏳ 配置告警规则
7. ⏳ 集成钉钉/企业微信

---

## 参考文档

- [完整技术方案](../../docs/OBSERVABILITY_STACK.md)
- [Prometheus官方文档](https://prometheus.io/docs/)
- [Grafana官方文档](https://grafana.com/docs/)
- [Loki官方文档](https://grafana.com/docs/loki/)
