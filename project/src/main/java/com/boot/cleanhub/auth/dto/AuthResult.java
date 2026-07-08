package com.boot.cleanhub.auth.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   로그인/발급/갱신 응답 — 프론트가 로그인 상태를 복원하는 데 필요한 최소 정보.
 *   refresh 토큰은 HttpOnly 쿠키로 내려가므로 여기 body 에는 포함하지 않는다(노출 금지).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Getter
public class AuthResult {

    /** API 호출용 access 토큰(프론트는 메모리에 보관해 Bearer 로 사용) */
    private final String accessToken;

    /** 로그인 사용자명 */
    private final String username;

    /** 권한 목록(예: ["ROLE_ADMIN"]) — 프론트의 isAdmin 판별 등에 사용 */
    private final List<String> roles;

    public AuthResult(String accessToken, String username, List<String> roles) {
        this.accessToken = accessToken;
        this.username = username;
        this.roles = roles;
    }
}
