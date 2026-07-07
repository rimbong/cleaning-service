package com.boot.cleanhub.common.aop;

/**
 * <pre>
 *   @TimeTrace 가로채기로 수집된 단일 트레이스 결과.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
public class TraceLog {

    private final String label;     // 트레이스 라벨(또는 메서드명)
    private final String method;    // 클래스.메서드
    private final String args;      // 파라미터 요약
    private final long elapsedMs;   // 실행 시간(ms)
    private final String status;    // OK / ERROR
    private final String error;     // 예외 메시지(있을 때)
    private final long timestamp;   // 종료 시각(epoch ms)

    public TraceLog(String label, String method, String args, long elapsedMs,
                    String status, String error, long timestamp) {
        this.label = label;
        this.method = method;
        this.args = args;
        this.elapsedMs = elapsedMs;
        this.status = status;
        this.error = error;
        this.timestamp = timestamp;
    }

    public String getLabel() { return label; }
    public String getMethod() { return method; }
    public String getArgs() { return args; }
    public long getElapsedMs() { return elapsedMs; }
    public String getStatus() { return status; }
    public String getError() { return error; }
    public long getTimestamp() { return timestamp; }
}
