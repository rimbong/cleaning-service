package com.boot.cleanhub.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.boot.cleanhub.filter.JwtRequestFilter;

/**
 * <pre>
 *   서블릿 필터 등록/해제 설정.
 *
 *   ※ 스프링 부트의 암묵 동작: Filter 를 @Component 로 두면 시큐리티 체인 삽입과
 *     "별개로" 모든 요청의 일반 서블릿 필터로도 자동 등록된다.
 *     (OncePerRequestFilter 라 이중 실행은 안 되지만, /api/** 밖 요청까지
 *      Bearer 헤더 파싱을 시도하게 됨)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.18
 * @version 2.0 (2026.07.03 — JwtRequestFilter 자동 등록 비활성)
 */
@Configuration
public class FilterConfig {

    /**
     * JwtRequestFilter 의 서블릿 컨테이너 자동 등록을 끈다.
     * 이 필터는 JwtApiSecurityConfig 가 /api/** 시큐리티 체인 안에 직접 삽입하므로,
     * 전역(모든 요청) 필터로 중복 배치될 필요가 없다.
     * setEnabled(false) = "빈으로는 두되, 일반 필터 체인에는 넣지 마라"는 뜻.
     */
    @Bean
    public FilterRegistrationBean<JwtRequestFilter> jwtRequestFilterRegistration(JwtRequestFilter filter) {
        FilterRegistrationBean<JwtRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
