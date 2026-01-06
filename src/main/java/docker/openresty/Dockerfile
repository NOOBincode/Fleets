# 使用官方OpenResty镜像作为基础镜像
FROM openresty/openresty:1.19.3.2-alpine

# 设置工作目录
WORKDIR /usr/local/openresty

# 安装必要的依赖
RUN apk add --no-cache \
    git \
    curl \
    gcc \
    libc-dev \
    make \
    openssl-dev \
    pcre-dev \
    zlib-dev \
    linux-headers \
    gnupg \
    libxslt-dev \
    gd-dev \
    perl-dev

# 安装LuaRocks（Lua包管理器）
RUN curl -fsSL https://luarocks.org/releases/luarocks-3.8.0.tar.gz -o luarocks.tar.gz \
    && tar zxpf luarocks.tar.gz \
    && cd luarocks-3.8.0 \
    && ./configure --prefix=/usr/local/openresty/luajit \
        --with-lua=/usr/local/openresty/luajit/ \
        --lua-suffix=jit \
        --with-lua-include=/usr/local/openresty/luajit/include/luajit-2.1 \
    && make build \
    && make install \
    && cd .. \
    && rm -rf luarocks-3.8.0 luarocks.tar.gz

# 安装必要的Lua模块
RUN /usr/local/openresty/luajit/bin/luarocks install lua-cjson \
    && /usr/local/openresty/luajit/bin/luarocks install lua-resty-jwt \
    && /usr/local/openresty/luajit/bin/luarocks install lua-resty-http \
    && /usr/local/openresty/luajit/bin/luarocks install lua-resty-websocket \
    && /usr/local/openresty/luajit/bin/luarocks install lua-resty-redis

# 创建Nginx配置目录
RUN mkdir -p /usr/local/openresty/nginx/conf/conf.d \
    && mkdir -p /usr/local/openresty/nginx/conf/stream.d \
    && mkdir -p /usr/local/openresty/nginx/lua \
    && mkdir -p /usr/local/openresty/nginx/logs


COPY ./conf/nginx.conf /usr/local/openresty/nginx/conf/nginx.conf
COPY ./conf/conf.d/ /usr/local/openresty/nginx/conf/conf.d/
COPY ./lua/ /usr/local/openresty/nginx/lua/

# 设置环境变量
ENV PATH=$PATH:/usr/local/openresty/luajit/bin:/usr/local/openresty/nginx/sbin:/usr/local/openresty/bin

# 暴露端口
EXPOSE 80 443 9001

# 设置健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# 启动OpenResty
CMD ["/usr/local/openresty/bin/openresty", "-g", "daemon off;"]