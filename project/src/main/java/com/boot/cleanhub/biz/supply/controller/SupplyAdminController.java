package com.boot.cleanhub.biz.supply.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.biz.supply.dto.SupplyItemRequest;
import com.boot.cleanhub.biz.supply.dto.SupplyItemResponse;
import com.boot.cleanhub.biz.supply.dto.SupplyTransactionRequest;
import com.boot.cleanhub.biz.supply.dto.SupplyTransactionResponse;
import com.boot.cleanhub.biz.supply.service.SupplyService;
import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.common.dto.PageRequestFactory;
import com.boot.cleanhub.common.dto.PageResponse;
import com.boot.cleanhub.util.file.FileUtillMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   약품/소모품 재고 API — 관리자 전용. /api/admin/** 이므로 ROLE_ADMIN 보호.
 *   품목 CRUD 와 품목별 입출고 등록/조회를 함께 제공한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/supplies")
@RequiredArgsConstructor
public class SupplyAdminController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final SupplyService supplyService;

    /** 재고 현황 엑셀(xlsx) 다운로드(품목명 검색 반영) */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(required = false) String keyword) {
        byte[] bytes = supplyService.buildExcel(keyword);
        return FileUtillMo.downloadResponse(bytes, "약품재고현황.xlsx", XLSX_CONTENT_TYPE);
    }

    @GetMapping
    public ApiResponse<PageResponse<SupplyItemResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(supplyService.list(keyword, PageRequestFactory.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SupplyItemResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(supplyService.get(id));
    }

    @PostMapping
    public ApiResponse<SupplyItemResponse> create(@Valid @RequestBody SupplyItemRequest request) {
        return ApiResponse.ok(supplyService.create(request), "품목이 등록되었습니다.");
    }

    @PutMapping("/{id}")
    public ApiResponse<SupplyItemResponse> update(@PathVariable Long id,
            @Valid @RequestBody SupplyItemRequest request) {
        return ApiResponse.ok(supplyService.update(id, request), "품목이 수정되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplyService.delete(id);
        return ApiResponse.ok(null, "품목이 삭제되었습니다.");
    }

    /** 품목별 입출고 이력(최신순, 페이징) */
    @GetMapping("/{id}/transactions")
    public ApiResponse<PageResponse<SupplyTransactionResponse>> history(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(supplyService.history(id, PageRequestFactory.of(page, size)));
    }

    /** 입고/사용/조정 등록 */
    @PostMapping("/{id}/transactions")
    public ApiResponse<SupplyTransactionResponse> addTransaction(@PathVariable Long id,
            @Valid @RequestBody SupplyTransactionRequest request) {
        return ApiResponse.ok(supplyService.addTransaction(id, request), "입출고가 등록되었습니다.");
    }

    /** 잘못 등록한 입출고 이력 삭제 */
    @DeleteMapping("/{id}/transactions/{transactionId}")
    public ApiResponse<Void> deleteTransaction(@PathVariable Long id, @PathVariable Long transactionId) {
        supplyService.deleteTransaction(id, transactionId);
        return ApiResponse.ok(null, "입출고 내역이 삭제되었습니다.");
    }
}
