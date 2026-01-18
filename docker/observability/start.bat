@echo off
chcp 65001 >nul
echo ==========================================
echo   Fleets 可观测性技术栈启动脚本
echo ==========================================
echo.

REM 检查Docker是否运行
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker未运行，请先启动Docker Desktop
    pause
    exit /b 1
)

echo ✅ Docker运行正常
echo.

REM 创建日志目录
echo 📁 创建日志目录...
if not exist "..\..\logs" mkdir "..\..\logs"
echo ✅ 日志目录创建完成
echo.

REM 启动服务
echo 🚀 启动可观测性技术栈...
docker-compose up -d

REM 等待服务启动
echo.
echo ⏳ 等待服务启动（30秒）...
timeout /t 30 /nobreak >nul

REM 检查服务状态
echo.
echo 📊 服务状态：
docker-compose ps

echo.
echo ==========================================
echo   启动完成！
echo ==========================================
echo.
echo 访问地址：
echo   • Grafana:       http://localhost:3000 (admin/admin123)
echo   • Prometheus:    http://localhost:9090
echo   • AlertManager:  http://localhost:9093
echo.
echo 下一步：
echo   1. 启动Fleets应用
echo   2. 访问 http://localhost:9090/targets 检查指标采集
echo   3. 访问 http://localhost:3000 配置Dashboard
echo.
echo 查看日志：
echo   docker-compose logs -f
echo.
echo 停止服务：
echo   docker-compose down
echo.
pause
