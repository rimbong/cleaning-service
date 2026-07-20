package com.boot.cleanhub.biz.contract.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   청소 주기 — 정기 청소가 얼마나 자주 실행되는지.
 *   수정환경.xls 의 "격주","첫주" 같은 표기를 구조화한다.
 *
 *   monthlyMultiplier 는 청소 요일 하나가 한 달에 몇 번이 되는지다.
 *   요일은 "무슨 요일에 가는가", 주기는 "그 요일을 얼마나 자주 반복하는가" 이므로
 *   월 방문 횟수 = 요일 개수 x 이 배수가 된다(매주 월·목이면 2 x 4 = 8회).
 *   한 달을 4주로 본다(원 단가표가 주1회=월4회로 잡은 것과 맞춘다).
 *
 *   배수를 여기 둔 이유: 예전에는 이 규칙이 화면과 서버 두 곳에 흩어져 있었고,
 *   격주와 매월이 요일 개수를 무시하는 버그가 각각 따로 생겼다. 주기를 추가할 때
 *   배수를 같이 적게 만들어 빠뜨릴 수 없게 한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.1
 */
@Getter
@RequiredArgsConstructor
public enum CleaningCycle {

    WEEKLY("매주", 4),
    BIWEEKLY("격주", 2),
    /**
     * 매월은 "매월 첫째주 수요일, 넷째주 금요일"처럼 요일로 적을 수 없다.
     * 배수를 null 로 두어 "요일로 계산할 수 없는 주기"임을 타입으로 표현한다.
     * 이런 주기는 월 방문 횟수를 직접 입력받는다.
     */
    MONTHLY("매월", null);

    private final String label;

    /**
     * 청소 요일 하나가 한 달에 몇 번이 되는지.
     * null 이면 요일로 계산할 수 없는 주기다(직접 입력 대상).
     */
    private final Integer monthlyMultiplier;

    /** 요일과 주기로 월 방문 횟수를 계산할 수 있는지 */
    public boolean isDerivable() {
        return monthlyMultiplier != null;
    }
}
