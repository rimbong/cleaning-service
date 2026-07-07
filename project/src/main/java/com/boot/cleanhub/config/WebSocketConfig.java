package com.boot.cleanhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * <pre>
 *   STOMP over WebSocket 설정.
 *   레거시(HIS)의 저수준 WebSocketHandler(세션 Set 수동 관리)를 대신해,
 *   Spring 표준 메시지 브로커 방식으로 재구현한다.
 *
 *   - 클라이언트 연결 엔드포인트: /ws-chat (SockJS fallback 지원)
 *   - 발행(클→서): /app/**     (@MessageMapping 으로 라우팅)
 *   - 구독(서→클): /topic/**   (인메모리 SimpleBroker 가 브로드캐스트)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        // 서버 → 클라이언트 구독 경로 (인메모리 브로커)
        registry.enableSimpleBroker("/topic");
        // 클라이언트 → 서버 발행 경로 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }
}
