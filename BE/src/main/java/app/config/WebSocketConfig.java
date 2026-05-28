package app.config;

import app.auth.security.JwtTokenProvider;
import app.auth.security.CustomUserDetailsService;
import app.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            if (tokenProvider.validateToken(token)) {
                                // 1. Lấy Email từ Token (thay vì ID)
                                String email = tokenProvider.getEmailFromToken(token);

                                // 2. Load User từ DB bằng Email
                                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                                // 3. Tạo Authentication
                                Principal userPrincipal = new Principal() {
                                    @Override
                                    public String getName() {
                                        if (userDetails instanceof UserPrincipal) {
                                            return String.valueOf(((UserPrincipal) userDetails).getId());
                                        }
                                        return userDetails.getUsername(); // Fallback
                                    }
                                };

                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(userPrincipal, null, userDetails.getAuthorities());

                                accessor.setUser(auth);
                                log.info("User connected WS: {}", email);
                            }
                        } catch (Exception e) {
                            log.error("WebSocket Auth Error: {}", e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}