package com.boot.cleanhub.settlement.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.settlement.dto.TaxInvoiceAggResponse;
import com.boot.cleanhub.settlement.dto.TaxInvoiceIssueRequest;
import com.boot.cleanhub.settlement.dto.TaxInvoiceResponse;
import com.boot.cleanhub.settlement.service.TaxInvoiceService;
import com.boot.cleanhub.util.file.FileUtillMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   세금계산서 API — 관리자 전용. 기간·거래처 집계 + 발행 기록 + 집계표 엑셀 출력.
 *   /api/admin/** 이므로 ROLE_ADMIN 보호.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/tax-invoices")
@RequiredArgsConstructor
public class TaxInvoiceAdminController {

    private final TaxInvoiceService taxInvoiceService;

    /** 거래처별 기간 집계(청구/수금 기준) 미리보기 */
    @GetMapping("/aggregate")
    public ApiResponse<TaxInvoiceAggResponse> aggregate(
            @RequestParam int year,
            @RequestParam int fromMonth,
            @RequestParam int toMonth,
            @RequestParam(required = false, defaultValue = "BILLED") String basis) {
        return ApiResponse.ok(taxInvoiceService.aggregate(year, fromMonth, toMonth, basis));
    }

    /** 집계표 엑셀(xlsx) 다운로드 */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(
            @RequestParam int year,
            @RequestParam int fromMonth,
            @RequestParam int toMonth,
            @RequestParam(required = false, defaultValue = "BILLED") String basis) {
        byte[] bytes = taxInvoiceService.buildSummaryExcel(year, fromMonth, toMonth, basis);
        String filename = "세금계산서집계_" + year + "_" + fromMonth + "-" + toMonth + ".xlsx";
        return FileUtillMo.downloadResponse(bytes, filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /** 발행 기록 목록 */
    @GetMapping
    public ApiResponse<List<TaxInvoiceResponse>> list() {
        return ApiResponse.ok(taxInvoiceService.list());
    }

    /** 발행 기록 저장 */
    @PostMapping("/issue")
    public ApiResponse<TaxInvoiceResponse> issue(@Valid @RequestBody TaxInvoiceIssueRequest request) {
        return ApiResponse.ok(taxInvoiceService.issue(request), "세금계산서 발행 기록이 저장되었습니다.");
    }

    /** 발행 기록 삭제 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taxInvoiceService.delete(id);
        return ApiResponse.ok(null, "발행 기록이 삭제되었습니다.");
    }
}
