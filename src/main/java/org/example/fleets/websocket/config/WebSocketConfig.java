package org.example.fleets.websocket.config;

import lombok.RequiredArgsConstructor;
import org.example.fleets.websocket.handler.WebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置
 * 使用 STOMP 协议
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    
    /**
     * 配置消息代理
     * TODO: 配置消息代理路径
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // TODO: 实现消息代理配置
        // 提示：配置 /topic（广播）、/queue（点对点）
    }
    
    /**
     * 注册 STOMP 端点
     * TODO: 注册 WebSocket 端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // TODO: 实现端点注册
        // 提示：添加端点、跨域配置、握手拦截器、SockJS 支持
    }
    
    /**
     * 配置客户端入站通道拦截器
     * TODO: 添加认证拦截器（可选）
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // TODO: 添加拦截器进行认证、日志等
    }
}
