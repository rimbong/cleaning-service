package com.boot.cleanhub.auth.service;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.auth.domain.AuthUser;
import com.boot.cleanhub.auth.repository.AuthUserRepository;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   세션 로그인용 UserDetailsService.
 *
 *   [스프링 시큐리티에서 이 클래스의 위치]
 *   폼 로그인 POST /auth/login
 *     → UsernamePasswordAuthenticationFilter 가 요청을 가로챔
 *     → AuthenticationManager → DaoAuthenticationProvider
 *     → (바로 여기) loadUserByUsername(username) 으로 사용자 조회
 *     → PasswordEncoder(BCrypt).matches(입력비번, DB해시) 로 비밀번호 대조
 *     → 성공 시 SecurityContext + 세션(JSESSIONID)에 인증 저장
 *
 *   즉 개발자는 "사용자를 어떻게 찾는지"만 구현하고,
 *   비밀번호 대조·세션 저장은 스프링 시큐리티가 담당한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.03
 * @version 1.0
 */
@Service("authUserDetailsService")
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser user = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        String role = user.getRole() != null ? user.getRole() : "ROLE_USER";

        // 스프링 시큐리티 표준 User 객체로 변환(우리 엔티티를 시큐리티가 이해하는 형태로)
        return new User(
                user.getUsername(),
                user.getPassword(), // BCrypt 해시 — 대조는 DaoAuthenticationProvider 가 수행
                Collections.singletonList(new SimpleGrantedAuthority(role)));
    }
}
