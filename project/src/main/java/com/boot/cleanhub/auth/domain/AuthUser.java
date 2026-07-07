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
 *   세션 로그인용 사용자 계정.
 *   정식 인증 모듈(com.boot.cleanhub.auth)의 세션 로그인 사용자 저장소.
 *
 *   ※ 비밀번호는 반드시 BCrypt 해시로 저장한다(평문 금지).
 *     시드는 AuthUserSeeder 가 기동 시 넣는다(bcrypt 해시는 생성할 때마다 달라
 *     data.sql 고정값으로 넣기 애매하기 때문).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.03
 * @version 1.0
 */
@Entity
@Table(name = "auth_user")
@Getter
@Setter
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt 해시 문자열(예: $2a$10$...) */
    @Column(nullable = false)
    private String password;

    /** 데모용 단일 역할(예: ROLE_USER) */
    private String role;
}
