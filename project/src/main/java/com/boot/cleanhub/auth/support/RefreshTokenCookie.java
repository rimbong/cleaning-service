package com.boot.cleanhub.auth.support;

import java.time.Duration;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * <pre>
 *   refresh 토큰을 담는 HttpOnly 쿠키 관리자.
 *
 *   ■ 왜 HttpOnly 쿠키인가
 *     refresh 토큰은 "로그인 유지(자동로그인)"의 장기 신분증이라 탈취 시 피해가 크다.
 *     - HttpOnly: 자바스크립트가 읽지 못함 → XSS 로도 못 훔침(localStorage 대비 강점)
 *     - SameSite=Lax: 타 사이트에서 온 요청엔 쿠키 미첨부 → CSRF 완화
 *     - Secure: HTTPS 에서만 전송(운영). 개발(http://localhost)은 false 로 둔다(설정값).
 *     - Path=/ : 세션 존(/auth/api/token 발급)과 JWT 존(/api/auth/refresh 갱신) 모두에 전송
 *
 *   ■ 로그인 유지(rememberMe)
 *     - true  : Max-Age = refresh 만료(기본 7일) → 브라우저를 껐다 켜도 남아 자동로그인
 *     - false : Max-Age 미지정(세션 쿠키) → 브라우저를 닫으면 사라짐(그 세션에서만 유지)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Component
public class RefreshTokenCookie {

    /** 쿠키 이름 — 갱신 시 @CookieValue 로 읽는 키와 동일해야 한다 */
    public static final String NAME = "refreshToken";

    /** HTTPS 전용 여부. 운영(prod)은 env(REFRESH_COOKIE_SECURE=true)로 켠다. 개발 기본 false. */
    @Value("${jwt.refresh-cookie.secure:false}")
    private boolean secure;

    /** refresh 만료(ms) — rememberMe 쿠키의 Max-Age 로 사용 */
    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * refresh 토큰을 쿠키로 응답에 심는다.
     *
     * @param response   HTTP 응답
     * @param token      refresh 토큰 값
     * @param rememberMe true=영속(자동로그인) / false=세션 쿠키
     */
    public void write(HttpServletResponse response, String token, boolean rememberMe) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/");
        if (rememberMe) {
            builder.maxAge(Duration.ofMillis(refreshExpirationMs));
        }
        // rememberMe=false 이면 Max-Age 를 지정하지 않아 "세션 쿠키"가 된다(브라우저 종료 시 삭제).
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    /**
     * refresh 쿠키를 삭제한다(로그아웃 등). Max-Age=0 으로 즉시 만료시킨다.
     *
     * @param response HTTP 응답
     */
    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
