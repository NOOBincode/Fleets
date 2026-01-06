local jwt = require "resty.jwt"
local cjson = require "cjson"

-- 从请求头获取JWT令牌
local auth_header = ngx.req.get_headers()["Authorization"]
if not auth_header then
    ngx.status = 401
    ngx.header.content_type = "application/json; charset=utf-8"
    ngx.say(cjson.encode({code = 401, message = "未授权：缺少认证令牌"}))
    return ngx.exit(401)
end

-- 提取令牌
local _, _, token = string.find(auth_header, "Bearer%s+(.+)")
if not token then
    ngx.status = 401
    ngx.header.content_type = "application/json; charset=utf-8"
    ngx.say(cjson.encode({code = 401, message = "未授权：令牌格式错误"}))
    return ngx.exit(401)
end

-- 验证JWT令牌
local jwt_secret = "your-jwt-secret-key" -- 应该从配置或环境变量中获取
local jwt_obj = jwt:verify(jwt_secret, token)
if not jwt_obj.verified then
    ngx.status = 401
    ngx.header.content_type = "application/json; charset=utf-8"
    ngx.say(cjson.encode({code = 401, message = "未授权：无效的令牌", error = jwt_obj.reason}))
    return ngx.exit(401)
end

-- 将用户信息添加到请求头
if jwt_obj.payload and jwt_obj.payload.sub then
    ngx.req.set_header("X-User-ID", jwt_obj.payload.sub)
end

-- 检查令牌是否已被撤销（可选，需要与Redis集成）
local redis = require "resty.redis"
local red = redis:new()
red:set_timeout(1000) -- 1秒超时

local ok, err = red:connect("im-redis", 6379)
if not ok then
    ngx.log(ngx.ERR, "无法连接到Redis: ", err)
    return -- 继续处理请求，即使Redis连接失败
end

-- 检查令牌是否在黑名单中
local is_blacklisted, err = red:get("token_blacklist:" .. token)
if is_blacklisted then
    ngx.status = 401
    ngx.header.content_type = "application/json; charset=utf-8"
    ngx.say(cjson.encode({code = 401, message = "未授权：令牌已被撤销"}))
    return ngx.exit(401)
end

-- 将连接放回连接池
local ok, err = red:set_keepalive(10000, 100)
if not ok then
    ngx.log(ngx.ERR, "无法将Redis连接放回连接池: ", err)
end