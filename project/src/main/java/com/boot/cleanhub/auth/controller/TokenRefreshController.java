package com.boot.cleanhub.auth.controller;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.auth.dto.AuthResult;
import com.boot.cleanhub.auth.domain.RefreshToken;
import com.boot.cleanhub.auth.dto.AuthenticationResponse;
import com.boot.cleanhub.auth.repository.RefreshTokenRepository;
import com.boot.cleanhub.auth.service.TokenService;
import com.boot.cleanhub.auth.support.RefreshTokenCookie;
import com.boot.cleanhub.util.jwt.JwtUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   [토큰 수명 관리] 갱신(refresh) / 폐기(logout).
 *
 *   ■ 왜 refresh 는 DB 대조를 하나 (access 는 안 하는데)
 *     access 는 수명이 짧아(기본 1시간) 서명 검증만으로 충분하지만,
 *     refresh 는 수명이 길어(기본 7일) 탈취 시 피해가 크다.
 *     → DB(jwt_refresh_token)에 저장된 것과 대조해, 로그아웃/재로그인 시
 *       즉시 무효화(삭제/교체)할 수 있게 한다.
 *
 *   ■ refresh 갱신 정책 — 옵션 (etc.properties: jwt.refresh-token-rotation)
 *     [true — 회전(rotation)]
 *       갱신할 때마다 refresh 도 새로 발급하고 구 토큰은 폐기(1회용).
 *       · 탈취 감지: 도둑과 정상 사용자 중 늦게 쓴 쪽이 401 → 유출 사실이 드러남
 *       · 수명: 새 refresh 가 매번 7일짜리 → "마지막 사용 후 7일" 비활동 연장형
 *         (계속 쓰는 한 로그인 유지, 7일 방치하면 로그아웃)
 *     [false — 고정]
 *       access 만 갱신, refresh 는 만료까지 재사용.
 *       · 단순함. 수명: "발급 후 7일" 절대 만료형(아무리 써도 7일째 재로그인)
 *       · 탈취된 refresh 가 만료까지 유효한 위험은 감수
 *     ※ 클라이언트는 응답의 refreshToken 을 항상 저장하도록 구현하면 두 모드 모두 호환
 *       (회전이면 새 값, 고정이면 같은 값이 내려온다).
 *
 *   ■ 과거 버그 수정(2026.07): 만료된 refresh 토큰이 오면 파싱 단계에서
 *     ExpiredJwtException 이 터져 500 이 났다 → catch 해서 401 + DB 정리로 변경.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.18
 * @version 2.1 (2026.07.03 — refresh 회전을 설정 옵션으로 도입)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenRefreshController {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final RefreshTokenCookie refreshTokenCookie;

    /** refresh 갱신 정책(true=회전/false=고정) — 클래스 상단 주석 참고 */
    @Value("${jwt.refresh-token-rotation:false}")
    private boolean refreshRotation;

    /**
     * Access Token 갱신(= 자동로그인 복원).
     * refresh 토큰은 HttpOnly 쿠키에서 읽는다(body 아님). 앱 시작·새로고침·재시작 시 프론트가
     * 이 엔드포인트를 호출해 access 를 재발급받고 로그인 상태를 복원한다.
     *
     * @param refreshTokenValue HttpOnly 쿠키의 refresh 토큰(없으면 미로그인 → 401)
     * @param rememberMe        회전 시 새 쿠키의 영속 여부를 유지하기 위한 힌트(기본 true=유지)
     * @return access(body) + username + roles. 회전이면 새 refresh 를 쿠키로 재발급.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResult>> refreshAccessToken(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshTokenValue,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") boolean rememberMe,
            HttpServletResponse response) {
        // 0) 쿠키 자체가 없음 = 로그인 이력 없음(또는 로그인 유지 안 함) → 401
        if (!StringUtils.hasText(refreshTokenValue)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("REFRESH_MISSING", "로그인이 필요합니다."));
        }

        // 1) DB 대조 — 로그아웃/재로그인/회전으로 삭제·교체된 토큰이면 여기서 걸러진다
        Optional<RefreshToken> stored = refreshTokenRepository.findByToken(refreshTokenValue);
        if (!stored.isPresent()) {
            refreshTokenCookie.clear(response); // 무효 쿠키 정리
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("REFRESH_INVALID", "유효하지 않은 인증입니다. 다시 로그인해 주세요."));
        }

        try {
            // 2) 서명+만료 검증(파싱) — 만료 시 ExpiredJwtException 발생
            Claims claims = jwtUtil.parseClaims(refreshTokenValue);

            // 2-1) refresh 타입만 인정 — access 토큰으로 갱신을 시도하는 오용 차단
            if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(jwtUtil.getTokenType(claims))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("REFRESH_INVALID", "유효하지 않은 인증입니다."));
            }

            String username = claims.getSubject();
            List<String> roles = jwtUtil.getRoles(claims);

            // 3) 정책에 따라 발급
            String newAccessToken;
            if (refreshRotation) {
                // [회전] access+refresh 모두 새로 발급 → 새 refresh 를 쿠키로 교체(구 토큰은 DB upsert 로 무효)
                AuthenticationResponse issued = tokenService.issue(username, roles);
                newAccessToken = issued.getAccessToken();
                refreshTokenCookie.write(response, issued.getRefreshToken(), rememberMe);
            } else {
                // [고정] 새 access 만 발급, refresh 쿠키는 그대로 둔다
                newAccessToken = jwtUtil.generateAccessToken(username, roles);
            }
            return ResponseEntity.ok(ApiResponse.ok(new AuthResult(newAccessToken, username, roles)));
        } catch (ExpiredJwtException e) {
            // 만료된 refresh 는 더 못 쓰므로 DB·쿠키 정리 → 클라이언트는 재로그인
            refreshTokenRepository.delete(stored.get());
            refreshTokenCookie.clear(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("REFRESH_EXPIRED", "로그인이 만료되었습니다. 다시 로그인해 주세요."));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("REFRESH_INVALID", "유효하지 않은 인증입니다. 다시 로그인해 주세요."));
        }
    }

    /**
     * JWT 로그아웃 = refresh 토큰 폐기(DB 삭제 + 쿠키 삭제).
     * 이후 그 refresh 로는 갱신 불가. access 는 무상태라 남은 수명 동안만 유효하다.
     * (세션 로그아웃은 별도: POST /auth/logout — SessionSecurityConfig)
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshTokenValue,
            HttpServletResponse response) {
        if (StringUtils.hasText(refreshTokenValue)) {
            refreshTokenRepository.findByToken(refreshTokenValue)
                    .ifPresent(refreshTokenRepository::delete);
        }
        refreshTokenCookie.clear(response);
        // 존재하지 않는 토큰이어도 성공 응답(이미 폐기된 상태와 동일한 결과이므로)
        return ApiResponse.ok(null, "로그아웃(토큰 폐기) 되었습니다.");
    }
}
