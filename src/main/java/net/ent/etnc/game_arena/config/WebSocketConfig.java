package net.ent.etnc.game_arena.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtInterceptor webSocketJwtInterceptor;

    public WebSocketConfig(
            WebSocketJwtInterceptor webSocketJwtInterceptor
    ) {
        this.webSocketJwtInterceptor = webSocketJwtInterceptor;
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry config
    ) {
        config.enableSimpleBroker("/topic");

        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://172.16.64.192:5173"
                )
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    ) {
        registration.interceptors(
                webSocketJwtInterceptor
        );
    }
}