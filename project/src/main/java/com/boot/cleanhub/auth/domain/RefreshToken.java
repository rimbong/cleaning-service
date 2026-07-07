package com.boot.cleanhub.auth.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   Refresh 토큰 저장 엔티티.
 *
 *   access 는 무상태(서명 검증만)지만, refresh 는 수명이 길어(기본 7일) 탈취 피해가 크므로
 *   DB 에 저장해 두고 갱신 요청 때마다 대조한다 — 로그아웃/재로그인/회전(rotation) 시
 *   행을 삭제·교체하는 것으로 즉시 무효화가 가능해진다.
 *
 *   사용자당 1행 정책(username unique): 재발급 시 upsert 로 덮어써서
 *   이전 refresh 는 자동으로 무효가 된다 — TokenService.issue() 참고.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.18
 * @version 2.0 (2026.07.06 — 정식 모듈(auth.domain) 승격, import 정리)
 */
@Entity
@Table(name = "jwt_refresh_token")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, length = 512)
    private String token;
}
