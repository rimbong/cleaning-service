package com.boot.cleanhub.biz.supply.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   섞으면 유독가스가 나오는 약품 조합.
 *
 *   창고에 두 성질의 약품을 함께 보유하고 있으면 화면에서 경고한다.
 *   청소 사고는 대부분 "더 잘 지우려고" 섞다가 나기 때문이다.
 *
 *   판정을 서버에 두는 이유: 화면에서 하면 그때 보이는 목록(한 페이지)만 보게 되어
 *   락스와 산성 세제가 다른 페이지에 있으면 어느 페이지에서도 경고가 뜨지 않는다.
 *   안전 정보라 "경고 없음"이 "안전"으로 읽히므로 창고 전체를 기준으로 판정해야 한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum HazardousMix {

    CHLORINE_GAS(PhType.OXIDIZER, PhType.ACID, "염소가스",
            "염소계 표백제(락스)와 산성 세제·식초·구연산을 섞으면 염소가스가 나옵니다. 소량으로도 중독·질식 위험이 있습니다."),

    CHLORAMINE_GAS(PhType.OXIDIZER, PhType.ALKALI, "클로라민 가스",
            "락스와 암모니아가 든 세제를 섞으면 클로라민 가스가 나옵니다. 호흡곤란·폐 손상 위험이 있습니다.");

    private final PhType first;
    private final PhType second;
    /** 생성되는 유독 물질 */
    private final String gas;
    /** 왜 위험한지 */
    private final String detail;

    /**
     * 보유 중인 약품 성질만으로 이 조합이 성립하는지 판단한다.
     *
     * @param phTypesInStock 재고가 남아 있는 품목들의 pH 구분
     * @return 두 성질을 모두 보유하고 있으면 true
     */
    public boolean matches(java.util.Set<PhType> phTypesInStock) {
        return phTypesInStock.contains(first) && phTypesInStock.contains(second);
    }
}
