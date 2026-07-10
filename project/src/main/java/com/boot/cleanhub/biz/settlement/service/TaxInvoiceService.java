package com.boot.cleanhub.biz.settlement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.client.repository.ClientRepository;
import com.boot.cleanhub.biz.contract.domain.VatType;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.biz.settlement.domain.Billing;
import com.boot.cleanhub.biz.settlement.domain.TaxInvoice;
import com.boot.cleanhub.biz.settlement.dto.TaxInvoiceAggResponse;
import com.boot.cleanhub.biz.settlement.dto.TaxInvoiceAggRow;
import com.boot.cleanhub.biz.settlement.dto.TaxInvoiceIssueRequest;
import com.boot.cleanhub.biz.settlement.dto.TaxInvoiceResponse;
import com.boot.cleanhub.biz.settlement.repository.BillingRepository;
import com.boot.cleanhub.biz.settlement.repository.PaymentRepository;
import com.boot.cleanhub.biz.settlement.repository.TaxInvoiceRepository;
import com.boot.cleanhub.biz.company.dto.CompanyResponse;
import com.boot.cleanhub.biz.company.service.CompanyService;
import com.boot.cleanhub.util.excel.PoiMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   세금계산서 — 기간·거래처별 집계(청구/수금 기준) + 발행 기록 + 집계표 엑셀 출력.
 *   집계는 정산(billing/payment)에서 계산한다. 세액 = 공급가액 * 10%.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxInvoiceService {

    private static final String BASIS_PAID = "PAID";

    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final TaxInvoiceRepository taxInvoiceRepository;
    private final ClientRepository clientRepository;
    private final CompanyService companyService;

    /**
     * 거래처별 기간 집계(BILLED=청구합 / PAID=수금합).
     * 기간은 (시작연,시작월)~(종료연,종료월) — 연도 경계(예: 2025-11 ~ 2026-02)도 지원한다.
     */
    public TaxInvoiceAggResponse aggregate(int fromYear, int fromMonth, int toYear, int toMonth, String basis) {
        validatePeriod(fromYear, fromMonth, toYear, toMonth);
        String b = BASIS_PAID.equalsIgnoreCase(basis) ? BASIS_PAID : "BILLED";
        // 연·월을 하나의 키(연*100+월)로 만들어 연도 경계 기간도 한 번에 조회
        int fromKey = fromYear * 100 + fromMonth;
        int toKey = toYear * 100 + toMonth;
        List<Billing> billings = billingRepository.findByPeriodWithRefs(fromKey, toKey);

        Map<Long, Long> paidMap = new HashMap<>();
        if (BASIS_PAID.equals(b) && !billings.isEmpty()) {
            List<Long> ids = billings.stream().map(Billing::getId).collect(Collectors.toList());
            // 수금 기준: 입금일이 집계 기간 내인 입금만 합산(기간 밖에 들어온 입금은 제외)
            LocalDate fromDate = YearMonth.of(fromYear, fromMonth).atDay(1);
            LocalDate toDate = YearMonth.of(toYear, toMonth).atEndOfMonth();
            for (Object[] row : paymentRepository.sumGroupedByBillingIdsInPeriod(ids, fromDate, toDate)) {
                paidMap.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        Map<Long, Acc> byClient = new HashMap<>();
        for (Billing bill : billings) {
            Client cl = clientOf(bill);
            Long key = cl != null ? cl.getId() : null;
            long amount = BASIS_PAID.equals(b)
                    ? paidMap.getOrDefault(bill.getId(), 0L)
                    : (bill.getAmount() != null ? bill.getAmount() : 0L);
            // 청구액을 그 계약의 부가세 기준(별도/포함/면세)에 따라 공급가액·세액으로 나눈다.
            long[] sv = splitVat(amount, vatTypeOf(bill));
            Acc acc = byClient.computeIfAbsent(key, k -> new Acc());
            if (acc.name == null) {
                acc.name = cl != null ? cl.getName() : "(거래처 미연결)";
                acc.businessNumber = cl != null ? cl.getBusinessNumber() : null;
            }
            acc.supply += sv[0];
            acc.tax += sv[1];
            acc.count += 1;
        }

        List<TaxInvoiceAggRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Acc> e : byClient.entrySet()) {
            Acc a = e.getValue();
            rows.add(new TaxInvoiceAggRow(e.getKey(), a.name, a.businessNumber, a.supply, a.tax, a.count));
        }
        rows.sort(Comparator.comparing(TaxInvoiceAggRow::getClientName, Comparator.nullsLast(Comparator.naturalOrder())));
        return new TaxInvoiceAggResponse(fromYear, fromMonth, toYear, toMonth, b, rows);
    }

    /**
     * 집계 기간 파라미터 검증 — 월은 1~12, 시작 시점(연,월)은 종료 시점보다 이후일 수 없다.
     * GET 파라미터가 원시 int 라 0·13·역순이 들어오면 조용히 빈 결과가 나오던 것을 400 으로 막는다.
     */
    private static void validatePeriod(int fromYear, int fromMonth, int toYear, int toMonth) {
        if (fromMonth < 1 || fromMonth > 12 || toMonth < 1 || toMonth > 12) {
            throw new BizException(ErrorCode.INVALID_PERIOD);
        }
        if (fromYear * 100 + fromMonth > toYear * 100 + toMonth) {
            throw new BizException(ErrorCode.INVALID_PERIOD);
        }
    }

    /** 기간 라벨 — 같은 해면 "2026년 1~6월", 해를 넘기면 "2025년 11월 ~ 2026년 2월". */
    private static String periodLabel(int fromYear, int fromMonth, int toYear, int toMonth) {
        if (fromYear == toYear) {
            return fromYear + "년 " + fromMonth + "~" + toMonth + "월";
        }
        return fromYear + "년 " + fromMonth + "월 ~ " + toYear + "년 " + toMonth + "월";
    }

    /** 발행 기록 저장 — 그 거래처·기간 집계액으로(중복 발행 방지). */
    @Transactional
    public TaxInvoiceResponse issue(TaxInvoiceIssueRequest req) {
        TaxInvoiceAggResponse agg = aggregate(req.getFromYear(), req.getFromMonth(), req.getToYear(), req.getToMonth(), req.getBasis());
        // 같은 거래처·기간·기준으로 이미 발행됐으면 거부(재발행은 삭제 후 다시)
        if (taxInvoiceRepository.existsByClient_IdAndFromYearAndFromMonthAndToYearAndToMonthAndBasis(
                req.getClientId(), req.getFromYear(), req.getFromMonth(), req.getToYear(), req.getToMonth(), agg.getBasis())) {
            throw new BizException(ErrorCode.TAX_INVOICE_ALREADY_ISSUED);
        }
        TaxInvoiceAggRow row = agg.getRows().stream()
                .filter(r -> Objects.equals(r.getClientId(), req.getClientId()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.BILLING_NOT_FOUND));
        Client client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new BizException(ErrorCode.CLIENT_NOT_FOUND));

        TaxInvoice t = new TaxInvoice();
        t.setClient(client);
        t.setFromYear(req.getFromYear());
        t.setFromMonth(req.getFromMonth());
        t.setToYear(req.getToYear());
        t.setToMonth(req.getToMonth());
        t.setSupplyAmount(row.getSupplyAmount());
        t.setTaxAmount(row.getTaxAmount());
        t.setBasis(agg.getBasis());
        t.setIssueDate(req.getIssueDate());
        return TaxInvoiceResponse.from(taxInvoiceRepository.save(t));
    }

    /** 발행 기록 목록. */
    public List<TaxInvoiceResponse> list() {
        return taxInvoiceRepository.findAllWithClient().stream()
                .map(TaxInvoiceResponse::from)
                .collect(Collectors.toList());
    }

    /** 발행 기록 삭제. */
    @Transactional
    public void delete(Long id) {
        TaxInvoice t = taxInvoiceRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.TAX_INVOICE_NOT_FOUND));
        taxInvoiceRepository.delete(t);
    }

    /**
     * 집계표 엑셀(xlsx) 바이트 — 세금계산서 발행/신고용 거래처별 공급가액·세액 목록.
     * 공용 유틸 {@link PoiMo} 로 생성한다(setData 가 한글 폭을 반영해 열 너비를 자동 조정).
     * 데이터 정렬은 값 뒤 "$c"(가운데)/"$r"(오른쪽) 수식어로 지정한다.
     * 제목은 setMergedData 로 병합해 넣어(열 너비 영향 없음) 순번 열이 넓어지지 않게 한다.
     * 구성: 0행 제목(병합) / 1행 공백 / 2행 헤더 / 3행~ 데이터 / 마지막 합계.
     */
    public byte[] buildSummaryExcel(int fromYear, int fromMonth, int toYear, int toMonth, String basis) {
        TaxInvoiceAggResponse agg = aggregate(fromYear, fromMonth, toYear, toMonth, basis);
        String basisLabel = BASIS_PAID.equals(agg.getBasis()) ? "수금 기준" : "청구 기준";
        String titleText = periodLabel(fromYear, fromMonth, toYear, toMonth) + " 세금계산서 집계 (" + basisLabel + ")";
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PoiMo poi = PoiMo.create("세금계산서집계.xlsx");
            try {
                // 스타일: 제목(굵게 큰 글씨), 헤더(굵게+연노랑+테두리), 본문(테두리), 합계(굵게+테두리)
                CellStyle titleStyle = poi.createNewStyle();
                poi.setFontStyle(titleStyle, "맑은 고딕", (short) 14, "bold", false, false);

                CellStyle head = poi.createNewStyle();
                poi.setFontStyle(head, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setBackgroundColor(head, "light-yellow");
                poi.setLineBorder(head, "thin");

                CellStyle body = poi.createNewStyle();
                poi.setLineBorder(body, "thin");

                CellStyle total = poi.createNewStyle();
                poi.setFontStyle(total, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setLineBorder(total, "thin");

                // 금액용 숫자 스타일(테두리+오른쪽정렬+천단위콤마) — 텍스트가 아닌 진짜 숫자라 SUM 대상이 됨
                CellStyle num = poi.createNewStyle();
                poi.setLineBorder(num, "thin");
                poi.setAlign(num, "r");
                poi.setNumberFormat(num, "#,##0");
                // 건수용 가운데 숫자 스타일
                CellStyle cnt = poi.createNewStyle();
                poi.setLineBorder(cnt, "thin");
                poi.setAlign(cnt, "c");
                // 합계 금액용(굵게 + 숫자)
                CellStyle numTotal = poi.createNewStyle();
                poi.setFontStyle(numTotal, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setLineBorder(numTotal, "thin");
                poi.setAlign(numTotal, "r");
                poi.setNumberFormat(numTotal, "#,##0");

                // 제목(0행) — 0~5열 병합, 가운데 정렬(열 너비에 영향 주지 않음)
                poi.setMergedData(titleStyle, 0, 0, 5, titleText + "$c");

                // 헤더(2행) — "$c" 로 가운데 정렬 (1행은 공백)
                String[] cols = { "순번", "사업자번호", "상호", "공급가액", "세액", "건수" };
                for (int i = 0; i < cols.length; i++) {
                    poi.setData(head, 2, i, cols[i] + "$c");
                }

                // 데이터(3행~). 공급가액·세액·건수는 숫자 셀로 넣어야 합계 SUM 이 동작한다.
                final int firstDataRow = 3;
                int r = firstDataRow;
                int no = 1;
                for (TaxInvoiceAggRow row : agg.getRows()) {
                    poi.setData(body, r, 0, no++ + "$c");
                    poi.setData(body, r, 1, row.getBusinessNumber() != null ? row.getBusinessNumber() : "");
                    poi.setData(body, r, 2, row.getClientName() != null ? row.getClientName() : "");
                    poi.setNumber(num, r, 3, row.getSupplyAmount());
                    poi.setNumber(num, r, 4, row.getTaxAmount());
                    poi.setNumber(cnt, r, 5, row.getCount());
                    r++;
                }

                // 합계 행 — 공급가액/세액은 SUM 수식(엑셀에서 값 수정 시 자동 재계산)
                poi.setData(total, r, 0, "");
                poi.setData(total, r, 1, "");
                poi.setData(total, r, 2, "합계$c");
                if (!agg.getRows().isEmpty()) {
                    int lastDataRow = r - 1;
                    poi.setFormula(numTotal, r, 3, "SUM(" + ref(firstDataRow, 3) + ":" + ref(lastDataRow, 3) + ")");
                    poi.setFormula(numTotal, r, 4, "SUM(" + ref(firstDataRow, 4) + ":" + ref(lastDataRow, 4) + ")");
                } else {
                    poi.setNumber(numTotal, r, 3, 0L);
                    poi.setNumber(numTotal, r, 4, 0L);
                }
                poi.setData(total, r, 5, "");

                // 금액 열(공급가액·세액)은 setNumber/SUM 이라 자동 폭 조정이 안 돼, 합계가 크면 #### 로 잘린다.
                // 큰 합계도 보이도록 너비를 명시한다(상호 열은 이름 길이에 맞춰 자동조정되게 그대로 둔다).
                poi.setColumnWidth(0, 6);   // 순번
                poi.setColumnWidth(1, 15);  // 사업자번호
                poi.setColumnWidth(3, 16);  // 공급가액
                poi.setColumnWidth(4, 16);  // 세액
                poi.setColumnWidth(5, 8);   // 건수

                poi.evaluateAllFormulas(); // 수식 결과를 미리 계산해 캐시(뷰어 호환)
                poi.write(out);
            } finally {
                poi.close();
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 개별 세금계산서(별지 제11호 서식) — 빈 양식 템플릿(templates/tax_resource.xls)을 열어 값만 채운다.
     * 원본 양식의 선·색·병합·라벨을 그대로 유지하려고 코드로 다시 그리지 않고 템플릿을 채우는 방식이다.
     * 한 장에 2부(공급받는자 보관용=위, 공급자 보관용=아래, +24행 오프셋)가 있어 같은 값을 양쪽에 기입한다.
     * 공급자는 회사정보(Company), 공급받는자는 거래처(Client), 금액은 발행 기록(TaxInvoice)에서 가져온다.
     *
     * @param id        발행 기록(TaxInvoice) id
     * @param withStamp 회사 도장(인장) 포함 여부(등록된 도장이 있을 때만)
     * @return 별지11호 양식 xls 바이트
     */
    public byte[] buildInvoiceForm(Long id, boolean withStamp) {
        TaxInvoice ti = taxInvoiceRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.TAX_INVOICE_NOT_FOUND));
        CompanyResponse supplier = companyService.get();
        Client buyer = ti.getClient();
        long supply = ti.getSupplyAmount() != null ? ti.getSupplyAmount() : 0L;
        long tax = ti.getTaxAmount() != null ? ti.getTaxAmount() : 0L;
        LocalDate issue = ti.getIssueDate();

        try (InputStream in = new ClassPathResource("templates/tax_resource.xls").getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Workbook wb = new HSSFWorkbook(in);
            Sheet sheet = wb.getSheetAt(0);
            // 위(공급받는자 보관용)/아래(공급자 보관용) 두 부에 동일 데이터 기입
            for (int base : new int[] { 0, 24 }) {
                fillInvoiceCopy(sheet, base, supplier, buyer, supply, tax, issue);
            }
            // 도장 포함 요청 시, 등록된 회사 도장을 공급자·영수 자리에 찍는다(2부 모두).
            if (withStamp) {
                byte[] stamp = companyService.getStampBytes();
                if (stamp != null) {
                    insertStamps(wb, sheet, stamp);
                }
            }
            wb.write(out);
            wb.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 도장 이미지를 각 부(공급받는자용=위, 공급자용=아래)에 2군데씩 찍는다:
     * (1) 공급자 성명/(인) 자리, (2) 우하단 "이 금액을 (영수) 함" 자리(영수 도장).
     * 이미지 종류(PNG/JPEG)는 바이트 시그니처로 판별한다.
     */
    private static void insertStamps(Workbook wb, Sheet sheet, byte[] stamp) {
        int picIdx = wb.addPicture(stamp, detectPictureType(stamp));
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        for (int base : new int[] { 0, 24 }) {
            // 공급자 도장 — 성명/(인) 부근(대략 열 13~18, 행 base+5~base+10)
            placePicture(wb, drawing, picIdx, 13, base + 5, 18, base + 10);
            // 영수 도장 — 우하단 "(영수) 함" 부근(대략 열 28~33, 행 base+19~base+23)
            placePicture(wb, drawing, picIdx, 28, base + 19, 33, base + 23);
        }
    }

    /** 지정한 셀 영역(col1,row1)~(col2,row2)에 그림을 배치. */
    private static void placePicture(Workbook wb, Drawing<?> drawing, int picIdx,
            int col1, int row1, int col2, int row2) {
        ClientAnchor anchor = wb.getCreationHelper().createClientAnchor();
        anchor.setCol1(col1);
        anchor.setRow1(row1);
        anchor.setCol2(col2);
        anchor.setRow2(row2);
        drawing.createPicture(anchor, picIdx);
    }

    /** 이미지 바이트 시그니처로 POI 그림 종류 판별(PNG/JPEG, 그 외는 PNG 로 가정). */
    private static int detectPictureType(byte[] bytes) {
        if (bytes != null && bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        return Workbook.PICTURE_TYPE_PNG;
    }

    /** 사업자등록번호 자릿수 칸(대시는 양식에 이미 있음) — 공급자/공급받는자. */
    private static final int[] SUPPLIER_REG_COLS = { 5, 6, 7, 9, 10, 12, 13, 14, 15, 16 };
    private static final int[] BUYER_REG_COLS = { 21, 22, 23, 25, 26, 28, 29, 30, 31, 32 };

    /**
     * 별지11호 한 부를 채운다(base = 행 오프셋; 0=위 보관용, 24=아래 보관용).
     * 셀 스타일(파란 선·글꼴)은 템플릿 것을 그대로 유지하고 값만 넣는다.
     */
    private void fillInvoiceCopy(Sheet sheet, int base, CompanyResponse supplier, Client buyer,
            long supply, long tax, LocalDate issue) {
        // 공급자
        placeRegNumber(sheet, base + 4, SUPPLIER_REG_COLS, supplier.getBusinessNumber());
        setCellText(sheet, base + 6, 5, supplier.getCompanyName());
        setCellText(sheet, base + 6, 12, supplier.getOwnerName());
        setCellText(sheet, base + 8, 5, supplier.getAddress());
        setCellText(sheet, base + 10, 5, supplier.getBusinessType());
        setCellText(sheet, base + 10, 12, supplier.getBusinessItem());
        // 공급받는자
        placeRegNumber(sheet, base + 4, BUYER_REG_COLS, buyer.getBusinessNumber());
        setCellText(sheet, base + 6, 21, buyer.getName());
        setCellText(sheet, base + 6, 28, buyer.getRepresentativeName());
        setCellText(sheet, base + 8, 21, buyer.getAddress());
        setCellText(sheet, base + 10, 21, buyer.getBusinessType());
        setCellText(sheet, base + 10, 28, buyer.getBusinessItem());
        // 작성일(년/월/일)
        if (issue != null) {
            setCellNumber(sheet, base + 14, 1, issue.getYear());
            setCellNumber(sheet, base + 14, 3, issue.getMonthValue());
            setCellNumber(sheet, base + 14, 4, issue.getDayOfMonth());
        }
        // 공급가액(백…일 c7~c17) / 세액(십…일 c18~c27) 자릿수 기입
        placeAmountDigits(sheet, base + 14, 7, 17, supply);
        placeAmountDigits(sheet, base + 14, 18, 27, tax);
        // 품목 첫 행(월/일/품목/공급가액/세액)
        if (issue != null) {
            setCellNumber(sheet, base + 16, 1, issue.getMonthValue());
            setCellNumber(sheet, base + 16, 2, issue.getDayOfMonth());
        }
        setCellText(sheet, base + 16, 3, "청소비");
        setCellNumber(sheet, base + 16, 20, supply);
        setCellNumber(sheet, base + 16, 26, tax);
        // 합계금액
        setCellNumber(sheet, base + 21, 1, supply + tax);
    }

    /** 셀에 문자열 기입(빈 값이면 건너뜀) — 템플릿 셀 스타일 유지. */
    private static void setCellText(Sheet sheet, int rowIdx, int colIdx, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        cellAt(sheet, rowIdx, colIdx).setCellValue(value);
    }

    /** 셀에 숫자 기입 — 템플릿 셀 스타일(표시 형식 포함) 유지. */
    private static void setCellNumber(Sheet sheet, int rowIdx, int colIdx, double value) {
        cellAt(sheet, rowIdx, colIdx).setCellValue(value);
    }

    /** (row,col) 셀을 얻는다(행/셀 없으면 생성). */
    private static Cell cellAt(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        return cell;
    }

    /** 사업자등록번호에서 숫자만 뽑아 지정 칸에 한 자리씩(대시는 양식에 이미 있음). */
    private static void placeRegNumber(Sheet sheet, int rowIdx, int[] cols, String bizNumber) {
        if (bizNumber == null) {
            return;
        }
        String digits = bizNumber.replaceAll("[^0-9]", "");
        for (int i = 0; i < cols.length && i < digits.length(); i++) {
            cellAt(sheet, rowIdx, cols[i]).setCellValue(String.valueOf(digits.charAt(i)));
        }
    }

    /** 금액을 자릿수 칸(startCol~endCol)에 오른쪽부터 한 자리씩 채운다. */
    private static void placeAmountDigits(Sheet sheet, int rowIdx, int startCol, int endCol, long amount) {
        String s = String.valueOf(Math.max(0L, amount));
        int di = s.length() - 1;
        for (int col = endCol; col >= startCol && di >= 0; col--) {
            cellAt(sheet, rowIdx, col).setCellValue(String.valueOf(s.charAt(di)));
            di--;
        }
    }

    /** (row,col) 0-based 좌표를 엑셀 A1 참조 문자열("D4" 등)로 — SUM 범위 만들 때. */
    private static String ref(int row, int col) {
        return new CellReference(row, col).formatAsString();
    }

    // ===== helpers =====

    private static Client clientOf(Billing b) {
        if (b.getContract() != null) {
            return b.getContract().getClient();
        }
        if (b.getQuote() != null) {
            return b.getQuote().getClient();
        }
        return null;
    }

    /** 청구의 부가세 기준 — 계약의 vatType. 견적 청구/미지정은 기본 EXCLUSIVE(별도). */
    private static VatType vatTypeOf(Billing b) {
        if (b.getContract() != null && b.getContract().getVatType() != null) {
            return b.getContract().getVatType();
        }
        return VatType.EXCLUSIVE;
    }

    /**
     * 청구 금액을 부가세 기준에 따라 [공급가액, 세액]으로 나눈다.
     *   EXCLUSIVE(별도): 청구액=공급가액, 세액=공급가액*10%
     *   INCLUSIVE(포함): 공급가액=청구액/1.1, 세액=청구액-공급가액
     *   FREE(면세):     공급가액=청구액, 세액=0
     *
     * @param amount 청구(또는 수금) 금액
     * @param vt     부가세 기준
     * @return {공급가액, 세액}
     */
    private static long[] splitVat(long amount, VatType vt) {
        switch (vt) {
            case INCLUSIVE:
                long supply = Math.round(amount / 1.1);
                return new long[] { supply, amount - supply };
            case FREE:
                return new long[] { amount, 0L };
            case EXCLUSIVE:
            default:
                return new long[] { amount, Math.round(amount * 0.1) };
        }
    }

    /** 거래처별 누적기(집계용) */
    private static final class Acc {
        private String name;
        private String businessNumber;
        private long supply;
        private long tax;
        private int count;
    }
}
