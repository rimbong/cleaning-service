package com.boot.cleanhub.biz.settlement.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.biz.settlement.dto.BillingEditRequest;
import com.boot.cleanhub.biz.settlement.dto.BillingResponse;
import com.boot.cleanhub.biz.settlement.dto.PaymentRequest;
import com.boot.cleanhub.biz.settlement.dto.PaymentResponse;
import com.boot.cleanhub.biz.settlement.dto.SettlementMonthResponse;
import com.boot.cleanhub.biz.settlement.dto.YearlyCollectionResponse;
import com.boot.cleanhub.biz.settlement.service.SettlementService;
import com.boot.cleanhub.util.file.FileUtillMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   정산 API — 관리자 전용. 월 청구 생성/조회 + 청구/입금 관리.
 *   /api/admin/** 이므로 ROLE_ADMIN 보호.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class SettlementAdminController {

    private final SettlementService settlementService;

    /** 월 정산 조회(청구 목록 + 합계) */
    @GetMapping
    public ApiResponse<SettlementMonthResponse> getMonth(
            @RequestParam int year, @RequestParam int month) {
        return ApiResponse.ok(settlementService.getMonth(year, month));
    }

    /** 월 청구 자동 생성(활성 계약 기준) */
    @PostMapping("/generate")
    public ApiResponse<Integer> generate(
            @RequestParam int year, @RequestParam int month) {
        int created = settlementService.generateMonth(year, month);
        return ApiResponse.ok(created, created + "건의 청구가 생성되었습니다.");
    }

    /** 연간 거래처 수금 현황(거래처 x 12개월 수금일 매트릭스) */
    @GetMapping("/yearly")
    public ApiResponse<YearlyCollectionResponse> yearly(@RequestParam int year) {
        return ApiResponse.ok(settlementService.getYearlyCollection(year));
    }

    /** 연간 거래처 수금 현황 엑셀(xlsx) 다운로드 */
    @GetMapping("/yearly/excel")
    public ResponseEntity<byte[]> yearlyExcel(@RequestParam int year) {
        byte[] bytes = settlementService.buildYearlyCollectionExcel(year);
        String filename = "거래처수금현황_" + year + ".xlsx";
        return FileUtillMo.downloadResponse(bytes, filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /** 청구액/메모 수정 */
    @PutMapping("/{billingId}")
    public ApiResponse<BillingResponse> editBilling(
            @PathVariable Long billingId, @Valid @RequestBody BillingEditRequest request) {
        return ApiResponse.ok(settlementService.editBilling(billingId, request), "청구가 수정되었습니다.");
    }

    /** 청구 삭제 */
    @DeleteMapping("/{billingId}")
    public ApiResponse<Void> deleteBilling(@PathVariable Long billingId) {
        settlementService.deleteBilling(billingId);
        return ApiResponse.ok(null, "청구가 삭제되었습니다.");
    }

    /** 견적 1회성 청구 생성 */
    @PostMapping("/quote/{quoteId}")
    public ApiResponse<BillingResponse> createQuoteBilling(
            @PathVariable Long quoteId, @RequestParam int year, @RequestParam int month) {
        return ApiResponse.ok(settlementService.createQuoteBilling(quoteId, year, month), "견적 청구가 생성되었습니다.");
    }

    /** 청구의 입금 목록 */
    @GetMapping("/{billingId}/payments")
    public ApiResponse<List<PaymentResponse>> listPayments(@PathVariable Long billingId) {
        return ApiResponse.ok(settlementService.listPayments(billingId));
    }

    /** 입금 등록 */
    @PostMapping("/{billingId}/payments")
    public ApiResponse<PaymentResponse> addPayment(
            @PathVariable Long billingId, @Valid @RequestBody PaymentRequest request) {
        return ApiResponse.ok(settlementService.addPayment(billingId, request), "입금이 기록되었습니다.");
    }

    /** 입금 삭제 */
    @DeleteMapping("/payments/{paymentId}")
    public ApiResponse<Void> deletePayment(@PathVariable Long paymentId) {
        settlementService.deletePayment(paymentId);
        return ApiResponse.ok(null, "입금이 삭제되었습니다.");
    }
}
