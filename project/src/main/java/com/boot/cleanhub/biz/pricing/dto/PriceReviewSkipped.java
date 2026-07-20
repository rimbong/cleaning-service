package com.boot.cleanhub.biz.pricing.dto;

import lombok.Getter;

/**
 * <pre>
 *   적정가 재산정에서 빠진 계약 한 줄.
 *
 *   건수만 알려주면 "22건이 빠졌다"는 것만 알고 어디를 고쳐야 할지 찾을 수 없다.
 *   무엇이 왜 빠졌는지 같이 내려줘서 바로 눌러 고칠 수 있게 한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class PriceReviewSkipped {

    /** 빠진 이유 */
    public enum Reason {

        /** 거래처에 층수·세대수가 없어 금액을 계산할 수 없다 */
        NO_BUILDING("건물 규모 미입력", "거래처 수정에서 층수·세대수를 넣으세요."),
        /** 청소 주기가 없거나 매월인데 방문 횟수가 없다 */
        NO_VISITS("방문 횟수 미확인", "계약 수정에서 청소 주기(매월이면 월 방문 횟수)를 지정하세요.");

        private final String label;
        private final String howToFix;

        Reason(String label, String howToFix) {
            this.label = label;
            this.howToFix = howToFix;
        }

        public String getLabel() {
            return label;
        }

        public String getHowToFix() {
            return howToFix;
        }
    }

    private final Long contractId;
    private final Long clientId;
    private final String clientName;
    private final String contractTitle;
    private final Reason reason;
    private final String reasonLabel;
    private final String howToFix;

    public PriceReviewSkipped(Long contractId, Long clientId, String clientName,
            String contractTitle, Reason reason) {
        this.contractId = contractId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.contractTitle = contractTitle;
        this.reason = reason;
        this.reasonLabel = reason.getLabel();
        this.howToFix = reason.getHowToFix();
    }
}
