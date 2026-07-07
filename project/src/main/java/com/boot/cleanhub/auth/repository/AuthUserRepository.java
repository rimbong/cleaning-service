package com.boot.cleanhub.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.cleanhub.auth.domain.AuthUser;

/**
 * AuthUser JPA 저장소.
 */
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByUsername(String username);
}
