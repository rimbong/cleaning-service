package com.boot.cleanhub.common.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.boot.cleanhub.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <pre>
 *   JWT 존(/api/**)의 "인가 실패(403)" 응답기.
 *
 *   [EntryPoint(401) 와의 차이 — 헷갈리기 쉬움]
 *     - AuthenticationEntryPoint : "너 누구야?" 가 해결 안 됨 → 401 Unauthorized
 *       (토큰 없음/무효 — JwtAuthenticationEntryPoint 담당)
 *     - AccessDeniedHandler      : "누군진 알겠는데, 권한이 없어" → 403 Forbidden
 *       (인증은 됐지만 hasRole 등 조건 미달 — 이 클래스 담당)
 *
 *   둘 다 JwtApiSecurityConfig 의 exceptionHandling 에 등록해야 동작한다.
 *   응답은 표준 envelope(ApiResponse)로 통일.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.06
 * @version 1.0
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("FORBIDDEN", "이 리소스에 접근할 권한이 없습니다.")));
        response.getWriter().flush();
    }
}
