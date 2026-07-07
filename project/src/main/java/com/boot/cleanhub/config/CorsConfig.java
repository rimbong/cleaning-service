package com.boot.cleanhub.config;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   CORS 설정.
 *   React/Vue 개발 서버(다른 오리진)에서 API 를 호출할 수 있도록 허용한다.
 *   허용 오리진은 CorsProperties(cors.allowed-origins, 콤마 구분)로 관리한다.
 *
 *   Spring Security 가 먼저 동작하므로, 이 CorsConfigurationSource 빈을 SecurityFilterChain 의
 *   .cors() 가 사용하도록 연동되어 있다(JwtApiSecurityConfig / SessionSecurityConfig).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.01
 * @version 1.1
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // allowCredentials(true) 와 함께 쓰려면 '*' 대신 originPatterns 를 사용해야 한다.
        config.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "accessToken", "refreshToken"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
