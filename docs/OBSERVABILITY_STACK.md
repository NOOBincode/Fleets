# Fleets 可观测性技术栈方案

## 项目定位

**短期目标**：本科毕业设计项目（2025年6月答辩）  
**长期目标**：个人长期维护项目，与监控项目、资源调配项目集成

---

## 技术栈选型：Fluentd + Prometheus + Grafana

### 为什么选择这个组合？

#### vs ELK
- ✅ **更轻量**：Prometheus内存占用200MB vs Elasticsearch 2GB
- ✅ **更现代**：云原生标准（CNCF项目）
- ✅ **更灵活**：Fluentd支持多种输出（Loki、Elasticsearch、S3等）
- ✅ **更便宜**：开源免费，无License限制

#### vs 传统监控
- ✅ **统一界面**：Grafana统一展示日志、指标、链路
- ✅ **易于集成**：与K8s、Docker、云平台无缝集成
- ✅ **社区活跃**：大量现成的Dashboard和插件

#### 适合你的场景
- ✅ **多项目集成**：监控项目、资源调配项目可共用Grafana
- ✅ **长期维护**：技术栈成熟，不会过时
- ✅ **答辩加分**：展示云原生技术栈，提升项目档次

---

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Grafana                               │
│              (统一可视化界面 - 端口3000)                      │
│  ┌──────────────┬──────────────┬──────────────────────────┐ │
│  │  日志查询     │  指标监控     │  告警管理                │ │
│  └──────────────┴──────────────┴──────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
         │                │                    │
         ▼                ▼                    ▼
┌─────────────┐  ┌─────────────┐    ┌─────────────────┐
│    Loki     │  │ Prometheus  │    │  AlertManager   │
│  (日志存储)  │  │  (指标存储)  │    │   (告警通知)     │
│  端口3100   │  │   端口9090   │    │    端口9093     │
└─────────────┘  └─────────────┘    └─────────────────┘
         ▲                ▲
         │                │
┌─────────────┐  ┌─────────────────────────────────┐
│  Fluentd    │  │  Micrometer (Spring Boot)       │
│  (日志采集)  │  │  + Prometheus Exporter          │
│  端口24224  │  │  (指标暴露 - 端口8080/actuator) │
└─────────────┘  └─────────────────────────────────┘
         ▲                ▲
         │                │
┌──────────────────────────────────────────────────┐
│           Fleets Application                      │
│  (Spring Boot + WebSocket + RocketMQ)            │
└──────────────────────────────────────────────────┘
```

### 数据流

1. **日志流**：Fleets → Fluentd → Loki → Grafana
2. **指标流**：Fleets → Prometheus → Grafana
3. **告警流**：Prometheus → AlertManager → 钉钉/邮件/Webhook

---

## 详细实施方案

### 1. Prometheus + Micrometer（指标监控）

#### 1.1 添加依赖

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
</dependencies>
```

#### 1.2 配置Actuator

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
    export:
      prometheus:
        enabled: true
```

#### 1.3 自定义业务指标

```java
// 在Service中添加自定义指标
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final Counter registerCounter;
    private final Counter loginCounter;
    private final Timer loginTimer;
    
    public UserServiceImpl(MeterRegistry registry) {
        // 注册计数器
        this.registerCounter = Counter.builder("fleets.user.register")
            .description("用户注册次数")
            .tag("type", "register")
            .register(registry);
            
        this.loginCounter = Counter.builder("fleets.user.login")
            .description("用户登录次数")
            .tag("type", "login")
            .register(registry);
            
        // 注册计时器
        this.loginTimer = Timer.builder("fleets.user.login.duration")
            .description("用户登录耗时")
            .register(registry);
    }
    
    @Override
    public UserVO register(UserRegisterDTO dto) {
        registerCounter.increment(); // 计数+1
        // 业务逻辑...
    }
    
    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        return loginTimer.record(() -> {
            loginCounter.increment();
            // 业务逻辑...
        });
    }
}
```

#### 1.4 Prometheus配置

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # Fleets应用监控
  - job_name: 'fleets-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
        labels:
          app: 'fleets'
          env: 'dev'
  
  # MySQL监控（可选）
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']
  
  # Redis监控（可选）
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
  
  # MongoDB监控（可选）
  - job_name: 'mongodb'
    static_configs:
      - targets: ['mongodb-exporter:9216']
```

---

### 2. Fluentd + Loki（日志采集）

#### 2.1 Logback配置（输出JSON格式）

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- JSON格式输出（供Fluentd采集） -->
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
    
    <root level="INFO">
        <appender-ref ref="JSON_FILE"/>
    </root>
</configuration>
```

```xml
<!-- pom.xml 添加依赖 -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

#### 2.2 Fluentd配置

```conf
# fluentd.conf
<source>
  @type tail
  path /var/log/fleets/fleets-json.log
  pos_file /var/log/fleets/fleets-json.log.pos
  tag fleets.app
  <parse>
    @type json
    time_key timestamp
    time_format %Y-%m-%dT%H:%M:%S.%L%z
  </parse>
</source>

# 过滤器：添加标签
<filter fleets.**>
  @type record_transformer
  <record>
    hostname "#{Socket.gethostname}"
    app "fleets"
  </record>
</filter>

# 输出到Loki
<match fleets.**>
  @type loki
  url http://loki:3100
  extra_labels {"app":"fleets"}
  <label>
    level
    logger_name
  </label>
  <buffer>
    flush_interval 10s
    flush_at_shutdown true
  </buffer>
</match>
```

---

### 3. Docker Compose部署

```yaml
# docker-compose-observability.yml
version: '3.8'

services:
  # Prometheus - 指标存储
  prometheus:
    image: prom/prometheus:v2.47.0
    container_name: fleets-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    networks:
      - fleets-network
    restart: unless-stopped

  # Loki - 日志存储
  loki:
    image: grafana/loki:2.9.0
    container_name: fleets-loki
    ports:
      - "3100:3100"
    volumes:
      - ./loki/loki-config.yml:/etc/loki/local-config.yaml
      - loki-data:/loki
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - fleets-network
    restart: unless-stopped

  # Fluentd - 日志采集
  fluentd:
    image: fluent/fluentd:v1.16-1
    container_name: fleets-fluentd
    ports:
      - "24224:24224"
      - "24224:24224/udp"
    volumes:
      - ./fluentd/fluent.conf:/fluentd/etc/fluent.conf
      - ../logs:/var/log/fleets  # 挂载应用日志目录
    networks:
      - fleets-network
    depends_on:
      - loki
    restart: unless-stopped

  # Grafana - 统一可视化
  grafana:
    image: grafana/grafana:10.1.0
    container_name: fleets-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    networks:
      - fleets-network
    depends_on:
      - prometheus
      - loki
    restart: unless-stopped

  # AlertManager - 告警管理（可选）
  alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: fleets-alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
    networks:
      - fleets-network
    restart: unless-stopped

volumes:
  prometheus-data:
  loki-data:
  grafana-data:

networks:
  fleets-network:
    driver: bridge
```

---

### 4. Grafana Dashboard配置

#### 4.1 数据源配置

```yaml
# grafana/provisioning/datasources/datasources.yml
apiVersion: 1

datasources:
  # Prometheus数据源
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true

  # Loki数据源
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    editable: true
```

#### 4.2 预置Dashboard

**应用监控Dashboard**（JSON配置）：
- JVM内存使用率
- GC次数和耗时
- HTTP请求QPS
- 接口响应时间（P50/P95/P99）
- 数据库连接池状态
- Redis命中率
- WebSocket连接数
- 消息队列积压量

**业务监控Dashboard**：
- 用户注册/登录趋势
- 在线用户数
- 消息发送量
- 好友添加量
- 错误率统计

---

## 监控指标设计

### 系统指标（自动采集）

| 指标类型 | 指标名称 | 说明 |
|---------|---------|------|
| JVM | jvm_memory_used_bytes | JVM内存使用量 |
| JVM | jvm_gc_pause_seconds | GC暂停时间 |
| HTTP | http_server_requests_seconds | HTTP请求耗时 |
| HTTP | http_server_requests_total | HTTP请求总数 |
| 线程 | jvm_threads_live | 活跃线程数 |
| 连接池 | hikaricp_connections_active | 数据库活跃连接 |

### 业务指标（自定义）

| 指标类型 | 指标名称 | 说明 |
|---------|---------|------|
| Counter | fleets_user_register_total | 用户注册总数 |
| Counter | fleets_user_login_total | 用户登录总数 |
| Gauge | fleets_user_online_count | 在线用户数 |
| Counter | fleets_message_send_total | 消息发送总数 |
| Timer | fleets_message_send_duration | 消息发送耗时 |
| Counter | fleets_friendship_add_total | 好友添加总数 |
| Gauge | fleets_websocket_connections | WebSocket连接数 |

---

## 告警规则配置

### Prometheus告警规则

```yaml
# prometheus/rules/fleets-alerts.yml
groups:
  - name: fleets-app-alerts
    interval: 30s
    rules:
      # 应用宕机告警
      - alert: FleetsAppDown
        expr: up{job="fleets-app"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Fleets应用宕机"
          description: "Fleets应用已宕机超过1分钟"

      # 高错误率告警
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "错误率过高"
          description: "5xx错误率超过5%"

      # JVM内存告警
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM内存使用率过高"
          description: "堆内存使用率超过90%"

      # 响应时间告警
      - alert: SlowResponse
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "接口响应慢"
          description: "P95响应时间超过1秒"
```

### AlertManager配置

```yaml
# alertmanager/alertmanager.yml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'dingtalk'

receivers:
  # 钉钉告警
  - name: 'dingtalk'
    webhook_configs:
      - url: 'http://your-dingtalk-webhook-url'
        send_resolved: true

  # 邮件告警（可选）
  - name: 'email'
    email_configs:
      - to: 'your-email@example.com'
        from: 'alertmanager@example.com'
        smarthost: 'smtp.example.com:587'
        auth_username: 'alertmanager@example.com'
        auth_password: 'password'
```

---

## 与其他项目集成

### 1. 监控项目集成

**场景**：你的监控项目可以从Prometheus拉取Fleets的指标

```yaml
# 监控项目的Prometheus配置
scrape_configs:
  - job_name: 'fleets-from-monitoring-project'
    honor_labels: true
    metrics_path: '/federate'
    params:
      'match[]':
        - '{job="fleets-app"}'
    static_configs:
      - targets: ['fleets-prometheus:9090']
```

### 2. 资源调配项目集成

**场景**：根据Fleets的负载自动调整资源

```python
# 资源调配项目示例代码
import requests

def get_fleets_metrics():
    """从Prometheus获取Fleets指标"""
    response = requests.get(
        'http://prometheus:9090/api/v1/query',
        params={'query': 'fleets_user_online_count'}
    )
    return response.json()

def auto_scale():
    """根据在线用户数自动扩缩容"""
    metrics = get_fleets_metrics()
    online_users = metrics['data']['result'][0]['value'][1]
    
    if int(online_users) > 1000:
        # 扩容逻辑
        scale_up_fleets_app()
    elif int(online_users) < 100:
        # 缩容逻辑
        scale_down_fleets_app()
```

### 3. 统一Grafana

**所有项目共用一个Grafana实例**：
- Fleets Dashboard
- 监控项目 Dashboard
- 资源调配项目 Dashboard
- 统一告警视图

---

## 实施计划

### 阶段1：基础监控（毕业设计阶段 - 2周）

**目标**：满足答辩需求

- [x] 配置Logback日志
- [ ] 添加Prometheus依赖
- [ ] 配置Actuator端点
- [ ] 部署Prometheus + Grafana
- [ ] 创建基础Dashboard（JVM、HTTP）

**工作量**：8小时

### 阶段2：日志采集（答辩后 - 1周）

**目标**：完善日志系统

- [ ] 配置Fluentd
- [ ] 部署Loki
- [ ] 配置日志JSON格式
- [ ] 在Grafana中查询日志

**工作量**：6小时

### 阶段3：业务监控（长期维护 - 2周）

**目标**：深度业务监控

- [ ] 添加自定义业务指标
- [ ] 创建业务Dashboard
- [ ] 配置告警规则
- [ ] 集成钉钉告警

**工作量**：12小时

### 阶段4：多项目集成（长期维护 - 1周）

**目标**：与其他项目打通

- [ ] 配置Prometheus联邦
- [ ] 统一Grafana界面
- [ ] 实现自动化资源调配
- [ ] 完善告警体系

**工作量**：8小时

---

## 成本分析

### 资源占用

| 组件 | 内存 | CPU | 磁盘 |
|-----|------|-----|------|
| Prometheus | 200MB | 0.1核 | 10GB |
| Loki | 150MB | 0.1核 | 5GB |
| Fluentd | 100MB | 0.1核 | 1GB |
| Grafana | 150MB | 0.1核 | 1GB |
| **总计** | **600MB** | **0.4核** | **17GB** |

**对比ELK**：
- 内存：600MB vs 3GB（节省80%）
- 磁盘：17GB vs 50GB（节省66%）

### 云服务器成本（阿里云）

**推荐配置**：
- 2核4GB内存（可运行Fleets + 可观测性栈）
- 40GB SSD
- 5Mbps带宽

**价格**：约100元/月（学生机更便宜）

---

## 答辩展示建议

### 展示内容

1. **实时监控大屏**（Grafana）
   - 在线用户数实时变化
   - 消息发送量曲线
   - 接口响应时间

2. **日志查询演示**
   - 查询某个用户的登录日志
   - 查询错误日志并定位问题
   - 展示日志聚合统计

3. **告警演示**
   - 模拟高负载触发告警
   - 展示钉钉告警通知
   - 展示告警恢复

### 答辩话术

> "本项目采用云原生可观测性技术栈，包括Prometheus指标监控、Loki日志聚合、Grafana统一可视化。相比传统ELK方案，资源占用降低80%，同时保持了企业级的监控能力。该技术栈可与我的监控项目、资源调配项目无缝集成，形成完整的DevOps工具链。"

---

## 总结

### 为什么选择 Fluentd + Prometheus + Grafana？

1. ✅ **轻量级**：600MB内存 vs ELK的3GB
2. ✅ **云原生**：CNCF标准，与K8s无缝集成
3. ✅ **易集成**：与监控项目、资源调配项目打通
4. ✅ **长期维护**：技术栈成熟，社区活跃
5. ✅ **答辩加分**：展示现代化技术栈

### 实施建议

**毕业设计阶段**（现在-6月）：
- 先完成核心功能开发
- 答辩前2周部署基础监控（Prometheus + Grafana）
- 答辩时展示实时监控大屏

**长期维护阶段**（答辩后）：
- 补充日志采集（Fluentd + Loki）
- 完善业务监控指标
- 与其他项目集成
- 实现自动化运维

### 下一步行动

1. 继续完成Fleets核心功能
2. 答辩前1个月开始部署监控
3. 答辩后逐步完善可观测性体系
