# Fleets 可观测性快速启动指南

## 一分钟了解

**技术栈**：Fluentd + Prometheus + Grafana + Loki  
**资源占用**：600MB内存，17GB磁盘  
**部署时间**：5分钟  
**适用场景**：长期维护项目，与监控/资源调配项目集成

---

## 快速启动（3步）

### 步骤1：启动可观测性栈

```bash
# Windows
cd docker/observability
start.bat

# Linux/Mac
cd docker/observability
chmod +x start.sh
./start.sh
```

### 步骤2：配置Fleets应用

#### 2.1 添加Maven依赖

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
    
    <!-- JSON日志 -->
    <dependency>
        <groupId>net.logstash.logback</groupId>
        <artifactId>logstash-logback-encoder</artifactId>
        <version>7.4</version>
    </dependency>
</dependencies>
```

#### 2.2 配置application.yml

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      application: fleets
```

#### 2.3 启动Fleets应用

```bash
mvn spring-boot:run
```

### 步骤3：验证监控

1. **检查指标采集**：访问 http://localhost:9090/targets
   - 应该看到 `fleets-app` 状态为 `UP`

2. **查看监控面板**：访问 http://localhost:3000
   - 用户名：`admin`
   - 密码：`admin123`

3. **查询日志**：在Grafana中选择Loki数据源
   - 查询：`{app="fleets"}`

---

## 核心功能

### 1. 指标监控（Prometheus）

**自动采集的指标**：
- JVM内存、GC、线程
- HTTP请求数、响应时间
- 数据库连接池
- Redis连接

**自定义业务指标**：

```java
@Service
public class UserServiceImpl {
    private final Counter registerCounter;
    
    public UserServiceImpl(MeterRegistry registry) {
        this.registerCounter = Counter.builder("fleets_user_register_total")
            .description("用户注册总数")
            .register(registry);
    }
    
    public void register() {
        registerCounter.increment();
        // 业务逻辑...
    }
}
```

### 2. 日志采集（Fluentd + Loki）

**日志格式**：JSON（自动配置）

**日志查询**：
```
# 查询所有日志
{app="fleets"}

# 查询错误日志
{app="fleets"} |= "ERROR"

# 查询特定用户操作
{app="fleets"} |= "userId=123"
```

### 3. 告警通知（AlertManager）

**预置告警规则**：
- 应用宕机
- 高错误率
- JVM内存过高
- 接口响应慢
- 数据库连接池耗尽

**配置钉钉告警**：
1. 编辑 `docker/observability/alertmanager/alertmanager.yml`
2. 替换 `YOUR_DINGTALK_TOKEN`
3. 重启：`docker-compose restart alertmanager`

---

## 与其他项目集成

### 场景1：监控项目读取Fleets指标

```yaml
# 监控项目的prometheus.yml
scrape_configs:
  - job_name: 'fleets'
    honor_labels: true
    metrics_path: '/federate'
    params:
      'match[]': ['{job="fleets-app"}']
    static_configs:
      - targets: ['fleets-prometheus:9090']
```

### 场景2：资源调配项目自动扩缩容

```python
# 根据在线用户数自动扩缩容
import requests

def get_online_users():
    resp = requests.get(
        'http://prometheus:9090/api/v1/query',
        params={'query': 'fleets_user_online_count'}
    )
    return int(resp.json()['data']['result'][0]['value'][1])

def auto_scale():
    users = get_online_users()
    if users > 1000:
        scale_up()  # 扩容
    elif users < 100:
        scale_down()  # 缩容
```

### 场景3：统一Grafana Dashboard

所有项目共用一个Grafana实例：
- Fleets监控面板
- 监控项目面板
- 资源调配面板
- 统一告警视图

---

## 常用操作

### 查看服务状态

```bash
cd docker/observability
docker-compose ps
```

### 查看日志

```bash
# 查看所有日志
docker-compose logs -f

# 查看特定服务
docker-compose logs -f prometheus
docker-compose logs -f grafana
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart prometheus
```

### 停止服务

```bash
docker-compose down
```

### 清理数据（谨慎）

```bash
docker-compose down -v  # 删除所有数据
```

---

## 答辩展示建议

### 展示内容

1. **实时监控大屏**
   - 打开Grafana Dashboard
   - 展示在线用户数、消息发送量
   - 展示JVM内存、GC情况

2. **日志查询演示**
   - 查询某个用户的操作日志
   - 查询错误日志并定位问题

3. **告警演示**
   - 停止应用触发告警
   - 展示钉钉通知

### 答辩话术

> "本项目采用云原生可观测性技术栈Prometheus + Grafana + Loki，实现了指标监控、日志聚合、告警通知的完整闭环。相比传统ELK方案，资源占用降低80%，同时保持了企业级监控能力。该技术栈可与我的监控项目、资源调配项目无缝集成，形成完整的DevOps工具链。"

---

## 实施时间线

### 毕业设计阶段（现在-6月）

**优先级**：低（核心功能优先）

- ✅ 配置文件已准备好
- ⏳ 答辩前2周部署
- ⏳ 答辩时展示基础监控

**工作量**：8小时

### 长期维护阶段（答辩后）

**优先级**：高（完善可观测性）

- ⏳ 补充自定义业务指标
- ⏳ 配置告警规则
- ⏳ 与其他项目集成
- ⏳ 实现自动化运维

**工作量**：20小时

---

## 资源占用

| 组件 | 内存 | CPU | 磁盘 |
|-----|------|-----|------|
| Prometheus | 200MB | 0.1核 | 10GB |
| Loki | 150MB | 0.1核 | 5GB |
| Fluentd | 100MB | 0.1核 | 1GB |
| Grafana | 150MB | 0.1核 | 1GB |
| **总计** | **600MB** | **0.4核** | **17GB** |

**推荐服务器配置**：
- 2核4GB内存
- 40GB SSD
- 5Mbps带宽
- 成本：约100元/月

---

## 故障排查

### Q1: Prometheus无法采集指标

**检查**：
1. Fleets应用是否启动
2. 访问 http://localhost:8080/actuator/prometheus
3. 检查 `prometheus.yml` 中的 `targets` 配置

### Q2: Grafana无法连接Prometheus

**检查**：
1. Prometheus是否运行：`docker-compose ps`
2. 数据源URL是否正确：`http://prometheus:9090`
3. 点击 `Save & Test` 测试连接

### Q3: 日志未采集

**检查**：
1. 日志文件是否存在：`logs/fleets-json.log`
2. Fluentd是否运行：`docker-compose logs fluentd`
3. 卷挂载是否正确：`docker-compose.yml`

---

## 下一步

1. ✅ 阅读本文档
2. ⏳ 继续完成Fleets核心功能
3. ⏳ 答辩前2周部署可观测性栈
4. ⏳ 答辩后完善监控体系
5. ⏳ 与其他项目集成

---

## 参考文档

- [完整技术方案](./OBSERVABILITY_STACK.md) - 详细设计文档
- [部署指南](../docker/observability/README.md) - 部署步骤
- [ELK对比](./ELK_DEPLOYMENT_RECOMMENDATION.md) - 为什么不用ELK

---

## 总结

**核心优势**：
- ✅ 轻量级（600MB vs ELK的3GB）
- ✅ 云原生（CNCF标准）
- ✅ 易集成（与其他项目打通）
- ✅ 长期维护（技术栈成熟）

**实施建议**：
- 现在：专注核心功能开发
- 答辩前：部署基础监控
- 答辩后：完善可观测性体系

**记住**：可观测性是锦上添花，核心功能才是根本。先把Fleets做好，再考虑监控。
