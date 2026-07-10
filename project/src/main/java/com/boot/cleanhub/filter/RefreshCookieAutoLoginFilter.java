package com.boot.cleanhub.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.boot.cleanhub.auth.repository.RefreshTokenRepository;
import com.boot.cleanhub.auth.support.RefreshTokenCookie;
import com.boot.cleanhub.util.jwt.JwtUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   [세션 자동로그인 필터] — refresh 쿠키로 "페이지" 요청에서 세션을 재생성한다(로그인 유지).
 *
 *   ■ 왜 필요한가 (세션 체인 전용)
 *     관리자 "페이지"(/admin)는 세션(ROLE_ADMIN)으로 보호된다. 서버·브라우저 재시작으로 세션이
 *     사라지면, 새로고침 시 브라우저가 /admin 을 서버에 다시 요청하는데 이 시점엔 SPA(JS)가 아직
 *     안 떠서 /api/auth/refresh 를 호출할 수 없다(닭-달걀). 그래서 세션 게이트에 막혀 로그인으로 튕긴다.
 *     → 이 필터가 인가 검사 "전"에 refresh 쿠키(HttpOnly)를 검증해, 유효하면 세션을 만들어 준다.
 *       "브라우저 켤 때 토큰 확인 → 유효하면 로그인 + 세션 생성"의 서버측 구현.
 *
 *   ■ 안전장치
 *     - 이미 인증돼 있으면(세션 존재) 아무것도 안 함 → 세션당 DB 조회 1회로 끝난다.
 *     - refresh 타입 토큰만, DB(jwt_refresh_token) 대조까지 통과해야 로그인(폐기 토큰 차단).
 *     - 이 필터는 세션 체인(SessionSecurityConfig)에만 등록 → /api/** (JWT 무상태 체인)에는 영향 없음.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.10
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class RefreshCookieAutoLoginFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // 이미 인증됨(세션 있음)이면 자동로그인 불필요
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String refreshToken = readRefreshCookie(request);
            if (StringUtils.hasText(refreshToken)) {
                autoLogin(request, refreshToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    /** refresh 쿠키가 유효하면(서명·만료·타입·DB 대조) 세션 인증을 세운다. 실패하면 조용히 미인증 진행. */
    private void autoLogin(HttpServletRequest request, String refreshToken) {
        // DB 대조 — 로그아웃/재로그인/회전으로 폐기된 토큰이면 자동로그인 안 함
        if (!refreshTokenRepository.findByToken(refreshToken).isPresent()) {
            return;
        }
        try {
            Claims claims = jwtUtil.parseClaims(refreshToken); // 서명 + 만료 검증
            if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(jwtUtil.getTokenType(claims))) {
                return; // access 토큰을 자동로그인에 오용하는 것 차단
            }
            String username = claims.getSubject();
            List<String> roles = jwtUtil.getRoles(claims);
            List<GrantedAuthority> authorities = roles == null
                    ? Collections.emptyList()
                    : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            // 세션에 직접 저장 → 이후 요청도 세션으로 인증 유지(관리자 페이지 앵커 복원)
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        } catch (JwtException | IllegalArgumentException e) {
            // 무효/만료(ExpiredJwtException 포함) refresh → 자동로그인 안 함(게이트가 로그인으로 유도)
            logger.debug("자동로그인 실패(무효/만료 refresh): " + e.getMessage());
        }
    }

    /** 요청 쿠키에서 refresh 토큰 값 추출(없으면 null). */
    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (RefreshTokenCookie.NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
