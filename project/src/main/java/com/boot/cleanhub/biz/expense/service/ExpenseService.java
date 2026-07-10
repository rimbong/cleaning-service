package com.boot.cleanhub.biz.expense.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boot.cleanhub.common.dto.PageResponse;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.biz.expense.domain.Expense;
import com.boot.cleanhub.biz.expense.dto.ExpenseRequest;
import com.boot.cleanhub.biz.expense.dto.ExpenseResponse;
import com.boot.cleanhub.biz.expense.repository.ExpenseRepository;
import com.boot.cleanhub.util.excel.PoiMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   지출 도메인 서비스 — 등록/조회/수정/삭제.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public PageResponse<ExpenseResponse> list(String keyword, Pageable pageable) {
        Page<Expense> expenses;
        if (StringUtils.hasText(keyword)) {
            expenses = expenseRepository
                    .findByVendorNameContainingIgnoreCaseOrderByExpenseDateDescIdDesc(keyword.trim(), pageable);
        } else {
            expenses = expenseRepository.findAllByOrderByExpenseDateDescIdDesc(pageable);
        }
        return PageResponse.from(expenses.map(ExpenseResponse::from));
    }

    public ExpenseResponse get(Long id) {
        return ExpenseResponse.from(findOrThrow(id));
    }

    /**
     * 지출 내역 엑셀(xlsx) — 세금계산서자동합계.xlsx 의 지출/주유 시트 레이아웃.
     * 분류·거래처(주유소)·사업자번호·금액·지출일·메모 + 금액 합계(SUM 수식). 공용 PoiMo 로 생성.
     *
     * @param keyword 거래처/주유소명 검색어(비면 전체)
     * @return xlsx 바이트
     */
    public byte[] buildExcel(String keyword) {
        List<Expense> list = StringUtils.hasText(keyword)
                ? expenseRepository.findByVendorNameContainingIgnoreCaseOrderByExpenseDateDescIdDesc(keyword.trim())
                : expenseRepository.findAllByOrderByExpenseDateDescIdDesc();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PoiMo poi = PoiMo.create("지출내역.xlsx");
            try {
                CellStyle title = poi.createNewStyle();
                poi.setFontStyle(title, "맑은 고딕", (short) 14, "bold", false, false);
                CellStyle head = poi.createNewStyle();
                poi.setFontStyle(head, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setBackgroundColor(head, "light-yellow");
                poi.setLineBorder(head, "thin");
                CellStyle body = poi.createNewStyle();
                poi.setLineBorder(body, "thin");
                CellStyle num = poi.createNewStyle();
                poi.setLineBorder(num, "thin");
                poi.setAlign(num, "r");
                poi.setNumberFormat(num, "#,##0");
                CellStyle total = poi.createNewStyle();
                poi.setFontStyle(total, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setLineBorder(total, "thin");
                CellStyle numTotal = poi.createNewStyle();
                poi.setFontStyle(numTotal, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setLineBorder(numTotal, "thin");
                poi.setAlign(numTotal, "r");
                poi.setNumberFormat(numTotal, "#,##0");

                // 컬럼: 순번 | 분류 | 거래처/주유소 | 사업자번호 | 금액 | 지출일 | 메모
                String[] cols = { "순번", "분류", "거래처/주유소", "사업자번호", "금액", "지출일", "메모" };
                int amountCol = 4;

                poi.setMergedData(title, 0, 0, cols.length - 1, "지출 내역 (주유·경비)$c");
                for (int i = 0; i < cols.length; i++) {
                    poi.setData(head, 2, i, cols[i] + "$c");
                }

                final int firstDataRow = 3;
                int r = firstDataRow;
                int no = 1;
                for (Expense e : list) {
                    poi.setData(body, r, 0, no++ + "$c");
                    poi.setData(body, r, 1, e.getCategory() != null ? e.getCategory().getLabel() + "$c" : "");
                    poi.setData(body, r, 2, e.getVendorName() != null ? e.getVendorName() : "");
                    poi.setData(body, r, 3, e.getBusinessNumber() != null ? e.getBusinessNumber() : "");
                    poi.setNumber(num, r, amountCol, e.getAmount() != null ? e.getAmount() : 0L);
                    poi.setData(body, r, 5, e.getExpenseDate() != null ? e.getExpenseDate().toString() + "$c" : "");
                    poi.setData(body, r, 6, e.getMemo() != null ? e.getMemo() : "");
                    r++;
                }

                // 합계 행 — 금액 SUM 수식
                poi.setData(total, r, 0, "");
                poi.setData(total, r, 1, "");
                poi.setData(total, r, 2, "");
                poi.setData(total, r, 3, "합계$c");
                if (!list.isEmpty()) {
                    int lastDataRow = r - 1;
                    poi.setFormula(numTotal, r, amountCol,
                            "SUM(" + new CellReference(firstDataRow, amountCol).formatAsString()
                                    + ":" + new CellReference(lastDataRow, amountCol).formatAsString() + ")");
                } else {
                    poi.setNumber(numTotal, r, amountCol, 0L);
                }
                poi.setData(total, r, 5, "");
                poi.setData(total, r, 6, "");

                poi.evaluateAllFormulas();
                poi.write(out);
            } finally {
                poi.close();
            }
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findOrThrow(id);
        apply(expense, request);
        expenseRepository.saveAndFlush(expense);
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.delete(findOrThrow(id));
    }

    private void apply(Expense expense, ExpenseRequest request) {
        expense.setCategory(request.getCategory());
        expense.setVendorName(request.getVendorName());
        expense.setBusinessNumber(request.getBusinessNumber());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setMemo(request.getMemo());
    }

    private Expense findOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.EXPENSE_NOT_FOUND));
    }
}
