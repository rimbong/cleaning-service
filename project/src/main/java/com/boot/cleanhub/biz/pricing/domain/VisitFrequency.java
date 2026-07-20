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
     * 월 방문 횟수를 표시용 문구로 바꾼다. 예) 3 -> "월 3회"
     *
     * 횟수만으로 "주 몇 회"인지 단정하지 않는다. 매월 첫째주·넷째주에 가는 계약도
     * 월 2회지만 격주가 아니다. 분포를 아는 경우에만 {@link #label(int, boolean)} 를 쓴다.
     *
     * @param visitsPerMonth 월 방문 횟수
     * @return 표시 문구
     */
    public static String label(int visitsPerMonth) {
        return "월 " + visitsPerMonth + "회";
    }

    /**
     * 방문이 매주 반복되는 계약이면 "주 N회 (월 M회)" 로, 아니면 "월 M회" 로 표시한다.
     *
     * @param visitsPerMonth   월 방문 횟수
     * @param weeklyRepeated   매주 반복되는 계약인지(계약의 청소 주기가 매주인지)
     * @return 표시 문구
     */
    public static String label(int visitsPerMonth, boolean weeklyRepeated) {
        if (weeklyRepeated && visitsPerMonth >= WEEKS_PER_MONTH && visitsPerMonth % WEEKS_PER_MONTH == 0) {
            return "주 " + (visitsPerMonth / WEEKS_PER_MONTH) + "회 (월 " + visitsPerMonth + "회)";
        }
        return label(visitsPerMonth);
    }
}
