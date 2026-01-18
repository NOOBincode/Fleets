"""
Fleets IM系统性能测试脚本（Locust）

运行方式：
1. Web UI模式：locust -f locustfile.py --host=http://localhost:8080
2. 无头模式：locust -f locustfile.py --host=http://localhost:8080 --users 100 --spawn-rate 10 --run-time 5m --headless --html report.html
3. 分布式：locust -f locustfile.py --master --host=http://localhost:8080
"""

from locust import HttpUser, task, between, events
import json
import random
import time

class IMUser(HttpUser):
    """IM系统用户行为模拟"""
    
    # 用户操作间隔时间（秒）
    wait_time = between(1, 3)
    
    # 测试用户池
    test_users = [f"testuser{i}" for i in range(1, 101)]
    
    def on_start(self):
        """
        用户启动时执行：登录获取Token
        """
        username = random.choice(self.test_users)
        
        response = self.client.post(
            "/api/user/login",
            json={
                "username": username,
                "password": "Test@123456"
            },
            name="用户登录"
        )
        
        if response.status_code == 200:
            try:
                data = response.json()
                self.token = data.get("data", {}).get("token")
                self.user_id = data.get("data", {}).get("userInfo", {}).get("id")
                self.headers = {"Authorization": self.token}
                print(f"✅ 用户 {username} 登录成功")
            except Exception as e:
                print(f"❌ 解析登录响应失败: {e}")
                self.token = None
                self.headers = {}
        else:
            print(f"❌ 用户 {username} 登录失败: {response.status_code}")
            self.token = None
            self.headers = {}
    
    @task(5)
    def send_message(self):
        """
        发送消息（权重5，执行频率最高）
        模拟用户发送文本消息
        """
        if not self.token:
            return
        
        self.client.post(
            "/api/message/send",
            headers=self.headers,
            json={
                "receiverId": random.randint(1, 100),
                "messageType": 1,  # 单聊
                "contentType": 1,  # 文本
                "content": f"性能测试消息 {random.randint(1, 10000)}"
            },
            name="发送消息"
        )
    
    @task(3)
    def get_friend_list(self):
        """
        获取好友列表（权重3）
        """
        if not self.token:
            return
        
        self.client.get(
            "/api/friend/list",
            headers=self.headers,
            name="获取好友列表"
        )
    
    @task(2)
    def get_chat_history(self):
        """
        获取聊天记录（权重2）
        """
        if not self.token:
            return
        
        target_user_id = random.randint(1, 100)
        self.client.get(
            f"/api/message/history?targetUserId={target_user_id}&pageNum=1&pageSize=20",
            headers=self.headers,
            name="获取聊天记录"
        )
    
    @task(2)
    def get_conversation_list(self):
        """
        获取会话列表（权重2）
        """
        if not self.token:
            return
        
        self.client.get(
            "/api/conversation/list",
            headers=self.headers,
            name="获取会话列表"
        )
    
    @task(1)
    def get_user_info(self):
        """
        获取用户信息（权重1）
        """
        if not self.token:
            return
        
        self.client.get(
            "/api/user/info",
            headers=self.headers,
            name="获取用户信息"
        )
    
    @task(1)
    def search_friend(self):
        """
        搜索好友（权重1）
        """
        if not self.token:
            return
        
        keywords = ["test", "user", "admin", "demo"]
        keyword = random.choice(keywords)
        
        self.client.get(
            f"/api/friend/search?keyword={keyword}&pageNum=1&pageSize=20",
            headers=self.headers,
            name="搜索好友"
        )
    
    def on_stop(self):
        """
        用户停止时执行：登出
        """
        if self.token:
            self.client.post(
                "/api/user/logout",
                headers=self.headers,
                name="用户登出"
            )


class AdminUser(HttpUser):
    """
    管理员用户行为模拟（可选）
    """
    
    wait_time = between(5, 10)
    
    @task
    def get_user_list(self):
        """获取用户列表"""
        self.client.get(
            "/api/user/list?pageNum=1&pageSize=20",
            name="获取用户列表"
        )


# ==================== 自定义事件 ====================

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """测试开始时打印信息"""
    print("\n" + "=" * 60)
    print("🚀 Fleets IM 性能测试开始")
    print(f"📍 目标地址: {environment.host}")
    print(f"👥 用户数: {environment.runner.target_user_count if hasattr(environment.runner, 'target_user_count') else '未知'}")
    print("=" * 60 + "\n")


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """测试结束时打印统计"""
    print("\n" + "=" * 60)
    print("🏁 Fleets IM 性能测试结束")
    
    if environment.stats.total.num_requests > 0:
        print(f"📊 总请求数: {environment.stats.total.num_requests}")
        print(f"❌ 失败请求: {environment.stats.total.num_failures}")
        print(f"📈 平均响应时间: {environment.stats.total.avg_response_time:.2f}ms")
        print(f"⚡ 最大响应时间: {environment.stats.total.max_response_time:.2f}ms")
        print(f"🎯 RPS: {environment.stats.total.total_rps:.2f}")
    
    print("=" * 60 + "\n")


@events.request.add_listener
def on_request(request_type, name, response_time, response_length, exception, **kwargs):
    """
    每个请求完成时的回调（可选）
    用于自定义日志或监控
    """
    if exception:
        print(f"❌ 请求失败: {name} - {exception}")


# ==================== 自定义形状（可选）====================

from locust import LoadTestShape

class StepLoadShape(LoadTestShape):
    """
    阶梯式负载测试
    逐步增加用户数，观察系统在不同负载下的表现
    """
    
    step_time = 60  # 每个阶段持续60秒
    step_load = 20  # 每个阶段增加20个用户
    spawn_rate = 5  # 每秒启动5个用户
    time_limit = 600  # 总测试时间10分钟
    
    def tick(self):
        run_time = self.get_run_time()
        
        if run_time > self.time_limit:
            return None
        
        current_step = run_time // self.step_time
        return (current_step + 1) * self.step_load, self.spawn_rate


# ==================== 使用说明 ====================

"""
1. 基础运行（Web UI）：
   locust -f locustfile.py --host=http://localhost:8080
   然后访问 http://localhost:8089

2. 无头模式（命令行）：
   locust -f locustfile.py --host=http://localhost:8080 \
     --users 100 \
     --spawn-rate 10 \
     --run-time 5m \
     --headless \
     --html report.html

3. 分布式测试：
   # Master节点
   locust -f locustfile.py --master --host=http://localhost:8080
   
   # Worker节点
   locust -f locustfile.py --worker --master-host=192.168.1.100

4. 使用自定义负载形状：
   locust -f locustfile.py --host=http://localhost:8080 --headless

5. 指定用户类：
   locust -f locustfile.py --host=http://localhost:8080 IMUser

参数说明：
--users: 并发用户数
--spawn-rate: 每秒启动的用户数
--run-time: 测试运行时间（如：5m, 1h）
--headless: 无头模式（不启动Web UI）
--html: 生成HTML报告
--csv: 生成CSV报告
"""
