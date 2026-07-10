package com.boot.cleanhub.biz.expense.controller;

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

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.common.dto.PageRequestFactory;
import com.boot.cleanhub.common.dto.PageResponse;
import com.boot.cleanhub.biz.expense.dto.ExpenseRequest;
import com.boot.cleanhub.biz.expense.dto.ExpenseResponse;
import com.boot.cleanhub.biz.expense.service.ExpenseService;
import com.boot.cleanhub.util.file.FileUtillMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   지출 관리 API — 관리자 전용. /api/admin/** 이므로 ROLE_ADMIN 보호.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/expenses")
@RequiredArgsConstructor
public class ExpenseAdminController {

    private final ExpenseService expenseService;

    /** 지출 내역 엑셀(xlsx) 다운로드(거래처/주유소명 검색 지원) */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(required = false) String keyword) {
        byte[] bytes = expenseService.buildExcel(keyword);
        return FileUtillMo.downloadResponse(bytes, "지출내역.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping
    public ApiResponse<PageResponse<ExpenseResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(expenseService.list(keyword, PageRequestFactory.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ExpenseResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(expenseService.get(id));
    }

    @PostMapping
    public ApiResponse<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.ok(expenseService.create(request), "지출이 등록되었습니다.");
    }

    @PutMapping("/{id}")
    public ApiResponse<ExpenseResponse> update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return ApiResponse.ok(expenseService.update(id, request), "지출이 수정되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        expenseService.delete(id);
        return ApiResponse.ok(null, "지출이 삭제되었습니다.");
    }
}
