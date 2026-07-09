package com.boot.cleanhub.biz.settlement.dto;

import java.util.List;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.contract.domain.Contract;

import lombok.Getter;

/**
 * <pre>
 *   연간 수금 현황 한 행 — 거래처(계약) 하나 + 1~12월 수금일.
 *   수정환경.xls 의 "거래처 현황(청소)" 장부 한 줄에 대응한다.
 *   월 칸(months)은 그 달 최종 수금일을 "M/D" 문자열로, 없으면 빈 문자열.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class YearlyCollectionRow {

    private final Long contractId;
    private final String clientName;     // 거래처(건물명)
    private final String managerName;    // 담당자
    private final String title;          // 계약명
    private final String address;        // 주소
    private final String phone;          // 전화번호
    private final String paymentMethod;  // 수금(방법)
    private final String doorCode;       // 비밀(현관 비번)
    private final Long monthlyFee;       // 금액(월정액)
    private final Integer billingDay;    // 결재(청구일)
    private final String status;         // 계약 상태
    private final List<String> months;   // 1~12월 수금일("M/D" 또는 "")

    private YearlyCollectionRow(Contract c, List<String> months) {
        Client cl = c.getClient();
        this.contractId = c.getId();
        this.clientName = cl != null ? cl.getName() : null;
        this.managerName = cl != null ? cl.getManagerName() : null;
        this.title = c.getTitle();
        this.address = cl != null ? cl.getAddress() : null;
        this.phone = cl != null ? cl.getManagerPhone() : null;
        this.paymentMethod = c.getPaymentMethod();
        this.doorCode = c.getDoorCode();
        this.monthlyFee = c.getMonthlyFee();
        this.billingDay = c.getBillingDay();
        this.status = c.getStatus() != null ? c.getStatus().name() : null;
        this.months = months;
    }

    public static YearlyCollectionRow of(Contract c, List<String> months) {
        return new YearlyCollectionRow(c, months);
    }
}
