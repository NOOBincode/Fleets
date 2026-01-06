local limit_req = require "resty.limit.req"
local cjson = require "cjson"

-- 限流配置：每秒10个请求，突发20个请求
local lim, err = limit_req.new("rate_limit", 10, 20)
if not lim then
    ngx.log(ngx.ERR, "创建限流器失败: ", err)
    return
end

-- 获取客户端标识（可以是IP或用户ID）
local key = ngx.var.remote_addr
local user_id = ngx.req.get_headers()["X-User-ID"]
if user_id then
    key = "user:" .. user_id
end

-- 执行限流检查
local delay, err = lim:incoming(key, true)
if not delay then
    if err == "rejected" then
        ngx.status = 429
        ngx.header.content_type = "application/json; charset=utf-8"
        ngx.say(cjson.encode({
            code = 429,
            message = "请求过于频繁，请稍后再试"
        }))
        return ngx.exit(429)
    end
    ngx.log(ngx.ERR, "限流检查失败: ", err)
    return
end

-- 如果有延迟，记录日志
if delay >= 0.001 then
    ngx.log(ngx.WARN, "请求被延迟: ", delay, "秒, key: ", key)
end
