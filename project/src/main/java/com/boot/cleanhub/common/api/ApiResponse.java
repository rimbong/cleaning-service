package com.boot.cleanhub.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * <pre>
 *   모든 API 응답의 표준 envelope.
 *   SPA(React/Vue) 프론트가 성공/실패·코드·데이터·메시지를 일관되게 소비할 수 있게 한다.
 *
 *   성공: { "success": true,  "code": "SUCCESS", "data": {...} }
 *   실패: { "success": false, "code": "VALIDATION_ERROR", "message": "...", "data": {필드에러 등} }
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.01
 * @version 1.0
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "SUCCESS", null, data);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, "SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}
