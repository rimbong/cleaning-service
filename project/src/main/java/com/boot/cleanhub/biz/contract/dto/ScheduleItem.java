package com.boot.cleanhub.biz.contract.dto;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.contract.domain.Contract;

import lombok.Getter;

/**
 * <pre>
 *   요일별 청소 스케줄의 한 항목 — 그 요일에 청소할 거래처(계약) 하나.
 *   현장 방문에 필요한 정보(주소·현관 비번·주기·메모)를 담는다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class ScheduleItem {

    private final Long contractId;
    private final Long clientId;
    private final String clientName;      // 거래처(건물명)
    private final String title;           // 계약명
    private final String address;         // 주소
    private final String phone;           // 연락처
    private final String doorCode;        // 현관 비번(현장 필요)
    private final String cleaningCycleLabel; // 주기(매주/격주/매월)
    private final String memo;

    private ScheduleItem(Contract c) {
        Client cl = c.getClient();
        this.contractId = c.getId();
        this.clientId = cl != null ? cl.getId() : null;
        this.clientName = cl != null ? cl.getName() : null;
        this.title = c.getTitle();
        this.address = cl != null ? cl.getAddress() : null;
        this.phone = cl != null ? cl.getManagerPhone() : null;
        this.doorCode = c.getDoorCode();
        this.cleaningCycleLabel = c.getCleaningCycle() != null ? c.getCleaningCycle().getLabel() : null;
        this.memo = c.getMemo();
    }

    public static ScheduleItem of(Contract c) {
        return new ScheduleItem(c);
    }
}
