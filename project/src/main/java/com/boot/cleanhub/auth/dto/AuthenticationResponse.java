package com.boot.cleanhub.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * JWT 인증 응답을 위한 DTO 입니다.
 */
@Data
@AllArgsConstructor
public class AuthenticationResponse {
    private final String accessToken;
    private final String refreshToken;
    private final String message;
}
