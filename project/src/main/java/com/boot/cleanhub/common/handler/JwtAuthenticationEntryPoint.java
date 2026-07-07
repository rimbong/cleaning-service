package com.boot.cleanhub.common.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * <pre>
 *   CustomAuthenticationEntryPoint
 *  인가 과정에서 Exception 발생시 아래 코드가 실행 된다. 
 * </pre>
 * @author In-seong Hwang
 * @since 2025.08.18
 * @version 1.0
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException,
            ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Authentication is required to access this resource.\"}");
        response.getWriter().flush();
    }
}