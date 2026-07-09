package com.boot.cleanhub.biz.quote.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.boot.cleanhub.biz.quote.domain.Quote;
import com.boot.cleanhub.biz.quote.domain.QuoteStatus;

import lombok.Getter;

/**
 * <pre>
 *   견적 응답 DTO — 엔티티를 그대로 노출하지 않고 화면에 필요한 형태로 변환해 내려준다.
 *   거래처는 연결됐을 때만 id/건물명을 담고, status 는 코드+라벨을 함께 준다.
 *
 *   ※ client 연관은 LAZY 이므로, 변환 전에 로딩돼 있어야 한다(Repository 의 left join fetch 로 함께 조회).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Getter
public class QuoteResponse {

    private final Long id;
    private final Long clientId;
    private final String clientName;
    private final String customerName;
    private final String customerPhone;
    private final String address;
    private final String title;
    private final Long amount;
    private final LocalDate quoteDate;
    private final LocalDate validUntil;
    private final QuoteStatus status;
    private final String statusLabel;
    private final String memo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private QuoteResponse(Quote q) {
        this.id = q.getId();
        this.clientId = q.getClient() != null ? q.getClient().getId() : null;
        this.clientName = q.getClient() != null ? q.getClient().getName() : null;
        this.customerName = q.getCustomerName();
        this.customerPhone = q.getCustomerPhone();
        this.address = q.getAddress();
        this.title = q.getTitle();
        this.amount = q.getAmount();
        this.quoteDate = q.getQuoteDate();
        this.validUntil = q.getValidUntil();
        this.status = q.getStatus();
        this.statusLabel = q.getStatus() != null ? q.getStatus().getLabel() : null;
        this.memo = q.getMemo();
        this.createdAt = q.getCreatedAt();
        this.updatedAt = q.getUpdatedAt();
    }

    /** 엔티티 → 응답 DTO 변환 */
    public static QuoteResponse from(Quote quote) {
        return new QuoteResponse(quote);
    }
}
