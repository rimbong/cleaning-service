package com.boot.cleanhub.error;

/**
 * <pre>
 *   비즈니스 예외.
 *   레거시(HIS)의 체크예외(extends Exception) 방식을 RuntimeException 으로 현대화하여
 *   호출부에 throws 강제가 없도록 한다. 에러 코드는 ErrorCode enum 으로 타입 안전하게 다룬다.
 *
 *   사용 예:
 *     throw new BizException(ErrorCode.DUPLICATE_MEMBER_ID);
 *     throw new BizException(ErrorCode.INVALID_RECEIVER_EMAIL, email);   // 메시지 {0} 치환 인자
 *
 *   실제 사용자 노출 메시지는 GlobalExceptionHandler 가 MessageSource(로케일)로 조회한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08.05
 * @version 2.0
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /** 메시지의 {0},{1}... 치환 인자(선택). */
    private final transient Object[] args;

    public BizException(ErrorCode errorCode) {
        this(errorCode, new Object[0]);
    }

    public BizException(ErrorCode errorCode, Object... args) {
        // 로그/스택트레이스용 메시지(사용자 노출 메시지는 GlobalExceptionHandler 가 로케일로 채움)
        super(errorCode.getDefaultMessage() != null ? errorCode.getDefaultMessage() : errorCode.getCode());
        this.errorCode = errorCode;
        this.args = args;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }

    /** 편의 메서드: 에러 코드 문자열. */
    public String getCode() {
        return errorCode.getCode();
    }
}
