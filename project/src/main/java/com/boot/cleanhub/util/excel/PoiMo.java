package com.boot.cleanhub.util.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * <pre>
 *   PoiMo — Apache POI(엑셀) 저수준 API 를 감싼 엑셀 생성/편집 헬퍼.
 *   POI 4.0 ~ 5.5.x 호환. .xlsx(XSSF) 와 .xls(HSSF) 를 파일 확장자로 자동 구분한다.
 *
 *   [무엇을 해 주나]
 *     - 셀에 값 넣기(setData) + 스타일 적용을 한 번에
 *     - 여러 셀 병합(setMergedData) — 제목 행, 세로 표머리 등
 *     - 열 너비: 값에 맞춰 자동 조정(한글은 넓게) 또는 고정 지정(setColumnWidth)
 *     - 스타일 만들기: 폰트/배경색/테두리(문자열 코드로 간단히)
 *     - 새/기존 통합문서 열기, 시트 생성, 출력(write)/닫기(close)
 *
 *   [핵심 개념 1) "$" 정렬 수식어]
 *     setData/setMergedData 에 넘기는 "값" 문자열 끝에 "$c/$r/$l" 을 붙이면
 *     각각 가운데/오른쪽/왼쪽 정렬이 된다. 값과 정렬을 한 문자열로 표현하는 관례다.
 *       예) "합계$c" → "합계"(가운데),  "1,200$r" → "1,200"(오른쪽),  "비고" → 왼쪽(기본)
 *     화면 값에 실제 '$' 문자가 들어가면 정렬 수식어로 오인될 수 있으니 주의.
 *
 *   [핵심 개념 2) CellStyle 캐싱]
 *     POI 는 통합문서당 CellStyle 개수 상한(.xls 4000여 개)이 있어, 셀마다 새 스타일을
 *     만들면 한도를 넘거나 파일이 커진다. 여기서는 (기준 스타일 + 정렬) 조합별로 만든
 *     CellStyle 을 styleCache 에 담아 재사용한다. → 스타일은 몇 개만 만들어 여러 셀에 공유.
 *
 *   [핵심 개념 3) 한글 인식 자동 열너비]
 *     setData 로 값을 넣을 때, 한글은 알파벳/숫자보다 넓게 폭을 계산해 열을 자동 확장한다
 *     (헤드리스 환경에서 autoSizeColumn 이 한글 폭을 작게 재는 문제 회피). 이미 더 넓으면 유지.
 *     ※ 병합(setMergedData)·고정 폭(setColumnWidth)은 자동 조정 대상이 아니다.
 *
 *   [사용 흐름]
 *     PoiMo poi = PoiMo.create("x.xlsx");            // 확장자로 xlsx/xls 결정
 *     CellStyle head = poi.createNewStyle();
 *     poi.setFontStyle(head, "맑은 고딕", (short) 11, "bold", false, false);
 *     poi.setBackgroundColor(head, "light-yellow");
 *     poi.setLineBorder(head, "thin");
 *     poi.setData(head, 0, 0, "제목$c");
 *     try (OutputStream os = ...) { poi.write(os); } finally { poi.close(); }
 *
 *   [주의]
 *     - setData 는 String(또는 RichText)만 받는다. 숫자는 미리 포맷(예: String.format("%,d", v)).
 *     - 스레드 안전하지 않다(하나의 요청/작업에서 지역적으로 생성해 사용).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2025.08
 * @version 1.1 (2026.07 — 기능 분석 기반 주석 보강. 병합/고정 열너비 메서드 추가분 문서화)
 */
public class PoiMo {

    /** 대상 통합문서(엑셀 파일 전체). create/open 이 채운다. */
    private Workbook wb = null;
    /** 현재 작업 대상 시트. setData 등은 이 시트에 쓴다. */
    private Sheet sheet = null;

    /** 자동 열너비 계산 시 한글 1글자에 더하는 폭(1/256 문자폭 단위). 한글은 넓어 크게 잡는다. */
    private static final int CELL_WIDTH_KR = 400;
    /** 자동 열너비 계산 시 그 외(알파벳/숫자/기호) 1글자에 더하는 폭. */
    private static final int CELL_WIDTH_APH = 200;
    /** POI 열 너비 상한(255문자). 이 값을 넘겨 지정하면 예외가 나므로 캡으로 쓴다. */
    private static final int MAX_COLUMN_WIDTH = 255 * 256;

    /** 정렬 수식어 코드(l/c/r) → POI 가로 정렬 매핑. */
    private static final Map<String, HorizontalAlignment> alignTypeMap = new HashMap<>();
    /** 테두리 코드(thin/thick) → POI 테두리 스타일 매핑. */
    private static final Map<String, BorderStyle> lineBorderTypeMap = new HashMap<>();
    /** 색상 코드(red/blue/…) → POI 인덱스 색상 매핑. */
    private static final Map<String, Short> colorMap = new HashMap<>();

    /**
     * CellStyle 캐시 — (기준 스타일) → (정렬 → 그 정렬이 적용된 CellStyle).
     * 같은 조합을 재사용해 CellStyle 남발(개수 상한 초과)을 막는다.
     */
    private final Map<CellStyle, Map<HorizontalAlignment, CellStyle>> styleCache = new HashMap<>();

    static {
        // 정렬 코드
        alignTypeMap.put("l", HorizontalAlignment.LEFT);
        alignTypeMap.put("c", HorizontalAlignment.CENTER);
        alignTypeMap.put("r", HorizontalAlignment.RIGHT);

        // 테두리 코드
        lineBorderTypeMap.put("thin", BorderStyle.THIN);
        lineBorderTypeMap.put("thick", BorderStyle.THICK);

        // 색상 코드
        colorMap.put("red", IndexedColors.RED.getIndex());
        colorMap.put("blue", IndexedColors.BLUE.getIndex());
        colorMap.put("yellow", IndexedColors.YELLOW.getIndex());
        colorMap.put("light-yellow", IndexedColors.LIGHT_YELLOW.getIndex());
        colorMap.put("white", IndexedColors.WHITE.getIndex());
        colorMap.put("orange", IndexedColors.LIGHT_ORANGE.getIndex());
    }

    /** 외부 생성 금지 — 정적 팩토리(create/open)로만 만든다. */
    private PoiMo() {}

    // ==================== 생성 / 열기 ====================

    /**
     * 빈 통합문서를 새로 만든다. 파일 확장자로 형식을 정한다(.xlsx=XSSF, 그 외=HSSF/.xls).
     * 파일을 디스크에 만들지는 않는다 — 경로 문자열은 확장자 판별에만 쓰고, 저장은 write() 로 한다.
     * 기본 시트 "Sheet1" 이 생성되어 바로 작업할 수 있다.
     *
     * @param newFilePath 확장자 판별용 파일명(예: "report.xlsx"). 실제 파일은 안 만든다.
     * @return 새 통합문서를 담은 PoiMo
     * @throws IOException 통합문서 생성 실패 시
     */
    public static PoiMo create(String newFilePath) throws IOException {
        PoiMo poi = new PoiMo();
        boolean isXlsx = newFilePath.toLowerCase().endsWith(".xlsx");
        poi.wb = isXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
        poi.createNewSheet("Sheet1");
        return poi;
    }

    /**
     * 기존 엑셀 파일을 열어 편집 대상으로 삼는다(양식 템플릿에 값만 채우는 용도 등).
     * 확장자로 형식을 정하고, 첫 번째 시트를 작업 대상으로 잡는다.
     *
     * @param existingFilePath 열 파일 경로(실제 존재해야 함)
     * @return 그 파일을 담은 PoiMo
     * @throws IOException 파일이 없거나 읽기 실패 시
     */
    public static PoiMo open(String existingFilePath) throws IOException {
        File file = new File(existingFilePath);
        if (!file.exists()) throw new IOException("File not found: " + existingFilePath);

        PoiMo poi = new PoiMo();
        boolean isXlsx = existingFilePath.toLowerCase().endsWith(".xlsx");
        try (FileInputStream fis = new FileInputStream(file)) {
            poi.wb = isXlsx ? new XSSFWorkbook(fis) : new HSSFWorkbook(new POIFSFileSystem(fis));
            if (poi.wb.getNumberOfSheets() > 0) {
                poi.sheet = poi.wb.getSheetAt(0);
            }
        }
        return poi;
    }

    // ==================== 스타일 캐싱 / 정렬 파싱(내부) ====================

    /**
     * (기준 스타일 + 정렬) 조합에 해당하는 CellStyle 을 캐시에서 찾거나 새로 만들어 준다.
     * 기준 스타일이 있으면 그대로 복제한 뒤 정렬만 덮어써 캐시에 보관·재사용한다.
     * 세로 정렬은 항상 가운데. 기준 스타일이 null 이면 정렬만 가진 임시 스타일을 만든다(캐시 안 함).
     *
     * @param baseStyle 폰트/테두리/배경 등이 이미 담긴 기준 스타일(null 허용)
     * @param alignment 적용할 가로 정렬
     * @return 정렬이 반영된 CellStyle(가능하면 캐시된 것)
     */
    private CellStyle getOrCreateStyledCellStyle(CellStyle baseStyle, HorizontalAlignment alignment) {
        if (baseStyle == null) {
            CellStyle style = wb.createCellStyle();
            style.setAlignment(alignment != null ? alignment : HorizontalAlignment.LEFT);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }

        Map<HorizontalAlignment, CellStyle> alignMap = styleCache.computeIfAbsent(baseStyle, k -> new HashMap<>());
        return alignMap.computeIfAbsent(alignment != null ? alignment : HorizontalAlignment.LEFT, k -> {
            CellStyle cloned = wb.createCellStyle();
            cloned.cloneStyleFrom(baseStyle);
            cloned.setAlignment(k);
            cloned.setVerticalAlignment(VerticalAlignment.CENTER);
            return cloned;
        });
    }

    /**
     * "$" 정렬 수식어를 떼고 순수 값만 돌려준다. 예: "합계$c" → "합계", "비고" → "비고".
     *
     * @param rawData 정렬 수식어가 붙었을 수 있는 원본 값
     * @return 정렬 수식어를 제거한 값(null 이면 빈 문자열)
     */
    private String extractCleanData(String rawData) {
        if (rawData == null || !rawData.contains("$")) return rawData != null ? rawData : "";
        return rawData.split("\\$")[0];
    }

    /**
     * "$" 뒤에 붙은 정렬 수식어(c/r/l)를 읽어 가로 정렬을 정한다. 없으면 왼쪽(기본).
     * 예: "1,200$r" → RIGHT, "합계$c" → CENTER.
     *
     * @param rawData 정렬 수식어가 붙었을 수 있는 원본 값
     * @return 파싱한 가로 정렬(기본 LEFT)
     */
    private HorizontalAlignment getAlignmentFromModifiers(String rawData) {
        if (rawData == null || !rawData.contains("$")) return HorizontalAlignment.LEFT;
        String[] parts = rawData.split("\\$");
        for (int i = 1; i < parts.length; i++) {
            String mod = parts[i].toLowerCase().trim();
            if (alignTypeMap.containsKey(mod)) {
                return alignTypeMap.get(mod);
            }
        }
        return HorizontalAlignment.LEFT;
    }

    /**
     * 정렬 코드 문자열("c"/"r"/"l")을 가로 정렬로 변환한다(RichText 오버로드용). 없으면 왼쪽.
     *
     * @param alignCode 정렬 코드
     * @return 가로 정렬(기본 LEFT)
     */
    private HorizontalAlignment getAlignmentFromCode(String alignCode) {
        if (alignCode == null || alignCode.isEmpty()) return HorizontalAlignment.LEFT;
        String mod = alignCode.toLowerCase().trim();
        return alignTypeMap.getOrDefault(mod, HorizontalAlignment.LEFT);
    }

    // ==================== 데이터 설정 ====================

    /**
     * 한 셀에 값 + 스타일을 넣는다. 값 끝의 "$c/$r/$l" 로 정렬을 지정할 수 있다.
     * 값 길이에 맞춰(한글은 넓게) 열 너비를 자동으로 넓힌다(이미 더 넓으면 유지).
     * 대상 행/셀이 없으면 생성한다.
     *
     * @param baseStyle 적용할 기준 스타일(null 이면 정렬만 가진 기본 스타일)
     * @param row       행 인덱스(0-based)
     * @param col       열 인덱스(0-based)
     * @param data      값(뒤에 $c/$r/$l 로 정렬 지정 가능)
     * @throws IllegalStateException 시트가 없을 때
     */
    public void setData(CellStyle baseStyle, int row, int col, String data) {
        if (sheet == null) throw new IllegalStateException("Sheet is not initialized");

        HorizontalAlignment alignment = getAlignmentFromModifiers(data);
        String cleanData = extractCleanData(data);

        CellStyle finalStyle = getOrCreateStyledCellStyle(baseStyle, alignment);

        Row rowObj = sheet.getRow(row) != null ? sheet.getRow(row) : sheet.createRow(row);
        Cell cell = rowObj.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(cleanData);
        cell.setCellStyle(finalStyle);

        autoAdjustColumnWidth(col, cleanData);
    }

    /**
     * 한 셀에 서식 있는 문자열(RichText) + 스타일을 넣는다. 정렬은 코드("c"/"r"/"l")로 지정.
     * 부분별 폰트/색을 다르게 주는 셀에 쓴다. 열 너비는 텍스트 길이에 맞춰 자동 조정.
     *
     * @param baseStyle 적용할 기준 스타일(null 허용)
     * @param row       행 인덱스(0-based)
     * @param col       열 인덱스(0-based)
     * @param richText  서식 있는 문자열
     * @param alignCode 정렬 코드("c"/"r"/"l", 없으면 왼쪽)
     * @throws IllegalStateException 시트가 없을 때
     */
    public void setData(CellStyle baseStyle, int row, int col, RichTextString richText, String alignCode) {
        if (sheet == null) throw new IllegalStateException("Sheet is not initialized");

        HorizontalAlignment alignment = getAlignmentFromCode(alignCode);
        CellStyle finalStyle = getOrCreateStyledCellStyle(baseStyle, alignment);

        Row rowObj = sheet.getRow(row) != null ? sheet.getRow(row) : sheet.createRow(row);
        Cell cell = rowObj.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(richText);
        cell.setCellStyle(finalStyle);

        autoAdjustColumnWidth(col, richText != null ? richText.toString() : "");
    }

    /**
     * 셀에 숫자 값을 넣는다(스타일 그대로 적용). setData 와 달리 텍스트가 아닌 "숫자"라,
     * SUM 같은 수식의 대상이 되고 셀 서식(#,##0 등)으로 표시된다.
     * 열 너비 자동조정은 하지 않는다(숫자 열 폭은 헤더/서식으로 확보).
     *
     * @param style 적용할 스타일(정렬·테두리·숫자서식 등, null 허용)
     * @param row   행 인덱스(0-based)
     * @param col   열 인덱스(0-based)
     * @param value 숫자 값
     * @throws IllegalStateException 시트가 없을 때
     */
    public void setNumber(CellStyle style, int row, int col, double value) {
        if (sheet == null) throw new IllegalStateException("Sheet is not initialized");
        Row rowObj = sheet.getRow(row) != null ? sheet.getRow(row) : sheet.createRow(row);
        Cell cell = rowObj.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    /**
     * 셀에 수식을 넣는다(맨 앞 '=' 없이, 예: "SUM(D4:D9)"). 실제 값은 엑셀이 파일을 열 때 계산한다
     * (미리 계산된 값을 파일에 넣고 싶으면 write 전에 {@link #evaluateAllFormulas()} 호출).
     * SUM 이 제대로 더하려면 대상 셀들이 텍스트가 아니라 숫자여야 한다(setNumber 로 넣을 것).
     *
     * @param style   적용할 스타일(null 허용)
     * @param row     행 인덱스(0-based)
     * @param col     열 인덱스(0-based)
     * @param formula 수식(맨 앞 '=' 제외)
     * @throws IllegalStateException 시트가 없을 때
     */
    public void setFormula(CellStyle style, int row, int col, String formula) {
        if (sheet == null) throw new IllegalStateException("Sheet is not initialized");
        Row rowObj = sheet.getRow(row) != null ? sheet.getRow(row) : sheet.createRow(row);
        Cell cell = rowObj.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellFormula(formula);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    /**
     * (편의) 한 행 안에서 여러 열을 가로 병합해 값을 쓴다(제목 행 등).
     * 내부적으로 {@link #setMergedData(CellStyle, int, int, int, int, String)} 에 위임한다.
     *
     * @param baseStyle 기준 스타일(null 이면 기본 스타일)
     * @param row       행 인덱스(0-based)
     * @param startCol  시작 열(0-based)
     * @param endCol    끝 열(포함, 0-based)
     * @param data      값(뒤에 $c/$r/$l 로 정렬 지정 가능)
     */
    public void setMergedData(CellStyle baseStyle, int row, int startCol, int endCol, String data) {
        setMergedData(baseStyle, row, row, startCol, endCol, data);
    }

    /**
     * 사각 영역(여러 행 x 여러 열)을 병합하고 값을 쓴다.
     * 제목처럼 가로로만 합칠 수도, 세로 표머리처럼 여러 행을 합칠 수도 있다.
     * 값은 좌상단 셀에만 넣고, 영역 전체에 스타일을 입혀 테두리 등이 끊기지 않게 한다.
     * 열 너비 자동조정은 하지 않는다 — 긴 병합 텍스트가 열 폭을 과도하게 넓히는 것을 막기 위함.
     * 데이터 정렬 수식어($c/$r/$l)를 그대로 지원한다(세로 정렬은 항상 가운데).
     *
     * @param baseStyle 기준 스타일(null 이면 기본 스타일)
     * @param startRow  시작 행(0-based)
     * @param endRow    끝 행(포함, 0-based)
     * @param startCol  시작 열(0-based)
     * @param endCol    끝 열(포함, 0-based)
     * @param data      값(뒤에 $c/$r/$l 로 정렬 지정 가능)
     * @throws IllegalStateException    시트가 없을 때
     * @throws IllegalArgumentException 끝 행/열이 시작보다 작을 때
     */
    public void setMergedData(CellStyle baseStyle, int startRow, int endRow, int startCol, int endCol, String data) {
        if (sheet == null) {
            throw new IllegalStateException("Sheet is not initialized");
        }
        if (endRow < startRow || endCol < startCol) {
            throw new IllegalArgumentException("endRow/endCol must be >= startRow/startCol");
        }

        HorizontalAlignment alignment = getAlignmentFromModifiers(data);
        String cleanData = extractCleanData(data);
        CellStyle finalStyle = getOrCreateStyledCellStyle(baseStyle, alignment);

        // 병합 영역의 모든 셀에 스타일을 입히고(테두리 등 유지), 좌상단 셀에만 값을 넣는다.
        for (int r = startRow; r <= endRow; r++) {
            Row rowObj = sheet.getRow(r) != null ? sheet.getRow(r) : sheet.createRow(r);
            for (int col = startCol; col <= endCol; col++) {
                Cell cell = rowObj.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                cell.setCellStyle(finalStyle);
                if (r == startRow && col == startCol) {
                    cell.setCellValue(cleanData);
                }
            }
        }
        // 실제로 2칸 이상일 때만 병합(1x1 이면 병합 불필요).
        if (endRow > startRow || endCol > startCol) {
            sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, startCol, endCol));
        }
        // 열 너비 자동조정은 의도적으로 생략(병합 텍스트가 열 폭에 영향 주지 않게).
    }

    /**
     * 열 너비를 명시적으로 지정한다(고정 양식용). 단위는 1/256 문자폭.
     * setData 의 자동 조정과 달리 고정 폭을 잡는 용도지만, 이후 setData 로 더 긴 값이 들어오면
     * 자동 조정이 더 넓힐 수 있다(고정 폭을 유지하려면 짧은 값만 넣거나 병합을 쓴다).
     *
     * @param col        열 인덱스(0-based)
     * @param widthChars 문자 단위 너비(예: 3 이면 약 3글자 폭). 상한(255) 초과 시 상한으로 캡.
     * @throws IllegalStateException 시트가 없을 때
     */
    public void setColumnWidth(int col, int widthChars) {
        if (sheet == null) {
            throw new IllegalStateException("Sheet is not initialized");
        }
        int units = Math.min(widthChars * 256, MAX_COLUMN_WIDTH);
        sheet.setColumnWidth(col, units);
    }

    /**
     * 값 길이 기준으로 열 너비를 넓힌다. 계산 폭이 현재 폭보다 클 때만 넓히고(줄이지 않음),
     * setData 계열이 값을 넣은 뒤 호출한다.
     *
     * @param col  열 인덱스(0-based)
     * @param text 방금 넣은 값(폭 계산 기준)
     */
    private void autoAdjustColumnWidth(int col, String text) {
        int width = calculateCellWidth(text);
        if (sheet.getColumnWidth(col) < width) {
            sheet.setColumnWidth(col, width);
        }
    }

    /**
     * 문자열의 표시 폭(1/256 문자폭 단위)을 추정한다. 한글은 넓게(CELL_WIDTH_KR),
     * 그 외는 좁게(CELL_WIDTH_APH) 더하며, 빈 값은 최소 폭, 상한은 MAX_COLUMN_WIDTH.
     *
     * @param data 폭을 잴 문자열
     * @return 추정 폭(POI 열너비 단위)
     */
    private int calculateCellWidth(String data) {
        if (data == null || data.isEmpty()) return 256 * 3;
        int width = 256;
        for (char c : data.toCharArray()) {
            width += isKorean(c) ? CELL_WIDTH_KR : CELL_WIDTH_APH;
        }
        return Math.min(width, MAX_COLUMN_WIDTH);
    }

    /**
     * 한글 음절/자모 범위인지 판별(폭 계산용). 완성형 가(AC00)~힣(D7A3) 또는 호환 자모(3131~318E).
     *
     * @param c 검사할 문자
     * @return 한글이면 true
     */
    private boolean isKorean(char c) {
        return (c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x3131 && c <= 0x318E);
    }

    // ==================== 여러 셀 한 번에(bulk) ====================

    /**
     * 문자열 배열을 한 행의 연속된 열에 순서대로 채운다. 열마다 스타일을 따로 지정한다.
     * 각 값은 "$c/$r/$l" 정렬 수식어를 그대로 지원한다.
     *
     * @param baseStyles 열별 기준 스타일(개수 >= data 길이)
     * @param row        행 인덱스(0-based)
     * @param startCol   시작 열(0-based). 이후 data 순서대로 오른쪽으로 채운다.
     * @param data       채울 값 배열
     * @throws IllegalArgumentException 스타일 개수가 값 개수보다 적을 때
     */
    public void setBulkColsDataFromStringArray(ArrayList<CellStyle> baseStyles, int row, int startCol, String[] data) {
        validateBulkParams(baseStyles, data);
        for (int i = 0; i < data.length; i++) {
            setData(baseStyles.get(i), row, startCol + i, data[i]);
        }
    }

    /**
     * Map 에서 지정한 key 순서대로 값을 꺼내 한 행의 연속된 열에 채운다(누락 key 는 빈 값).
     * DTO 대신 Map 으로 다룬 데이터를 표로 뿌릴 때 쓴다.
     *
     * @param baseStyles 열별 기준 스타일(개수 >= keys 길이)
     * @param row        행 인덱스(0-based)
     * @param startCol   시작 열(0-based)
     * @param map        값이 담긴 Map
     * @param keys       꺼낼 key 순서(= 열 순서)
     * @throws IllegalArgumentException 스타일 개수가 key 개수보다 적을 때
     */
    public void setBulkColsDataFromMap(ArrayList<CellStyle> baseStyles, int row, int startCol, Map<String, Object> map, String[] keys) {
        validateBulkParams(baseStyles, keys);
        for (int i = 0; i < keys.length; i++) {
            String value = String.valueOf(map.getOrDefault(keys[i], ""));
            setData(baseStyles.get(i), row, startCol + i, value);
        }
    }

    /**
     * Map 의 값 중 RichTextString 인 항목만 골라 한 행의 연속된 열에 채운다.
     * 열별 스타일과 정렬 코드(aligns)를 함께 지정한다. RichText 가 아닌 값은 건너뛴다.
     *
     * @param baseStyles 열별 기준 스타일(개수 >= keys 길이)
     * @param row        행 인덱스(0-based)
     * @param startCol   시작 열(0-based)
     * @param map        값이 담긴 Map
     * @param keys       꺼낼 key 순서(= 열 순서)
     * @param aligns     열별 정렬 코드("c"/"r"/"l", 개수 >= keys 길이)
     * @throws IllegalArgumentException 파라미터가 null 이거나 개수가 부족할 때
     */
    public void setBulkColsRichTextFromMap(ArrayList<CellStyle> baseStyles, int row, int startCol,
                                           Map<String, Object> map, String[] keys, String[] aligns) {
        if (baseStyles == null || map == null || keys == null || aligns == null ||
            baseStyles.size() < keys.length || aligns.length < keys.length) {
            throw new IllegalArgumentException("Invalid parameters for RichText bulk");
        }
        for (int i = 0; i < keys.length; i++) {
            Object obj = map.get(keys[i]);
            if (obj instanceof RichTextString) {
                setData(baseStyles.get(i), row, startCol + i, (RichTextString) obj, aligns[i]);
            }
        }
    }

    /**
     * bulk 메서드 공통 검증 — 스타일/데이터가 null 이 아니고 스타일 개수가 데이터 개수 이상인지.
     *
     * @param styles     열별 스타일
     * @param dataOrKeys 값 배열 또는 key 배열
     * @throws IllegalArgumentException 검증 실패 시
     */
    private void validateBulkParams(ArrayList<CellStyle> styles, Object[] dataOrKeys) {
        if (styles == null || dataOrKeys == null || styles.size() < dataOrKeys.length) {
            throw new IllegalArgumentException("Styles and data/keys length mismatch");
        }
    }

    // ==================== 스타일 만들기 ====================

    /**
     * 빈 CellStyle 을 하나 만든다. 여기에 setFontStyle/setBackgroundColor/setLineBorder 로
     * 폰트·배경·테두리를 얹어 기준 스타일로 쓴다(같은 스타일을 여러 셀에 공유 권장).
     *
     * @return 새 CellStyle
     */
    public CellStyle createNewStyle() {
        return wb.createCellStyle();
    }

    /**
     * 스타일에 배경색(단색 채우기)을 지정한다. 색은 문자열 코드(colorMap 의 키)로 준다.
     * 알 수 없는 코드면 흰색으로 처리한다.
     *
     * @param style 대상 스타일(null 이면 무시)
     * @param color 색상 코드(예: "light-yellow", "orange", "red")
     */
    public void setBackgroundColor(CellStyle style, String color) {
        if (style == null || color == null) return;
        style.setFillForegroundColor(colorMap.getOrDefault(color.toLowerCase(), IndexedColors.WHITE.getIndex()));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    /**
     * 스타일에 네 방향 테두리(검정)를 지정한다. 두께는 코드로 준다(모르면 thin).
     *
     * @param style        대상 스타일(null 이면 무시)
     * @param borderWeight 테두리 코드("thin"/"thick", 기본 thin)
     */
    public void setLineBorder(CellStyle style, String borderWeight) {
        if (style == null || borderWeight == null) return;
        BorderStyle border = lineBorderTypeMap.getOrDefault(borderWeight.toLowerCase(), BorderStyle.THIN);
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
        style.setBorderTop(border);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
    }

    /**
     * 스타일에 숫자 표시 형식을 지정한다. 예: "#,##0" → 1,000 처럼 천단위 콤마, "0.00" → 소수 2자리.
     * setNumber 로 넣은 숫자·수식 결과의 "보이는 형식"을 정한다(값 자체는 안 바뀜).
     *
     * @param style   대상 스타일(null 이면 무시)
     * @param pattern 표시 형식(엑셀 서식 코드)
     */
    public void setNumberFormat(CellStyle style, String pattern) {
        if (style == null || pattern == null) return;
        style.setDataFormat(wb.createDataFormat().getFormat(pattern));
    }

    /**
     * 스타일에 가로 정렬을 지정한다(setNumber/setFormula 처럼 "$" 수식어를 못 쓰는 셀용).
     * 세로 정렬은 항상 가운데.
     *
     * @param style 대상 스타일(null 이면 무시)
     * @param code  정렬 코드("l"/"c"/"r", 기본 왼쪽)
     */
    public void setAlign(CellStyle style, String code) {
        if (style == null) return;
        style.setAlignment(alignTypeMap.getOrDefault(code == null ? "" : code.toLowerCase(), HorizontalAlignment.LEFT));
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    /**
     * 스타일에 폰트를 지정한다(이름·크기·굵기·기울임·취소선).
     * 굵기는 "bold" 문자열일 때만 굵게(그 외는 보통).
     *
     * @param style      대상 스타일(null 이면 무시)
     * @param fontName   글꼴 이름(null 이면 무시) — 예: "맑은 고딕"
     * @param fontHeight 글자 크기(pt)
     * @param boldWeight "bold" 면 굵게, 그 외 보통
     * @param italic     기울임 여부
     * @param strikeOut  취소선 여부
     */
    public void setFontStyle(CellStyle style, String fontName, short fontHeight,
                             String boldWeight, boolean italic, boolean strikeOut) {
        if (style == null || fontName == null) return;
        Font font = wb.createFont();
        font.setFontName(fontName);
        font.setFontHeightInPoints(fontHeight);
        font.setBold("bold".equalsIgnoreCase(boldWeight));
        font.setItalic(italic);
        font.setStrikeout(strikeOut);
        style.setFont(font);
    }

    // ==================== 시트 / 출력 / 닫기 ====================

    /**
     * 새 시트를 만들어 작업 대상으로 삼는다. 이름이 비면 "Sheet{개수+1}" 로 자동 명명.
     *
     * @param sheetName 시트 이름(비면 자동)
     * @return 생성된 시트
     * @throws IllegalStateException 통합문서가 없을 때
     */
    public Sheet createNewSheet(String sheetName) {
        if (wb == null) throw new IllegalStateException("Workbook is not initialized");
        String name = (sheetName == null || sheetName.trim().isEmpty())
                ? "Sheet" + (wb.getNumberOfSheets() + 1) : sheetName;
        this.sheet = wb.createSheet(name);
        return this.sheet;
    }

    /**
     * 작업 대상 시트를 교체한다(open 으로 연 파일의 다른 시트를 다룰 때 등).
     *
     * @param sheet 대상 시트(null 금지)
     * @throws IllegalArgumentException sheet 가 null 일 때
     */
    public void setSheet(Sheet sheet) {
        if (sheet == null) throw new IllegalArgumentException("Sheet cannot be null");
        this.sheet = sheet;
    }

    /**
     * 모든 수식 셀을 지금 계산해 결과를 파일에 캐시한다. write 직전에 호출하면,
     * 수식을 다시 계산하지 않는 뷰어(미리보기·일부 라이브러리)에서도 합계 값이 바로 보인다.
     * (엑셀·구글시트 등은 파일을 열 때 어차피 재계산하므로 없어도 화면은 맞다.)
     */
    public void evaluateAllFormulas() {
        if (wb != null) {
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
        }
    }

    /**
     * 통합문서를 출력 스트림에 쓴다(파일/HTTP 응답 등). 스트림은 호출자가 닫는다.
     *
     * @param os 출력 스트림
     * @throws IOException           쓰기 실패 시
     * @throws IllegalStateException 통합문서가 없을 때
     */
    public void write(OutputStream os) throws IOException {
        if (wb == null) throw new IllegalStateException("Workbook is not initialized");
        wb.write(os);
    }

    /**
     * 통합문서를 닫아 리소스를 해제한다. write 후 반드시 호출(보통 finally 에서).
     *
     * @throws IOException 닫기 실패 시
     */
    public void close() throws IOException {
        if (wb != null) {
            wb.close();
            wb = null;
        }
    }
}
