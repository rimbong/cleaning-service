package com.boot.cleanhub.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.auth.dto.AuthenticationResponse;
import com.boot.cleanhub.auth.domain.RefreshToken;
import com.boot.cleanhub.auth.repository.RefreshTokenRepository;
import com.boot.cleanhub.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   JWT 발급 창구(단일 진입점).
 *
 *   Access/Refresh 토큰을 생성하고, Refresh 토큰을 DB(jwt_refresh_token)에
 *   저장(사용자당 1개 upsert)한다.
 *
 *   ⚠️ 왜 이 클래스로 통일했나:
 *   과거에는 로그인 경로마다 발급 코드가 흩어져 있어서, PKCE 경로는 refresh 토큰을
 *   DB에 저장하지 않았다 → /api/auth/refresh 가 DB 대조를 하므로 갱신이 항상 실패했다.
 *   어떤 경로(세션→발급, PKCE)든 반드시 이 issue() 를 거치면 그런 누락이 없다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.03
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * access/refresh 토큰 발급 + refresh 토큰 DB 저장.
     * 두 토큰 모두 token_type·roles 클레임을 담는다(JwtUtil 참고).
     *
     * @param username 토큰 주체(sub 클레임)
     * @param roles    권한 목록(예: ["ROLE_USER"]) — 필터가 인가에 사용. 없으면 빈 리스트/null
     * @return access/refresh 토큰 묶음
     */
    @Transactional
    public AuthenticationResponse issue(String username, List<String> roles) {
        String accessToken = jwtUtil.generateAccessToken(username, roles);
        String refreshTokenValue = jwtUtil.generateRefreshToken(username, roles);

        // 사용자당 refresh 토큰 1개 정책(재로그인 시 기존 토큰 덮어씀 → 이전 refresh 는 무효)
        RefreshToken refreshToken = refreshTokenRepository.findByUsername(username)
                .orElse(new RefreshToken());
        refreshToken.setUsername(username);
        refreshToken.setToken(refreshTokenValue);
        refreshTokenRepository.save(refreshToken);

        return new AuthenticationResponse(accessToken, refreshTokenValue, "success");
    }
}
