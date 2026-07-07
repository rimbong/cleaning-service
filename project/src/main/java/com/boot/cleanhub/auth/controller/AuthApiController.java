package com.boot.cleanhub.auth.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.auth.dto.AuthenticationResponse;
import com.boot.cleanhub.auth.service.TokenService;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   [연습 시나리오] 세션 로그인 → JWT 발급 → API 존 호출
 *
 *   이 컨트롤러는 "세션 존"에 있다(경로가 /api/** 가 아니므로 SessionSecurityConfig 담당).
 *   /auth/api/** 는 세션 인증 필수로 설정돼 있어, 폼 로그인(POST /auth/login)으로
 *   세션(JSESSIONID)을 얻은 사용자만 호출할 수 있다.
 *
 *   전체 흐름:
 *     1) POST /auth/login  (form: username=user1&password=1234)  → 세션 쿠키 획득
 *     2) POST /auth/api/token  (세션 쿠키로 인증)               → JWT(access/refresh) 발급 ★여기
 *     3) GET  /api/hello  (Authorization: Bearer {accessToken})       → JWT 존 API 호출
 *     4) 만료 시 POST /api/auth/refresh  → access 재발급
 *     5) POST /api/auth/logout(refresh 폐기) + POST /auth/logout(세션 종료)
 *
 *   실무 대응: 사내 포털(세션 로그인) 사용자가 별도 API 존/게이트웨이용
 *   출입증(JWT)을 발급받아 호출하는 하이브리드 구성.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.03
 * @version 1.0
 */
@RestController
@RequestMapping("/auth/api")
@RequiredArgsConstructor
public class AuthApiController {

    private final TokenService tokenService;

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
     * 세션 인증 사용자에게 API 존(JWT) 토큰 발급.
     * 여기서는 아이디/비밀번호를 다시 받지 않는다 — "세션이 곧 인증 증명"이기 때문.
     * 세션 사용자의 권한(예: ROLE_USER)을 토큰 클레임에 실어, JWT 존에서도 인가가 되게 한다.
     * 발급(access+refresh 생성, refresh DB 저장)은 TokenService 가 담당.
     */
    @PostMapping("/token")
    public ApiResponse<AuthenticationResponse> issueToken(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return ApiResponse.ok(
                tokenService.issue(authentication.getName(), roles),
                "API 존(JWT) 토큰이 발급되었습니다.");
    }
}
