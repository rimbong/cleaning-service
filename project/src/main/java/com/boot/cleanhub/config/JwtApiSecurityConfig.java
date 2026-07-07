package com.boot.cleanhub.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.boot.cleanhub.common.handler.JwtAccessDeniedHandler;
import com.boot.cleanhub.common.handler.JwtAuthenticationEntryPoint;
import com.boot.cleanhub.filter.JwtRequestFilter;

/**
 * <pre>
 *   [JWT 기반 보안 체인] — @Order(1), 담당 구역: /api/**
 *
 *   ■ 이중 체인의 큰 그림은 SessionSecurityConfig 상단 주석 참고.
 *     이 체인은 requestMatchers 로 "/api/** 만 내 담당"이라 선언했다.
 *     @Order(1) 이라 세션 체인보다 먼저 검사 → /api/** 는 전부 여기서 처리.
 *
 *   ■ 이 체인의 성격: 무상태(STATELESS) API 존
 *     - 세션을 만들지도, 쓰지도 않는다. 매 요청의 Authorization: Bearer {JWT} 가 신분증.
 *     - 토큰 검증은 JwtRequestFilter 가 수행(서명+만료 확인 후 SecurityContext 에 등록).
 *     - 이 체인은 아이디/비밀번호 인증을 하지 않으므로 AuthenticationManager/Provider 가 없다.
 *       (로그인은 세션 존에서 하고, 여기선 이미 발급된 토큰만 검증)
 *     - 미인증(토큰 없음/무효) → JwtAuthenticationEntryPoint(401 JSON),
 *       인가 실패(권한 부족) → JwtAccessDeniedHandler(403 JSON).
 *
 *   ■ 토큰 획득·수명 관리 (세션 존과 협력)
 *     - 발급: POST /auth/api/token (세션 로그인 후, 세션 존 AuthApiController)
 *     - 갱신: POST /api/auth/refresh (DB의 refresh 토큰 대조 후 재발급)
 *     - 폐기: POST /api/auth/logout  (DB의 refresh 토큰 삭제)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.18
 * @version 3.0 (2026.07 — PKCE/jwt_user 데모 배선 제거, Bearer 검증 전용으로 단순화)
 */
@Configuration
@EnableWebSecurity
@Order(1)
public class JwtApiSecurityConfig {

    /** 매 요청의 Bearer 토큰을 검증해 SecurityContext 에 인증을 등록하는 필터 */
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    /** 미인증(토큰 없음/무효) 접근에 401 JSON 을 돌려주는 응답기 */
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /** 인증은 됐으나 권한 부족(예: hasRole 미달)일 때 403 JSON 을 돌려주는 응답기 */
    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS 허용 — 정책은 CorsConfig 참고
            .cors(Customizer.withDefaults())

            // CSRF 비활성 — CSRF 는 "브라우저가 쿠키를 자동 첨부"하는 걸 악용하는 공격.
            // JWT 는 헤더로 직접 실어 보내므로(자동 첨부 없음) CSRF 영향이 없다.
            .csrf(csrf -> csrf.disable())

            // ★ 이 체인의 담당 구역 선언: /api/** 만. (그 외 URL 은 이 체인을 아예 안 탐)
            .requestMatchers(matchers -> matchers.antMatchers("/api/**"))

            // ── URL 별 접근 규칙 (위에서부터 순서대로 매칭 — 구체적인 것 먼저) ──
            .authorizeHttpRequests(authz -> authz
                // 토큰이 "없어도" 되는 공개 경로: 갱신·폐기
                .antMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                // 관리자 전용 API — ROLE_ADMIN 권한 필요(토큰의 roles 클레임 기준).
                //  hasRole("ADMIN") = 권한 "ROLE_ADMIN" 보유 검사(스프링이 ROLE_ 접두사 자동 처리).
                //  권한 미달 시 AccessDeniedException → jwtAccessDeniedHandler 가 403 JSON.
                //  (cleanhub 의 관리자 전용 API 는 /api/admin/** 아래에 두면 이 규칙이 적용됨)
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                // 그 외 /api/** 전부 = 유효한 Bearer 토큰 필수(권한은 안 봄)
                .anyRequest().authenticated())

            // ★ 무상태 선언: 세션을 만들지도 쓰지도 않음. JWT 존의 핵심 설정.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 예외 처리: 미인증(토큰 없음/무효)=401, 인가 실패(권한 부족)=403 — 둘 다 JSON
            .exceptionHandling(except -> except
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler));

        // ★ JWT 검증 필터를 체인에 삽입.
        //   UsernamePasswordAuthenticationFilter "앞"에 두어, 아이디/비밀번호 인증보다
        //   먼저 Bearer 토큰을 확인하게 한다(토큰이 유효하면 그걸로 인증 끝).
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
