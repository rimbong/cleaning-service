package com.boot.cleanhub.util.excel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 *   ExcelObject 
 *   PoiMo를 기반으로 만든 Builder 형태의 Excel 객체
 * </pre>
 * 
 * lib : implementation 'org.apache.poi:poi:4.1.2'
 * @author In-seong Hwang
 * @version 1.0
 */
public class ExcelObject {
    private static final int CELL_WIDTH_KR = 590;
    private static final int CELL_WIDTH_APH = 256;
    private static final int MAX_COLUMN_WIDTH = 255 * 256;
    
    private static final Map<String, HorizontalAlignment> ALIGN_MAP;
    private static final Map<String, IndexedColors> COLOR_MAP;
    private static final Map<String, BorderStyle> BORDER_MAP;

    static {
        // 정렬 맵
        ALIGN_MAP = new HashMap<>();
        ALIGN_MAP.put("l", HorizontalAlignment.LEFT);
        ALIGN_MAP.put("c", HorizontalAlignment.CENTER);
        ALIGN_MAP.put("r", HorizontalAlignment.RIGHT);

        // 색상 맵
        COLOR_MAP = new HashMap<>();
        COLOR_MAP.put("red", IndexedColors.RED);
        COLOR_MAP.put("blue", IndexedColors.BLUE);
        COLOR_MAP.put("yellow", IndexedColors.YELLOW);
        COLOR_MAP.put("light-yellow", IndexedColors.LIGHT_YELLOW);
        COLOR_MAP.put("white", IndexedColors.WHITE);
        COLOR_MAP.put("orange", IndexedColors.LIGHT_ORANGE);
        COLOR_MAP.put("grey-25", IndexedColors.GREY_25_PERCENT);
        COLOR_MAP.put("black", IndexedColors.BLACK);

        // 테두리 스타일 맵
        BORDER_MAP = new HashMap<>();
        BORDER_MAP.put("thin", BorderStyle.THIN);
        BORDER_MAP.put("thick", BorderStyle.THICK);
        BORDER_MAP.put("dotted", BorderStyle.DOTTED);
        BORDER_MAP.put("dashed", BorderStyle.DASHED);
    }

    private final Workbook workbook;

    private ExcelObject(Builder builder) {
        this.workbook = builder.workbook;
    }

    public void write(OutputStream os) throws IOException {
        try {
            workbook.write(os);
        } finally {
            IOUtils.closeQuietly(workbook);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Style style() {
        return new Style();
    }

    // --- 이너클래스 CellValue, Style, Builder ---

    public static class CellValue {
        private final Object value;
        private final String styleName;

        public CellValue(Object value, String styleName) {
            this.value = value;
            this.styleName = styleName;
        }

        public Object getValue() {
            return value;
        }

        public String getStyleName() {
            return styleName;
        }
    }

    public static class Style {
        private String fontName = "Calibri";
        private short fontHeight = 11;
        private boolean fontBold = false;
        private IndexedColors fontColor = null;
        private IndexedColors backgroundColor = null;
        private HorizontalAlignment alignment = HorizontalAlignment.LEFT;
        private BorderStyle border = null;
        private String dataFormat = null;

        public Style font(boolean bold, int height) {
            this.fontBold = bold;
            this.fontHeight = (short) height;
            return this;
        }

        public Style font(boolean bold, int height, IndexedColors color) {
            this.fontBold = bold;
            this.fontHeight = (short) height;
            this.fontColor = color;
            return this;
        }

        /**
         * 문자열 코드로 폰트 색상을 설정합니다.
         * @param colorName 색상 이름
         * @return Style 빌더
         */
        public Style font(boolean bold, int height, String colorName) {
            this.fontBold = bold;
            this.fontHeight = (short) height;
            this.fontColor = ExcelObject.COLOR_MAP.get(colorName.toLowerCase());
            return this;
        }

        public Style background(IndexedColors color) {
            this.backgroundColor = color;
            return this;
        }

        /**
         * 문자열 코드로 배경색을 설정합니다.
         * @param colorName 색상 이름
         * @return Style 빌더
         */
        public Style background(String colorName) {
            this.backgroundColor = ExcelObject.COLOR_MAP.get(colorName.toLowerCase());
            return this;
        }

        public Style align(HorizontalAlignment alignment) {
            this.alignment = alignment;
            return this;
        }

        /**
         * 문자열 코드로 정렬을 설정합니다. ("l", "c", "r")
         * @param alignCode 정렬 코드
         * @return Style 빌더
         */
        public Style align(String alignCode) {
            this.alignment = ExcelObject.ALIGN_MAP.getOrDefault(alignCode.toLowerCase(), HorizontalAlignment.LEFT);
            return this;
        }

        public Style border(BorderStyle border) {
            this.border = border;
            return this;
        }

        /**
         * 문자열 코드로 테두리를 설정합니다. ("thin", "thick", "dotted", ...)
         * @param borderName 테두리 스타일 이름
         * @return Style 빌더
         */
        public Style border(String borderName) {
            this.border = ExcelObject.BORDER_MAP.get(borderName.toLowerCase());
            return this;
        }

        public Style dataFormat(String dataFormat) {
            this.dataFormat = dataFormat;
            return this;
        }

        private CellStyle toCellStyle(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName(fontName);
            font.setFontHeightInPoints(fontHeight);
            font.setBold(fontBold);
            if (fontColor != null) {
                font.setColor(fontColor.getIndex());
            }
            style.setFont(font);

            if (backgroundColor != null) {
                style.setFillForegroundColor(backgroundColor.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }

            style.setAlignment(alignment);
            style.setVerticalAlignment(VerticalAlignment.CENTER);

            if (border != null) {
                style.setBorderTop(border);
                style.setBorderBottom(border);
                style.setBorderLeft(border);
                style.setBorderRight(border);
            }

            if (dataFormat != null) {
                style.setDataFormat(workbook.createDataFormat().getFormat(dataFormat));
            }

            return style;
        }
    }

    public static class Builder {
        private Workbook workbook;
        private String sheetName;
        private boolean isXlsx = true;
        private boolean autoSize = true;
        private List<String> headers;
        private List<Map<String, Object>> data;
        private String[] dataKeys;
        private Map<String, Style> definedStyles = new HashMap<>();
        private Map<Integer, String> columnStyles = new HashMap<>();
        private Map<Integer, String> headerColumnStyles = new HashMap<>(); // <-- 추가
        private String headerStyleName;
        private String bodyStyleName;

        public Builder() {
            this.sheetName = "default";
        }
        
        public Builder sheetName(String sheetName) {
            this.sheetName = sheetName;
            return this;
        }
        public Builder headers(List<String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder data(List<Map<String, Object>> data, String[] dataKeys) {
            this.data = data;
            this.dataKeys = dataKeys;
            return this;
        }

        public Builder useXlsFormat() {
            this.isXlsx = false;
            return this;
        }

        public Builder autoSize(boolean autoSize) {
            this.autoSize = autoSize;
            return this;
        }

        public Builder defineStyle(String name, Style style) {
            this.definedStyles.put(name, style);
            return this;
        }

        public Builder setHeaderStyle(String styleName) {
            this.headerStyleName = styleName;
            return this;
        }

        public Builder setBodyStyle(String styleName) {
            this.bodyStyleName = styleName;
            return this;
        }
        
        /**
         * 특정 헤더 컬럼에만 적용할 스타일을 지정합니다.
         * @param columnIndex 스타일을 적용할 0-based 컬럼 인덱스
         * @param styleName   미리 defineStyle로 정의된 스타일 이름
         * @return Builder
         */
        public Builder setHeaderColumnStyle(int columnIndex, String styleName) { // <-- 추가
            this.headerColumnStyles.put(columnIndex, styleName);
            return this;
        }

        public Builder setColumnStyle(int columnIndex, String styleName) {
            this.columnStyles.put(columnIndex, styleName);
            return this;
        }

        /**
         * 글자수에 따른 열 너비 계산. 한글 590, 알파뉴메릭 256 단위.
         * TODO: sheet.autoSizeColumn(col) 사용 고려 (POI 최신 버전 성능 개선).
         */
        private int calculateCellWidth(String data) {
            int cellWidth = 256;
            if (data != null && !data.isEmpty()) {
                for (char c : data.toCharArray()) {
                    cellWidth += isKorean(c) ? CELL_WIDTH_KR : CELL_WIDTH_APH;
                }
            }
            return Math.min(cellWidth, MAX_COLUMN_WIDTH);
        }

        /**
         * 한글 문자인지 확인.
         */
        private boolean isKorean(char c) {
            return (c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x3131 && c <= 0x318E);
        }

        private void setCellValue(Cell cell, Object value) {
            if (value == null) {
                cell.setCellValue("");
            } else if (value instanceof String) {
                cell.setCellValue((String) value);
            } else if (value instanceof Integer) {
                cell.setCellValue((Integer) value);
            } else if (value instanceof Long) {
                cell.setCellValue((Long) value);
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            } else {
                cell.setCellValue(value.toString());
            }
        }

        public ExcelObject build() {
            this.workbook = this.isXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
            Sheet sheet = workbook.createSheet(this.sheetName);

            // 1. 스타일 맵 생성
            Map<String, CellStyle> styleMap = new HashMap<>();
            for (Map.Entry<String, Style> entry : this.definedStyles.entrySet()) {
                styleMap.put(entry.getKey(), entry.getValue().toCellStyle(workbook));
            }

            // 2. 헤더 생성
            int currentRowNum = 0;
            if (this.headers != null && !this.headers.isEmpty()) {
                Row headerRow = sheet.createRow(currentRowNum++);
                CellStyle globalHeaderStyle = styleMap.get(this.headerStyleName); // 전체 헤더 스타일

                for (int i = 0; i < this.headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(this.headers.get(i));

                    // 특정 컬럼에 지정된 스타일이 있으면 그것을 사용, 없으면 전체 헤더 스타일 사용
                    String columnSpecificStyleName = this.headerColumnStyles.get(i);
                    CellStyle finalHeaderStyle = styleMap.get(columnSpecificStyleName);

                    if (finalHeaderStyle != null) {
                        cell.setCellStyle(finalHeaderStyle);
                    } else if (globalHeaderStyle != null) {
                        cell.setCellStyle(globalHeaderStyle);
                    }
                }
            }

            // 3. 데이터 채우기
            if (this.data != null && this.dataKeys != null && !this.data.isEmpty()) {
                CellStyle defaultBodyStyle = styleMap.getOrDefault(this.bodyStyleName, new Style().toCellStyle(workbook));

                for (Map<String, Object> dataMap : this.data) {
                    // 행 (row)
                    Row dataRow = sheet.createRow(currentRowNum++);
                    for (int i = 0; i < this.dataKeys.length; i++) {
                        // 열 (cell)
                        Cell cell = dataRow.createCell(i);
                        Object value = dataMap.get(this.dataKeys[i]);

                        CellStyle finalStyle;
                        Object finalValue;

                        if (value instanceof CellValue) {
                            CellValue cellValue = (CellValue) value;
                            finalValue = cellValue.getValue();
                            finalStyle = styleMap.getOrDefault(cellValue.getStyleName(), defaultBodyStyle);
                        } else {
                            finalValue = value;
                            finalStyle = styleMap.getOrDefault(this.columnStyles.get(i), defaultBodyStyle);
                        }

                        setCellValue(cell, finalValue);
                        cell.setCellStyle(finalStyle);

                        // 컬럼 너비 자동 조절
                        if (this.autoSize) {
                            int cellWidth = calculateCellWidth(finalValue != null ? finalValue.toString() : "");
                            int currentWidth = sheet.getColumnWidth(i);
                            if (currentWidth < cellWidth) {
                                sheet.setColumnWidth(i, cellWidth);
                            }
                        }
                    }
                }
            }
            return new ExcelObject(this);
        }
    }
}
