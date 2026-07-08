package com.boot.cleanhub.error;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.ConstraintViolationException;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.boot.cleanhub.common.api.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   전역 예외 → 표준 ApiResponse(JSON) 변환기.
 *   에러 메시지는 MessageSource 로 현재 로케일(세션 ?lang= 또는 Accept-Language)에 맞춰 조회한다.
 *   40x/50x 서블릿 에러는 CustomErrorController 가, 커스텀/검증 예외는 여기서 처리한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.05
 * @version 3.0
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /** 비즈니스 예외 → ErrorCode 의 HTTP 상태 + 로케일 메시지. */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BizException e) {
        ErrorCode ec = e.getErrorCode();
        String message = resolve(ec, e.getArgs());
        return ResponseEntity.status(ec.getStatus()).body(ApiResponse.error(ec.getCode(), message));
    }

    /** @Valid @RequestBody 검증 실패 → 400 + 필드별 메시지. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ErrorCode ec = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.error(ec.getCode(), resolve(ec, null), fieldErrors));
    }

    /** @Validated 파라미터(경로/쿼리) 제약 위반 → 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException e) {
        ErrorCode ec = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(ec.getStatus()).body(ApiResponse.error(ec.getCode(), e.getMessage()));
    }

    /** 업로드 파일이 multipart 상한(application.yml)을 초과 → 413. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        ErrorCode ec = ErrorCode.FILE_TOO_LARGE;
        return ResponseEntity.status(ec.getStatus()).body(ApiResponse.error(ec.getCode(), resolve(ec, null)));
    }

    // 그 외 일반 예외(및 404/500 서블릿 에러)는 CustomErrorController 가 처리한다.
    // (API 요청이면 JSON ApiResponse, 페이지 요청이면 HTML 에러 페이지)

    /**
     * ErrorCode → 현재 로케일 메시지.
     * properties(messages_{locale}) 가 단일 소스. 키가 없으면 enum defaultMessage(있을 때),
     * 그것도 없으면 코드 문자열로 안전하게 대체(메시지가 null 이 되지 않도록).
     */
    private String resolve(ErrorCode ec, Object[] args) {
        String fallback = ec.getDefaultMessage() != null ? ec.getDefaultMessage() : ec.getCode();
        return messageSource.getMessage(ec.getMessageKey(), args, fallback, LocaleContextHolder.getLocale());
    }
}
