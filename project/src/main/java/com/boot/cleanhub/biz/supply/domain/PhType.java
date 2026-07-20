package com.boot.cleanhub.biz.supply.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   청소 약품의 pH 구분.
 *
 *   세제 선택의 원칙은 "오염의 성질과 반대되는 pH를 쓴다" 이다.
 *   물때(알칼리성 미네랄)는 산성으로, 기름때(산성·유분)는 알칼리성으로 지운다.
 *
 *   mixWarning 은 이 성질의 약품을 다룰 때 반드시 지켜야 할 혼합 금지 사항이다.
 *   섞으면 유독가스가 나오는 조합이 있어 단순 참고가 아니라 안전 정보다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum PhType {

    ACID("산성", "pH 0~6",
            "물때·석회, 요석·소변석, 녹, 백화·워터스팟",
            "락스(염소계)와 절대 섞지 말 것 — 염소가스 발생. 대리석·천연석·금속에는 사용 금지."),

    NEUTRAL("중성", "pH 6~8",
            "먼지, 가벼운 기름·손때, 일상 얼룩",
            "재질 손상 위험이 가장 낮다. 어떤 세제를 쓸지 모를 때 1순위."),

    ALKALI("알칼리성", "pH 8~14",
            "기름때·찌든때, 음식물·유지, 그을음",
            "강알칼리는 화상 위험 — 장갑·보안경 필수. 암모니아 제품은 락스와 섞지 말 것(클로라민 가스). 알루미늄 부식 주의."),

    OXIDIZER("표백·산화계", "종류마다 다름",
            "곰팡이, 얼룩 색소, 살균·소독",
            "락스는 산성세제·식초·암모니아·알코올과 절대 섞지 말 것. 항상 단독으로, 찬물에, 환기하며 사용."),

    ENZYME("효소계", "약알칼리~중성",
            "혈액·음식물 등 단백질, 배수구 유기물",
            "미지근한 물(30~40도)에서 시간을 두고 반응시켜야 효과. 뜨거운 물이나 강한 산/알칼리와 함께 쓰면 효소가 파괴된다."),

    ETC("기타", "",
            "",
            "");

    private final String label;
    /** pH 범위 표기 */
    private final String phRange;
    /** 이 성질이 잘 지우는 오염 */
    private final String targetSoil;
    /** 취급 시 주의·혼합 금지 사항 */
    private final String mixWarning;
}
