package com.boot.cleanhub.settlement.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.contract.domain.Contract;
import com.boot.cleanhub.contract.repository.ContractRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.quote.domain.Quote;
import com.boot.cleanhub.quote.repository.QuoteRepository;
import com.boot.cleanhub.settlement.domain.Billing;
import com.boot.cleanhub.settlement.domain.Payment;
import com.boot.cleanhub.settlement.dto.BillingEditRequest;
import com.boot.cleanhub.settlement.dto.BillingResponse;
import com.boot.cleanhub.settlement.dto.PaymentRequest;
import com.boot.cleanhub.settlement.dto.PaymentResponse;
import com.boot.cleanhub.settlement.dto.SettlementMonthResponse;
import com.boot.cleanhub.settlement.repository.BillingRepository;
import com.boot.cleanhub.settlement.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   정산 서비스 — 월 청구 자동 생성 + 청구/입금 관리.
 *   청구(billing)와 입금(payment)을 분리해, 청구액(편집 가능)과 수금액(입금 합)을 모두 보유한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final QuoteRepository quoteRepository;

    /**
     * 월 청구 자동 생성 — 그 달 유효한 ACTIVE 계약마다 청구 1건(청구액=월정액). 이미 있으면 건너뜀.
     *
     * @return 생성된 청구 건수
     */
    @Transactional
    public int generateMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Contract> contracts = contractRepository.findActiveInPeriod(start, end);
        int created = 0;
        for (Contract c : contracts) {
            if (billingRepository.existsByContract_IdAndBillYearAndBillMonth(c.getId(), year, month)) {
                continue;
            }
            Billing b = new Billing();
            b.setContract(c);
            b.setBillYear(year);
            b.setBillMonth(month);
            b.setAmount(c.getMonthlyFee());
            billingRepository.save(b);
            created++;
        }
        return created;
    }

    /** 월 정산 조회(청구 목록 + 합계). 수금액은 입금 합으로 계산. */
    public SettlementMonthResponse getMonth(int year, int month) {
        List<Billing> billings = billingRepository.findByMonthWithRefs(year, month);
        Map<Long, Long> paidMap = paidAmounts(billings.stream().map(Billing::getId).collect(Collectors.toList()));
        List<BillingResponse> items = billings.stream()
                .map(b -> BillingResponse.of(b, paidMap.getOrDefault(b.getId(), 0L)))
                .collect(Collectors.toList());
        return new SettlementMonthResponse(year, month, items);
    }

    /** 청구액/메모 수정. */
    @Transactional
    public BillingResponse editBilling(Long billingId, BillingEditRequest request) {
        Billing b = billingRepository.findByIdWithRefs(billingId)
                .orElseThrow(() -> new BizException(ErrorCode.BILLING_NOT_FOUND));
        b.setAmount(request.getAmount());
        b.setMemo(request.getMemo());
        billingRepository.saveAndFlush(b);
        return BillingResponse.of(b, paymentRepository.sumByBillingId(billingId));
    }

    /** 청구 삭제(입금은 FK cascade 로 함께 삭제). */
    @Transactional
    public void deleteBilling(Long billingId) {
        Billing b = billingRepository.findById(billingId)
                .orElseThrow(() -> new BizException(ErrorCode.BILLING_NOT_FOUND));
        billingRepository.delete(b);
    }

    /** 수락된 견적을 특정 연월의 1회성 청구로 생성. */
    @Transactional
    public BillingResponse createQuoteBilling(Long quoteId, int year, int month) {
        Quote q = quoteRepository.findByIdWithClient(quoteId)
                .orElseThrow(() -> new BizException(ErrorCode.QUOTE_NOT_FOUND));
        Billing b = new Billing();
        b.setQuote(q);
        b.setBillYear(year);
        b.setBillMonth(month);
        b.setAmount(q.getAmount());
        billingRepository.save(b);
        return BillingResponse.of(b, 0L);
    }

    // ===== 입금 =====

    /** 청구의 입금 목록. */
    public List<PaymentResponse> listPayments(Long billingId) {
        return paymentRepository.findByBilling_IdOrderById(billingId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    /** 입금 등록. */
    @Transactional
    public PaymentResponse addPayment(Long billingId, PaymentRequest request) {
        Billing b = billingRepository.findById(billingId)
                .orElseThrow(() -> new BizException(ErrorCode.BILLING_NOT_FOUND));
        Payment p = new Payment();
        p.setBilling(b);
        p.setAmount(request.getAmount());
        p.setPaidDate(request.getPaidDate());
        p.setMethod(request.getMethod());
        p.setMemo(request.getMemo());
        return PaymentResponse.from(paymentRepository.save(p));
    }

    /** 입금 삭제. */
    @Transactional
    public void deletePayment(Long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BizException(ErrorCode.PAYMENT_NOT_FOUND));
        paymentRepository.delete(p);
    }

    /** 청구 id 목록 → 입금합 map(없으면 0). */
    private Map<Long, Long> paidAmounts(List<Long> billingIds) {
        Map<Long, Long> map = new HashMap<>();
        if (billingIds.isEmpty()) {
            return map;
        }
        for (Object[] row : paymentRepository.sumGroupedByBillingIds(billingIds)) {
            map.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
