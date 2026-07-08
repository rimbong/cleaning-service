package com.boot.cleanhub.auth.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.auth.dto.AuthResult;
import com.boot.cleanhub.auth.dto.AuthenticationResponse;
import com.boot.cleanhub.auth.service.TokenService;
import com.boot.cleanhub.auth.support.RefreshTokenCookie;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   [로그인 이후] 세션 인증 → JWT 발급 창구.
 *
 *   이 컨트롤러는 "세션 존"에 있다(경로가 /api/** 가 아니므로 SessionSecurityConfig 담당).
 *   /auth/api/** 는 세션 인증 필수 → 폼 로그인(POST /auth/login)으로 세션을 얻은 사용자만 호출 가능.
 *
 *   ■ 인증 구조(세션 앵커 + JWT API + refresh 쿠키)
 *     - 세션(HttpOnly): 브라우저가 열려 있는 동안의 로그인 상태·사용자정보의 진실.
 *     - JWT access(메모리): /api/** 호출용 Bearer. 짧은 수명.
 *     - refresh(HttpOnly 쿠키 + DB): "로그인 유지(자동로그인)"의 장기 신분증.
 *       → 브라우저를 껐다 켜(세션 소멸) 도, 이 쿠키로 /api/auth/refresh 하면 access 재발급 = 자동로그인.
 *
 *   ■ 발급 흐름
 *     1) POST /auth/login (form)           → 세션 획득
 *     2) POST /auth/api/token?rememberMe=  → access(body) + refresh(HttpOnly 쿠키) ★여기
 *     3) GET  /api/**  (Bearer access)     → JWT 존 호출
 *     4) access 만료/새로고침/재시작        → POST /api/auth/refresh (쿠키로) → 새 access
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.03
 * @version 2.0 (2026.07.08 — refresh 를 HttpOnly 쿠키로 발급, rememberMe 자동로그인 도입)
 */
@RestController
@RequestMapping("/auth/api")
@RequiredArgsConstructor
public class AuthApiController {

    private final TokenService tokenService;
    private final RefreshTokenCookie refreshTokenCookie;

    /**
     * 현재 세션 로그인 상태 확인.
     * Authentication 파라미터는 스프링이 SecurityContext(=세션에 저장된 인증)에서 주입해 준다.
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", authentication.getName());
        data.put("roles", roles);
        return ApiResponse.ok(data);
    }

    /**
     * 세션 인증 사용자에게 JWT 발급.
     * 아이디/비밀번호를 다시 받지 않는다 — "세션이 곧 인증 증명"이기 때문.
     * access 는 body 로, refresh 는 HttpOnly 쿠키로 내려간다(refresh 는 JS 노출 금지).
     *
     * @param rememberMe true=자동로그인(영속 쿠키) / false=세션 쿠키
     */
    @PostMapping("/token")
    public ApiResponse<AuthResult> issueToken(Authentication authentication,
            @RequestParam(defaultValue = "false") boolean rememberMe,
            HttpServletResponse response) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        String username = authentication.getName();

        AuthenticationResponse issued = tokenService.issue(username, roles);
        // refresh 는 쿠키로만 전달(body 에 넣지 않음)
        refreshTokenCookie.write(response, issued.getRefreshToken(), rememberMe);

        return ApiResponse.ok(new AuthResult(issued.getAccessToken(), username, roles),
                "JWT 가 발급되었습니다.");
    }
}
