package com.boot.cleanhub.biz.pricing.domain;

/**
 * <pre>
 *   월 방문 횟수를 사람이 읽는 말로 바꾸는 유틸.
 *
 *   산정은 방문 횟수(숫자)로만 한다. 예전에는 월1회~주3회 6단계 enum 으로 계산했는데,
 *   표에 없는 횟수(주 4회 등)가 조용히 낮은 금액으로 깎여 나가는 문제가 있었다.
 *   지금은 숫자를 그대로 쓰고, 표시할 때만 여기서 익숙한 표현으로 바꾼다.
 *
 *   주 단위 환산은 <b>한 달 4주</b> 기준이다(원 단가표가 주1회=월4회로 잡고 있다).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
public final class VisitFrequency {

    /** 한 달을 몇 주로 보는지 — 원 단가표가 주1회=월4회로 잡은 것과 맞춘다. */
    public static final int WEEKS_PER_MONTH = 4;

    private VisitFrequency() {
    }

    /**
     * 월 방문 횟수를 표시용 문구로 바꾼다.
     * 예) 1 -> "월 1회", 2 -> "월 2회(격주)", 4 -> "주 1회 (월 4회)", 16 -> "주 4회 (월 16회)"
     *
     * @param visitsPerMonth 월 방문 횟수
     * @return 표시 문구
     */
    public static String label(int visitsPerMonth) {
        if (visitsPerMonth == 2) {
            return "월 2회(격주)";
        }
        if (visitsPerMonth >= WEEKS_PER_MONTH && visitsPerMonth % WEEKS_PER_MONTH == 0) {
            return "주 " + (visitsPerMonth / WEEKS_PER_MONTH) + "회 (월 " + visitsPerMonth + "회)";
        }
        return "월 " + visitsPerMonth + "회";
    }
}
