package com.boot.cleanhub.error;

import org.springframework.http.HttpStatus;

/**
 * <pre>
 *   에러 코드 카탈로그(코드 + HTTP 상태 레지스트리).
 *   레거시(HIS) com.common.enumtype.EErrorCodeType 을 이관·현대화한 것.
 *
 *   설계 원칙:
 *   - enum 은 "코드"와 "HTTP 상태"만 관리한다.
 *   - 사용자 노출 "메시지"는 i18n properties(messages_ko/en.properties)의 "error.{코드}" 키가 단일 소스.
 *     → 언어별 메시지가 한 곳(properties)에 모여 일관성이 좋고, 번역 추가가 쉽다.
 *   - defaultMessage 는 "정말 필요한 경우"에만 선언하는 최후 안전망(properties 키 누락 시 fallback).
 *     대부분의 코드는 default 없이 선언하고, 메시지는 properties 에서 관리한다.
 *
 *   메시지 키 규칙: error.{code}   (예: error.000101)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.01
 * @version 2.0
 */
public enum ErrorCode {

    // ===== 유저 / 로그인 (00-00) =====
    MEMBER_NOT_FOUND         ("000001", HttpStatus.NOT_FOUND),
    MEMBER_INACTIVE          ("000002", HttpStatus.FORBIDDEN),
    ACCESS_LOG_UPDATE_FAILED ("000003", HttpStatus.INTERNAL_SERVER_ERROR),
    ABNORMAL_LOGIN_ACCESS    ("000004", HttpStatus.BAD_REQUEST),
    LOGIN_EXPIRED            ("000005", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_ISSUE_FAILED("000006", HttpStatus.INTERNAL_SERVER_ERROR),
    REFRESH_TOKEN_NOT_FOUND  ("000007", HttpStatus.UNAUTHORIZED),
    USER_INFO_FETCH_FAILED   ("000008", HttpStatus.INTERNAL_SERVER_ERROR),
    SESSION_NOT_FOUND        ("000009", HttpStatus.UNAUTHORIZED),
    TOKEN_RESET_FAILED       ("000010", HttpStatus.INTERNAL_SERVER_ERROR),
    KEY_NOT_FOUND            ("000011", HttpStatus.BAD_REQUEST),

    // ===== 유저 / 회원가입 (00-01) =====
    DUPLICATE_MEMBER_ID      ("000101", HttpStatus.CONFLICT),
    SIGNUP_FAILED            ("000102", HttpStatus.INTERNAL_SERVER_ERROR),
    ABNORMAL_SIGNUP_ACCESS   ("000103", HttpStatus.BAD_REQUEST),

    // ===== 유저 / 유저정보 (00-02) =====
    INVALID_USER_INFO        ("000201", HttpStatus.BAD_REQUEST),
    TEMP_PASSWORD_ISSUE_FAILED("000202", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_RECEIVER_EMAIL   ("000203", HttpStatus.BAD_REQUEST),

    // ===== 유저 / 알람 (00-03) =====
    NOTIFICATION_SEND_FAILED ("000301", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== 파일 (01-00) =====
    INVALID_FILE_FORMAT      ("010001", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED       ("010002", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE           ("010003", HttpStatus.PAYLOAD_TOO_LARGE),

    // ===== 거래처 (10-00) =====
    CLIENT_NOT_FOUND         ("100001", HttpStatus.NOT_FOUND),
    CLIENT_HAS_CONTRACTS     ("100002", HttpStatus.CONFLICT),

    // ===== 계약 (10-01) =====
    CONTRACT_NOT_FOUND       ("100101", HttpStatus.NOT_FOUND),
    CONTRACT_ATTACHMENT_NOT_FOUND("100102", HttpStatus.NOT_FOUND),

    // ===== 견적 (10-02) =====
    QUOTE_NOT_FOUND          ("100201", HttpStatus.NOT_FOUND),

    // ===== 공통 / 프레임워크 (99) =====
    VALIDATION_ERROR         ("999001", HttpStatus.BAD_REQUEST),
    // 최후 안전망: properties 조회 실패에 대비해 default 를 명시적으로 선언한 예외 케이스.
    INTERNAL_ERROR           ("999999", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;   // nullable — 정말 필요한 경우에만 지정

    ErrorCode(String code, HttpStatus status) {
        this(code, status, null);
    }

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** properties 에 키가 없을 때의 fallback. 대부분 null(= properties 가 단일 소스). */
    public String getDefaultMessage() {
        return defaultMessage;
    }

    /** MessageSource 조회용 키 (error.{code}). */
    public String getMessageKey() {
        return "error." + code;
    }

    /** 코드 문자열로 enum 조회(없으면 null). */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode ec : values()) {
            if (ec.code.equals(code)) {
                return ec;
            }
        }
        return null;
    }
}
