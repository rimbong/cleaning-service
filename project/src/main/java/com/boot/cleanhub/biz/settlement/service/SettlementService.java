package com.boot.cleanhub.biz.settlement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.biz.contract.domain.Contract;
import com.boot.cleanhub.biz.contract.repository.ContractRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.biz.quote.domain.Quote;
import com.boot.cleanhub.biz.quote.domain.QuoteStatus;
import com.boot.cleanhub.biz.quote.repository.QuoteRepository;
import com.boot.cleanhub.biz.settlement.domain.Billing;
import com.boot.cleanhub.biz.settlement.domain.Payment;
import com.boot.cleanhub.biz.settlement.dto.BillingEditRequest;
import com.boot.cleanhub.biz.settlement.dto.BillingResponse;
import com.boot.cleanhub.biz.settlement.dto.PaymentRequest;
import com.boot.cleanhub.biz.settlement.dto.PaymentResponse;
import com.boot.cleanhub.biz.settlement.dto.SettlementMonthResponse;
import com.boot.cleanhub.biz.settlement.dto.YearlyCollectionGroup;
import com.boot.cleanhub.biz.settlement.dto.YearlyCollectionResponse;
import com.boot.cleanhub.biz.settlement.dto.YearlyCollectionRow;
import com.boot.cleanhub.biz.settlement.repository.BillingRepository;
import com.boot.cleanhub.biz.settlement.repository.PaymentRepository;
import com.boot.cleanhub.util.excel.PoiMo;

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
     * 이미 청구가 있는 계약은 한 번에 preload 해 계약마다 존재확인(N+1)을 없앤다.
     * 동시 실행(중복 클릭 등)으로 유니크 제약(uq_billing_contract_month) 충돌 시 500 대신 409 로 응답한다.
     *
     * @return 생성된 청구 건수
     */
    @Transactional
    public int generateMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Contract> contracts = contractRepository.findActiveInPeriod(start, end);
        // 이미 청구가 있는 계약 id 를 한 번에 조회(계약마다 existsBy 호출하던 N+1 제거)
        Set<Long> alreadyBilled = new HashSet<>(billingRepository.findContractIdsWithBilling(year, month));
        int created = 0;
        try {
            for (Contract c : contracts) {
                if (alreadyBilled.contains(c.getId())) {
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
        } catch (DataIntegrityViolationException e) {
            // 두 요청이 동시에 같은 계약·연월을 생성 → 유니크 위반. 데이터는 안전(중복 저장 안 됨).
            throw new BizException(ErrorCode.BILLING_GENERATION_CONFLICT);
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

    /** 수락된 견적을 특정 연월의 1회성 청구로 생성(수락 상태만·중복 방지). */
    @Transactional
    public BillingResponse createQuoteBilling(Long quoteId, int year, int month) {
        Quote q = quoteRepository.findByIdWithClient(quoteId)
                .orElseThrow(() -> new BizException(ErrorCode.QUOTE_NOT_FOUND));
        // 수락된 견적만 청구로 전환(대기/거절 견적 방지)
        if (q.getStatus() != QuoteStatus.ACCEPTED) {
            throw new BizException(ErrorCode.QUOTE_NOT_ACCEPTED);
        }
        // 같은 견적·연월 중복 청구 방지(멱등)
        if (billingRepository.existsByQuote_IdAndBillYearAndBillMonth(quoteId, year, month)) {
            throw new BizException(ErrorCode.BILLING_ALREADY_EXISTS);
        }
        Billing b = new Billing();
        b.setQuote(q);
        b.setBillYear(year);
        b.setBillMonth(month);
        b.setAmount(q.getAmount());
        billingRepository.save(b);
        return BillingResponse.of(b, 0L);
    }

    // ===== 연간 수금 현황 (수정환경.xls 재현) =====

    /** 월 인덱스(1~12) → 배열 인덱스 변환용 상수. */
    private static final int MONTHS = 12;
    /** 요일 코드/라벨 — 수금 현황 블록 순서. */
    private static final String[] WEEKDAY_CODES = { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };
    private static final String[] WEEKDAY_LABELS = { "월", "화", "수", "목", "금", "토", "일" };

    /**
     * 연간 거래처 수금 현황 — 그 해 유효했던 계약(거래처)마다 1~12월 최종 수금일을 매트릭스로,
     * 거래처를 청소 요일 블록(월~일, 미지정)으로 묶는다(수정환경.xls 요일 블록 구조).
     * 한 거래처가 여러 요일(월수금)이면 각 요일 블록에 모두 들어간다.
     * 기존 정산(청구/입금)에서 파생한다(입금이 있으면 그 달 최종 수금일을 "M/D" 로).
     *
     * @param year 대상 연도
     * @return 요일 블록별 거래처 월별 수금일 현황
     */
    public YearlyCollectionResponse getYearlyCollection(int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        List<Contract> contracts = contractRepository.findOverlappingPeriod(yearStart, yearEnd);

        // (계약id, 월) → 청구id, 그리고 청구id → 최종 수금일 (그 해 1~12월 전체)
        List<Billing> billings = billingRepository.findByPeriodWithRefs(year * 100 + 1, year * 100 + MONTHS);
        Map<Long, Long> billingIdByKey = new HashMap<>();
        List<Long> billingIds = new ArrayList<>();
        for (Billing b : billings) {
            if (b.getContract() == null) {
                continue; // 견적 청구는 거래처 현황에서 제외
            }
            billingIds.add(b.getId());
            billingIdByKey.put(rowKey(b.getContract().getId(), b.getBillMonth()), b.getId());
        }
        Map<Long, LocalDate> dateByBilling = new HashMap<>();
        if (!billingIds.isEmpty()) {
            for (Object[] row : paymentRepository.findLatestPaidDateByBillingIds(billingIds)) {
                dateByBilling.put((Long) row[0], (LocalDate) row[1]);
            }
        }

        // 요일 블록 준비(월~일) + 미지정
        Map<String, List<YearlyCollectionRow>> byDay = new LinkedHashMap<>();
        for (String code : WEEKDAY_CODES) {
            byDay.put(code, new ArrayList<>());
        }
        List<YearlyCollectionRow> unassigned = new ArrayList<>();

        for (Contract c : contracts) {
            List<String> months = new ArrayList<>(MONTHS);
            for (int m = 1; m <= MONTHS; m++) {
                Long billingId = billingIdByKey.get(rowKey(c.getId(), m));
                LocalDate paid = billingId != null ? dateByBilling.get(billingId) : null;
                months.add(paid != null ? (paid.getMonthValue() + "/" + paid.getDayOfMonth()) : "");
            }
            YearlyCollectionRow row = YearlyCollectionRow.of(c, months);
            String weekdays = c.getCleaningWeekdays();
            if (weekdays == null || weekdays.trim().isEmpty()) {
                unassigned.add(row);
            } else {
                for (String code : weekdays.split(",")) {
                    List<YearlyCollectionRow> list = byDay.get(code.trim());
                    if (list != null) {
                        list.add(row); // 다중 요일이면 각 블록에 모두
                    }
                }
            }
        }

        List<YearlyCollectionGroup> groups = new ArrayList<>();
        for (int i = 0; i < WEEKDAY_CODES.length; i++) {
            List<YearlyCollectionRow> dayRows = byDay.get(WEEKDAY_CODES[i]);
            if (!dayRows.isEmpty()) {
                groups.add(new YearlyCollectionGroup(WEEKDAY_CODES[i], WEEKDAY_LABELS[i], dayRows));
            }
        }
        if (!unassigned.isEmpty()) {
            groups.add(new YearlyCollectionGroup("NONE", "요일 미지정", unassigned));
        }
        return new YearlyCollectionResponse(year, groups);
    }

    /** (계약id, 월) 을 하나의 long 키로 — 매트릭스 조합용. */
    private static long rowKey(long contractId, int month) {
        return contractId * 100L + month;
    }

    /**
     * 연간 수금 현황 엑셀(xlsx) — 수정환경.xls "거래처 현황(청소)" 레이아웃.
     * 청소 요일 블록(월~일)별로 나눠, 각 블록에 요일 헤더 + 컬럼 헤더 + 거래처 행을 그린다.
     * 공용 유틸 PoiMo 로 생성(제목 병합 + 한글 폭 자동조정).
     *
     * @param year 대상 연도
     * @return xlsx 바이트
     */
    public byte[] buildYearlyCollectionExcel(int year) {
        YearlyCollectionResponse data = getYearlyCollection(year);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PoiMo poi = PoiMo.create("거래처수금현황.xlsx");
            try {
                CellStyle titleStyle = poi.createNewStyle();
                poi.setFontStyle(titleStyle, "맑은 고딕", (short) 14, "bold", false, false);

                CellStyle dayStyle = poi.createNewStyle();
                poi.setFontStyle(dayStyle, "맑은 고딕", (short) 12, "bold", false, false);
                poi.setBackgroundColor(dayStyle, "orange");
                poi.setLineBorder(dayStyle, "thin");

                CellStyle head = poi.createNewStyle();
                poi.setFontStyle(head, "맑은 고딕", (short) 10, "bold", false, false);
                poi.setBackgroundColor(head, "light-yellow");
                poi.setLineBorder(head, "thin");

                CellStyle body = poi.createNewStyle();
                poi.setLineBorder(body, "thin");

                // 컬럼: 순번 | 거래처 | 담당자 | 결재 | 주소 | 전화번호 | 비밀 | 수금 | 금액 | 1월~12월
                String[] fixedCols = { "순번", "거래처", "담당자", "결재", "주소", "전화번호", "비밀", "수금", "금액" };
                int totalCols = fixedCols.length + MONTHS;

                // 제목(0행) — 전체 열 병합
                poi.setMergedData(titleStyle, 0, 0, totalCols - 1, year + "년 거래처 수금 현황 (청소)$c");

                int r = 2;
                for (YearlyCollectionGroup g : data.getGroups()) {
                    // 요일 블록 헤더(전체 열 병합)
                    poi.setMergedData(dayStyle, r, 0, totalCols - 1,
                            "[ " + g.getLabel() + " ]  " + g.getCount() + "곳$l");
                    r++;
                    // 컬럼 헤더
                    for (int i = 0; i < fixedCols.length; i++) {
                        poi.setData(head, r, i, fixedCols[i] + "$c");
                    }
                    for (int m = 1; m <= MONTHS; m++) {
                        poi.setData(head, r, fixedCols.length + m - 1, m + "월$c");
                    }
                    r++;
                    // 데이터 행
                    int no = 1;
                    for (YearlyCollectionRow row : g.getRows()) {
                        poi.setData(body, r, 0, (no++) + "$c");
                        poi.setData(body, r, 1, nvl(row.getClientName()));
                        poi.setData(body, r, 2, nvl(row.getManagerName()));
                        poi.setData(body, r, 3, row.getBillingDay() != null ? row.getBillingDay() + "일$c" : "");
                        poi.setData(body, r, 4, nvl(row.getAddress()));
                        poi.setData(body, r, 5, nvl(row.getPhone()));
                        poi.setData(body, r, 6, nvl(row.getDoorCode()));
                        poi.setData(body, r, 7, nvl(row.getPaymentMethod()));
                        poi.setData(body, r, 8, row.getMonthlyFee() != null
                                ? String.format("%,d", row.getMonthlyFee()) + "$r" : "");
                        List<String> months = row.getMonths();
                        for (int m = 0; m < MONTHS; m++) {
                            String v = m < months.size() ? months.get(m) : "";
                            poi.setData(body, r, fixedCols.length + m, v.isEmpty() ? "" : v + "$c");
                        }
                        r++;
                    }
                    r++; // 블록 사이 공백 행
                }

                poi.write(out);
            } finally {
                poi.close();
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /** null 을 빈 문자열로. */
    private static String nvl(String s) {
        return s != null ? s : "";
    }

    // ===== 입금 =====

    /** 청구의 입금 목록. */
    public List<PaymentResponse> listPayments(Long billingId) {
        return paymentRepository.findByBilling_IdOrderById(billingId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 입금 등록.
     * 입금액이 남은 잔액(청구액 - 기존 입금합)을 초과하면 거부한다(과입금 방지 → 월 미수합계 왜곡 예방).
     */
    @Transactional
    public PaymentResponse addPayment(Long billingId, PaymentRequest request) {
        Billing b = billingRepository.findById(billingId)
                .orElseThrow(() -> new BizException(ErrorCode.BILLING_NOT_FOUND));
        long billed = b.getAmount() != null ? b.getAmount() : 0L;
        long alreadyPaid = paymentRepository.sumByBillingId(billingId);
        long incoming = request.getAmount() != null ? request.getAmount() : 0L;
        if (alreadyPaid + incoming > billed) {
            throw new BizException(ErrorCode.PAYMENT_EXCEEDS_BALANCE);
        }
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
