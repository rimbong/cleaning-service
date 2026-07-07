package com.boot.cleanhub.auth.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.auth.dto.AuthenticationResponse;
import com.boot.cleanhub.auth.domain.RefreshToken;
import com.boot.cleanhub.auth.repository.RefreshTokenRepository;
import com.boot.cleanhub.auth.service.TokenService;
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

    /** refresh 갱신 정책(true=회전/false=고정) — 클래스 상단 주석 참고 */
    @Value("${jwt.refresh-token-rotation:false}")
    private boolean refreshRotation;

    /**
     * Access Token 갱신.
     * body: { "refreshToken": "..." }
     * 응답: 회전(true)이면 새 access + "새" refresh / 고정(false)이면 새 access + 기존 refresh.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshAccessToken(
            @RequestBody Map<String, String> request) {
        String refreshTokenValue = request.get("refreshToken");

        // 1) DB 대조 — 로그아웃/재로그인/회전으로 삭제·교체된 토큰이면 여기서 걸러진다
        Optional<RefreshToken> stored = refreshTokenRepository.findByToken(refreshTokenValue);
        if (!stored.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("REFRESH_INVALID", "유효하지 않은 refresh 토큰입니다. 다시 로그인해 주세요."));
        }

        try {
            // 2) 서명+만료 검증(파싱) — 만료 시 ExpiredJwtException 발생
            Claims claims = jwtUtil.parseClaims(refreshTokenValue);

            // 2-1) refresh 타입만 인정 — access 토큰으로 갱신을 시도하는 오용 차단
            //      (DB 대조로도 걸러지지만 명시 검사로 의도를 분명히 — 필터의 access 검사와 대칭)
            if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(jwtUtil.getTokenType(claims))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("REFRESH_INVALID", "refresh 토큰이 아닙니다."));
            }

            String username = claims.getSubject();
            // 권한은 제시된 refresh 토큰의 클레임에서 승계(새 access 에 물려줌)
            List<String> roles = jwtUtil.getRoles(claims);

            // 3) 정책에 따라 발급
            if (refreshRotation) {
                // [회전] access+refresh 모두 새로 발급. TokenService.issue 가 사용자당 1개
                // upsert 로 DB 를 교체하므로, 방금 사용된 구 refresh 는 이 시점에 무효가 된다(1회용).
                return ResponseEntity.ok(ApiResponse.ok(tokenService.issue(username, roles)));
            }
            // [고정] 새 access 만 발급, refresh 는 만료 전까지 재사용
            String newAccessToken = jwtUtil.generateAccessToken(username, roles);
            return ResponseEntity.ok(
                    ApiResponse.ok(new AuthenticationResponse(newAccessToken, refreshTokenValue, "success")));
        } catch (ExpiredJwtException e) {
            // 만료된 refresh 는 더 못 쓰므로 DB 에서도 정리 → 클라이언트는 재로그인
            refreshTokenRepository.delete(stored.get());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("REFRESH_EXPIRED", "refresh 토큰이 만료되었습니다. 다시 로그인해 주세요."));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("REFRESH_INVALID", "유효하지 않은 refresh 토큰입니다. 다시 로그인해 주세요."));
        }
    }

    /**
     * JWT 로그아웃 = refresh 토큰 폐기(DB 삭제).
     * 이후 그 refresh 로는 갱신 불가. access 는 무상태라 서버가 즉시 무효화할 수 없고
     * 남은 수명(짧게 설정) 동안만 유효하다 — 이것이 "짧은 access + 긴 refresh" 구조의 이유.
     * (세션 로그아웃은 별도: POST /auth/logout — SessionSecurityConfig)
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody Map<String, String> request) {
        String refreshTokenValue = request.get("refreshToken");
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshTokenRepository::delete);
        // 존재하지 않는 토큰이어도 성공 응답(이미 폐기된 상태와 동일한 결과이므로)
        return ApiResponse.ok(null, "로그아웃(토큰 폐기) 되었습니다.");
    }
}
