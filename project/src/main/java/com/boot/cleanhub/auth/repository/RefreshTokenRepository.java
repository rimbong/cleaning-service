package com.boot.cleanhub.auth.repository;

import com.boot.cleanhub.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUsername(String username);
    Optional<RefreshToken> findByToken(String token);
}
