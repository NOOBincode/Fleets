package org.example.fleets.websocket.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class WsHandshakeHandler extends DefaultHandshakeHandler {
    private static final String ATTR_USER_ID = "userId";

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String,Object> attributes) {
        Object userIdObj =attributes.get(ATTR_USER_ID);
        if (userIdObj == null) {
            return null;
        }
        Long userId = userIdObj instanceof Long ? (Long) userIdObj : Long.parseLong(String.valueOf(userIdObj));
        return () -> String.valueOf(userId);
    }
}
