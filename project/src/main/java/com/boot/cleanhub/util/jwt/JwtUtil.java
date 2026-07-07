package com.boot.cleanhub.util.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 *   JWT(Json Web Token) 생성·파싱·검증을 담당하는 유틸리티.
 *
 *   ■ 토큰에 담는 클레임(내용)
 *     sub        : 사용자명(주체)
 *     iat / exp  : 발급/만료 시각 (※ 초 단위 — 같은 1초 안에 재발급하면 동일 문자열이 됨)
 *     token_type : "access" | "refresh" — 용도 구분.
 *                  이게 없으면 수명 7일짜리 refresh 를 Authorization Bearer 에 넣어
 *                  API 존을 여는 오용이 가능하다(2026.07 보안 보강).
 *     roles      : 권한 목록 — 필터가 DB 조회 없이 인가(hasRole 등)를 수행할 근거.
 *
 *   ■ 파싱 = 검증이다
 *     parseClaims() 가 내부에서 "서명 검증(위조 확인) + exp 확인(만료 확인)"을 함께 수행한다.
 *     클레임이 반환됐다는 것 자체가 두 검증을 통과했다는 뜻이고,
 *     실패 시 값이 아니라 예외를 던진다(만료: ExpiredJwtException / 위조 등: JwtException).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.18
 * @version 2.0 (2026.07.03 — token_type/roles 클레임 도입, 미사용 메서드 정리)
 */
@Component
public class JwtUtil {

    /** 토큰 용도 클레임 키 */
    public static final String CLAIM_TOKEN_TYPE = "token_type";
    /** 권한 목록 클레임 키 */
    public static final String CLAIM_ROLES = "roles";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private Key key;

    /**
     * 컴포넌트 초기화 시 Secret 문자열의 원문 바이트로 HMAC-SHA 서명 키를 생성합니다.
     * (HS256 은 키가 최소 256bit(=32바이트) 필요 — 시크릿 길이가 짧으면 여기서 예외 발생)
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Access Token 생성 (token_type=access + roles 클레임 포함).
     *
     * @param username 토큰 주체(sub)
     * @param roles    권한 목록(예: ["ROLE_USER"]) — 없으면 빈 리스트
     * @return 생성된 Access Token
     */
    public String generateAccessToken(String username, List<String> roles) {
        return createToken(buildClaims(TOKEN_TYPE_ACCESS, roles), username, accessTokenExpirationMs);
    }

    /**
     * Refresh Token 생성 (token_type=refresh + roles 클레임 포함).
     * roles 를 함께 담는 이유: 갱신 시 새 access 에 물려줄 권한의 출처가 이 토큰이기 때문.
     *
     * @param username 토큰 주체(sub)
     * @param roles    권한 목록
     * @return 생성된 Refresh Token
     */
    public String generateRefreshToken(String username, List<String> roles) {
        return createToken(buildClaims(TOKEN_TYPE_REFRESH, roles), username, refreshTokenExpirationMs);
    }

    /** 공통 클레임(token_type, roles) 구성 */
    private Map<String, Object> buildClaims(String tokenType, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, tokenType);
        if (roles != null && !roles.isEmpty()) {
            claims.put(CLAIM_ROLES, roles);
        }
        return claims;
    }

    /**
     * Claims와 사용자 이름을 기반으로 JWT 토큰을 생성합니다.
     *
     * @param claims       토큰에 담을 정보
     * @param subject      토큰의 주체 (사용자 이름)
     * @param expirationMs 토큰의 만료 시간
     * @return 생성된 JWT 토큰
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰을 파싱해 클레임을 반환합니다. — ★ 이 호출이 곧 "검증"이다
     * 서명 검증(위조 확인)과 만료 확인을 함께 수행하며, 실패 시 예외를 던진다.
     *
     * @param token 검증·파싱할 토큰
     * @return 클레임(sub, token_type, roles 등)
     * @throws io.jsonwebtoken.ExpiredJwtException 만료된 토큰
     * @throws io.jsonwebtoken.JwtException        위조·형식 오류 등
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /**
     * 토큰에서 사용자 이름을 추출합니다(파싱 = 서명·만료 검증 포함).
     *
     * @param token 추출할 토큰
     * @return 사용자 이름
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 클레임에서 토큰 용도(access/refresh)를 꺼냅니다.
     * 구버전 토큰(token_type 없음)은 null 이 반환된다 — 호출부에서 불일치로 처리.
     */
    public String getTokenType(Claims claims) {
        return claims.get(CLAIM_TOKEN_TYPE, String.class);
    }

    /**
     * 클레임에서 권한 목록을 꺼냅니다(없으면 null).
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        return roles instanceof List ? (List<String>) roles : null;
    }
}
