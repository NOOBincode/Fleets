package org.example.fleets.websocket.config;

import lombok.RequiredArgsConstructor;
import org.example.fleets.common.config.properties.FleetsProperties;
import org.example.fleets.websocket.handler.WebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置
 * 使用 STOMP 协议。至少需注册端点和消息代理，否则 SubProtocolWebSocketHandler 会报 "No handlers" 导致应用无法启动。
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final FleetsProperties fleetsProperties;

    /**
     * 配置消息代理：应用目标前缀（/app）与 @MessageMapping 对应，/topic、/queue 供服务端推送。
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes(fleetsProperties.getWebsocket().getApplicationDestinationPrefix());
        registry.setUserDestinationPrefix(fleetsProperties.getWebsocket().getUserDestinationPrefix());
        registry.enableSimpleBroker("/topic", "/queue");
    }

    /**
     * 注册 STOMP 端点，否则 WebSocket 基础设施无可用 handler，启动会报 "No handlers"。
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(fleetsProperties.getWebsocket().getEndpoint())
                .setAllowedOrigins(fleetsProperties.getWebsocket().getAllowedOrigins())
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
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
