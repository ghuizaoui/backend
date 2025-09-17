// src/main/java/com/mercedes/workflowrh/config/WebSocketConfig.java
package com.mercedes.workflowrh.config;

import com.mercedes.workflowrh.security.AppUserDetailsService;
import com.mercedes.workflowrh.security.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final AppUserDetailsService userDetailsService;

    public WebSocketConfig(JwtUtil jwtUtil, AppUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                                   WebSocketHandler wsHandler, Map<String, Object> attrs) {
                        if (req instanceof ServletServerHttpRequest servlet) {
                            String token = servlet.getServletRequest().getParameter("token");
                            if (token != null && !token.isBlank()) attrs.put("token", token);
                        }
                        return true;
                    }
                })
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

                    // 1) Authorization header STOMP
                    String token = null;
                    List<String> auth = accessor.getNativeHeader("Authorization");
                    if (auth != null && !auth.isEmpty()) {
                        String h = auth.get(0);
                        if (h.startsWith("Bearer ")) token = h.substring(7);
                    }
                    // 2) Query param ajouté au handshake (SockJS)
                    if (token == null) {
                        Object t = accessor.getSessionAttributes().get("token");
                        if (t instanceof String s && !s.isBlank()) token = s;
                    }

                    // Authentifier le CONNECT STOMP
                    if (token != null && jwtUtil.validateToken(token, false)) {
                        String username = jwtUtil.getUsernameFromToken(token, false);
                        UserDetails user = userDetailsService.loadUserByUsername(username);
                        Authentication authentication =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        accessor.setUser(authentication);
                    } else {
                        // Si tu veux refuser la connexion quand pas/plus de token :
                        // return null;
                    }
                }
                return message;
            }
        });
    }
}
