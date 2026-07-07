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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * POI 4.0 ~ 5.x (최신 5.5.x까지) 전용 버전
 */
public class PoiMo {

    private Workbook wb = null;
    private Sheet sheet = null;

    private static final int CELL_WIDTH_KR = 400;
    private static final int CELL_WIDTH_APH = 200;
    private static final int MAX_COLUMN_WIDTH = 255 * 256;

    private static final Map<String, HorizontalAlignment> alignTypeMap = new HashMap<>();
    private static final Map<String, BorderStyle> lineBorderTypeMap = new HashMap<>();
    private static final Map<String, Short> colorMap = new HashMap<>();

    /** ★★★ CellStyle 캐싱 (enum 키로 변경) ★★★ */
    private final Map<CellStyle, Map<HorizontalAlignment, CellStyle>> styleCache = new HashMap<>();
    static {
        alignTypeMap.put("l", HorizontalAlignment.LEFT);
        alignTypeMap.put("c", HorizontalAlignment.CENTER);
        alignTypeMap.put("r", HorizontalAlignment.RIGHT);

        lineBorderTypeMap.put("thin", BorderStyle.THIN);
        lineBorderTypeMap.put("thick", BorderStyle.THICK);
        
        colorMap.put("red", IndexedColors.RED.getIndex());
        colorMap.put("blue", IndexedColors.BLUE.getIndex());
        colorMap.put("yellow", IndexedColors.YELLOW.getIndex());
        colorMap.put("light-yellow", IndexedColors.LIGHT_YELLOW.getIndex());
        colorMap.put("white", IndexedColors.WHITE.getIndex());
        colorMap.put("orange", IndexedColors.LIGHT_ORANGE.getIndex());
    }

    private PoiMo() {}

    // ==================== 생성/열기 (동일) ====================
    public static PoiMo create(String newFilePath) throws IOException {
        PoiMo poi = new PoiMo();
        boolean isXlsx = newFilePath.toLowerCase().endsWith(".xlsx");
        poi.wb = isXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
        poi.createNewSheet("Sheet1");
        return poi;
    }

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

    // ==================== CellStyle 캐싱 (enum 버전) ====================
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

    private String extractCleanData(String rawData) {
        if (rawData == null || !rawData.contains("$")) return rawData != null ? rawData : "";
        return rawData.split("\\$")[0];
    }

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

    private HorizontalAlignment getAlignmentFromCode(String alignCode) {
        if (alignCode == null || alignCode.isEmpty()) return HorizontalAlignment.LEFT;
        String mod = alignCode.toLowerCase().trim();
        return alignTypeMap.getOrDefault(mod, HorizontalAlignment.LEFT);
    }

    // ==================== 데이터 설정 (나머지는 동일) ====================
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

    private void autoAdjustColumnWidth(int col, String text) {
        int width = calculateCellWidth(text);
        if (sheet.getColumnWidth(col) < width) {
            sheet.setColumnWidth(col, width);
        }
    }

    private int calculateCellWidth(String data) {
        if (data == null || data.isEmpty()) return 256 * 3;
        int width = 256;
        for (char c : data.toCharArray()) {
            width += isKorean(c) ? CELL_WIDTH_KR : CELL_WIDTH_APH;
        }
        return Math.min(width, MAX_COLUMN_WIDTH);
    }

    private boolean isKorean(char c) {
        return (c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x3131 && c <= 0x318E);
    }

    // Bulk 메서드들은 완전히 동일 (생략 없이 그대로 복사)
    public void setBulkColsDataFromStringArray(ArrayList<CellStyle> baseStyles, int row, int startCol, String[] data) {
        validateBulkParams(baseStyles, data);
        for (int i = 0; i < data.length; i++) {
            setData(baseStyles.get(i), row, startCol + i, data[i]);
        }
    }

    public void setBulkColsDataFromMap(ArrayList<CellStyle> baseStyles, int row, int startCol, Map<String, Object> map, String[] keys) {
        validateBulkParams(baseStyles, keys);
        for (int i = 0; i < keys.length; i++) {
            String value = String.valueOf(map.getOrDefault(keys[i], ""));
            setData(baseStyles.get(i), row, startCol + i, value);
        }
    }

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

    private void validateBulkParams(ArrayList<CellStyle> styles, Object[] dataOrKeys) {
        if (styles == null || dataOrKeys == null || styles.size() < dataOrKeys.length) {
            throw new IllegalArgumentException("Styles and data/keys length mismatch");
        }
    }

    // ==================== 유틸 메서드 (enum 적용) ====================
    public CellStyle createNewStyle() {
        return wb.createCellStyle();
    }

    public void setBackgroundColor(CellStyle style, String color) {
        if (style == null || color == null) return;
        style.setFillForegroundColor(colorMap.getOrDefault(color.toLowerCase(), IndexedColors.WHITE.getIndex()));  // colorMap은 그대로
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);   // ← 여기만 변경
    }

    // colorMap은 static에 추가 필요 (아래에 추가했습니다)
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

    // createNewSheet, setSheet, write 메서드는 완전히 동일 (생략 없이 복사)
    public Sheet createNewSheet(String sheetName) {
        if (wb == null) throw new IllegalStateException("Workbook is not initialized");
        String name = (sheetName == null || sheetName.trim().isEmpty())
                ? "Sheet" + (wb.getNumberOfSheets() + 1) : sheetName;
        this.sheet = wb.createSheet(name);
        return this.sheet;
    }

    public void setSheet(Sheet sheet) {
        if (sheet == null) throw new IllegalArgumentException("Sheet cannot be null");
        this.sheet = sheet;
    }

    public void write(OutputStream os) throws IOException {
        if (wb == null) throw new IllegalStateException("Workbook is not initialized");
        wb.write(os);
    }
    
    public void close() throws IOException {
        if (wb != null) {
            wb.close();
            wb = null;   
        }
    }
}