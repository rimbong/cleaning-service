package com.boot.cleanhub.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.boot.cleanhub.util.jwt.JwtUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

/**
 * <pre>
 *   [인가 필터] 매 요청의 Authorization: Bearer {JWT} 를 검증한다.
 *
 *   ■ 동작 흐름 (JwtApiSecurityConfig 가 /api/** 체인에 삽입)
 *     1) 헤더에서 Bearer 토큰 추출
 *     2) 파싱 = 서명 검증 + 만료 확인 (JwtUtil.extractUsername 내부에서 수행, 실패 시 예외)
 *     3) 유효하면 SecurityContext 에 인증 등록 → 이후 authorizeHttpRequests 의
 *        authenticated() 검사를 통과한다
 *     4) 무효/없음이면 등록하지 않고 통과 → 보호 경로라면 EntryPoint 가 401 응답
 *
 *   ■ 왜 DB 조회 없이 "클레임 기반"으로 검증하나 (2026.07 변경)
 *     JWT 는 서버 서명이 들어간 "자체 완결 신분증"이다. 서명이 유효하면 내용(sub=username)을
 *     신뢰할 수 있으므로 매 요청 DB 를 볼 필요가 없다(무상태 — JWT 를 쓰는 이유 그 자체).
 *     또한 과거 방식(jwt_user 테이블 조회)은 "세션 사용자에게 발급한 토큰"처럼
 *     다른 저장소 사용자의 토큰을 검증하지 못하는 문제가 있었다.
 *     · 트레이드오프: DB 조회 방식은 탈퇴/차단된 사용자의 토큰을 즉시 거부할 수 있지만
 *       매 요청 DB 부하가 있다. 즉시 차단이 필요하면 토큰 블랙리스트나
 *       "짧은 access 만료 + refresh 시 DB 확인"(현재 구조)으로 보완한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.18
 * @version 2.1 (2026.07.03 — 클레임 기반 검증 + access 타입만 인정 + roles 권한 복원)
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String jwt = authorizationHeader.substring(BEARER_PREFIX.length());
            try {
                // 파싱 자체가 서명 검증 + 만료 확인을 포함한다(실패 시 예외 → catch 로)
                Claims claims = jwtUtil.parseClaims(jwt);

                // ★ access 토큰만 인정 — refresh(수명 7일)를 Bearer 로 오용해
                //   API 존을 여는 것을 차단한다(token_type 클레임 검사).
                if (!JwtUtil.TOKEN_TYPE_ACCESS.equals(jwtUtil.getTokenType(claims))) {
                    logger.warn("access 타입이 아닌 토큰의 API 접근 시도 차단 (token_type="
                            + jwtUtil.getTokenType(claims) + ")");
                } else {
                    // roles 클레임 복원 → hasRole/hasAuthority 인가에 사용 가능
                    List<String> roles = jwtUtil.getRoles(claims);
                    List<GrantedAuthority> authorities = roles == null
                            ? Collections.emptyList()
                            : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (ExpiredJwtException e) {
                // 만료는 정상 수명 주기의 일부(클라이언트가 /api/auth/refresh 로 갱신) → 조용히 로그만
                logger.debug("JWT expired: " + e.getMessage());
            } catch (JwtException | IllegalArgumentException e) {
                // 위조/형식 오류 — 인증 미등록으로 통과시키면 보호 경로에서 401 처리됨
                logger.warn("JWT invalid: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
