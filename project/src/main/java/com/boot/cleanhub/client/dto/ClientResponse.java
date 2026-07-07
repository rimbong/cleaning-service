package com.boot.cleanhub.client.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.boot.cleanhub.client.domain.CleaningType;
import com.boot.cleanhub.client.domain.Client;

import lombok.Getter;

/**
 * <pre>
 *   거래처 응답 DTO — 엔티티를 그대로 노출하지 않고 화면에 필요한 형태로 변환해 내려준다.
 *   cleaningType 은 코드(REGULAR)와 라벨(정기 청소)을 함께 담아 프론트가 바로 표시할 수 있게 한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.07
 * @version 1.0
 */
@Getter
public class ClientResponse {

    private final Long id;
    private final String name;
    private final String address;
    private final String managerName;
    private final String managerPhone;
    private final CleaningType cleaningType;
    private final String cleaningTypeLabel;
    private final LocalDate contractStartDate;
    private final String memo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ClientResponse(Client c) {
        this.id = c.getId();
        this.name = c.getName();
        this.address = c.getAddress();
        this.managerName = c.getManagerName();
        this.managerPhone = c.getManagerPhone();
        this.cleaningType = c.getCleaningType();
        this.cleaningTypeLabel = c.getCleaningType() != null ? c.getCleaningType().getLabel() : null;
        this.contractStartDate = c.getContractStartDate();
        this.memo = c.getMemo();
        this.createdAt = c.getCreatedAt();
        this.updatedAt = c.getUpdatedAt();
    }

    /** 엔티티 → 응답 DTO 변환 */
    public static ClientResponse from(Client client) {
        return new ClientResponse(client);
    }
}
