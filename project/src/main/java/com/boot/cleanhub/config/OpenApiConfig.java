package com.boot.cleanhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * <pre>
 *   OpenAPI(Swagger UI) 설정.
 *   프론트(React/Vue) 팀에 제공할 API 스펙 문서를 자동 생성한다.
 *
 *   - Swagger UI : http://localhost:70/swagger-ui.html
 *   - OpenAPI JSON: http://localhost:70/v3/api-docs
 *
 *   JWT 보호 API(/api/**) 테스트를 위해 상단 [Authorize] 에 Bearer 토큰을 넣을 수 있도록 보안 스키마를 등록한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.01
 * @version 1.0
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI apiDocs() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot Head API")
                        .description("레거시 이관 + SPA 준비 백엔드 프레임워크 API 문서")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
