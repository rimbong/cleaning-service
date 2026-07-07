package com.boot.cleanhub.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   CORS 관련 커스텀 프로퍼티 바인딩.
 *   application.yml 의 cors.* 를 타입 안전하게 매핑한다.
 *   (@ConfigurationProperties 로 등록하면 IDE 가 프로퍼티를 인식해 "Unknown property" 경고도 사라진다)
 *
 *   cors:
 *     allowed-origins: http://localhost:5173,http://localhost:3000
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.01
 * @version 1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /** 허용할 오리진 목록(콤마 구분 문자열도 자동으로 List 로 바인딩됨). */
    private List<String> allowedOrigins = new ArrayList<>();
}
