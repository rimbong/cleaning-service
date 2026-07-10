package com.boot.cleanhub.biz.contract.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.boot.cleanhub.biz.contract.domain.CleaningCycle;
import com.boot.cleanhub.biz.contract.domain.Contract;
import com.boot.cleanhub.biz.contract.domain.ContractStatus;
import com.boot.cleanhub.biz.contract.domain.VatType;

import lombok.Getter;

/**
 * <pre>
 *   계약 응답 DTO — 엔티티를 그대로 노출하지 않고 화면에 필요한 형태로 변환해 내려준다.
 *   거래처는 id 와 건물명(clientName)만 평면화해 담고, status 는 코드+라벨을 함께 준다.
 *
 *   ※ client 연관은 LAZY 이므로, 이 DTO 로 변환하기 전에 거래처가 로딩돼 있어야 한다
 *     (Repository 의 fetch join 쿼리로 함께 조회함).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Getter
public class ContractResponse {

    private final Long id;
    private final Long clientId;
    private final String clientName;
    private final String title;
    private final Long monthlyFee;
    private final Integer billingDay;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final ContractStatus status;
    private final String statusLabel;
    private final String memo;
    private final String documentLocation;
    private final String paymentMethod;
    private final String doorCode;
    private final List<String> cleaningWeekdays;
    private final CleaningCycle cleaningCycle;
    private final String cleaningCycleLabel;
    private final VatType vatType;
    private final String vatTypeLabel;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ContractResponse(Contract c) {
        this.id = c.getId();
        this.clientId = c.getClient() != null ? c.getClient().getId() : null;
        this.clientName = c.getClient() != null ? c.getClient().getName() : null;
        this.title = c.getTitle();
        this.monthlyFee = c.getMonthlyFee();
        this.billingDay = c.getBillingDay();
        this.startDate = c.getStartDate();
        this.endDate = c.getEndDate();
        this.status = c.getStatus();
        this.statusLabel = c.getStatus() != null ? c.getStatus().getLabel() : null;
        this.memo = c.getMemo();
        this.documentLocation = c.getDocumentLocation();
        this.paymentMethod = c.getPaymentMethod();
        this.doorCode = c.getDoorCode();
        this.cleaningWeekdays = splitWeekdays(c.getCleaningWeekdays());
        this.cleaningCycle = c.getCleaningCycle();
        this.cleaningCycleLabel = c.getCleaningCycle() != null ? c.getCleaningCycle().getLabel() : null;
        this.vatType = c.getVatType();
        this.vatTypeLabel = c.getVatType() != null ? c.getVatType().getLabel() : null;
        this.createdAt = c.getCreatedAt();
        this.updatedAt = c.getUpdatedAt();
    }

    /** 저장된 "MON,WED,FRI" 문자열을 요일 코드 리스트로. 비어 있으면 빈 리스트. */
    private static List<String> splitWeekdays(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(raw.split(","));
    }

    /** 엔티티 → 응답 DTO 변환 */
    public static ContractResponse from(Contract contract) {
        return new ContractResponse(contract);
    }
}
