package com.boot.cleanhub.util.hwp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bindata.EmbeddedBinaryData;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlField;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.TextFlowMethod;
import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPicture;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControlType;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.CharPositionShapeIdPair;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.docinfo.BinData;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.objectfinder.CellFinder;
import kr.dogfoot.hwplib.tool.objectfinder.FieldFinder;
import kr.dogfoot.hwplib.tool.objectfinder.SetFieldResult;
import kr.dogfoot.hwplib.tool.paragraphadder.ParaTextSetter;
import kr.dogfoot.hwplib.writer.HWPWriter;

/**
 * <pre>
 *   HwpMo — hwplib(한글 HWP) 저수준 API 를 감싼 "양식 채우기" 헬퍼.
 *   PoiMo(엑셀)와 같은 역할을 HWP 에서 한다.
 *
 *   [쓰는 방식]
 *     빈 양식(.hwp)에 한글에서 미리 만들어 둔 "값 자리"를 이름으로 찾아 값을 넣는다.
 *     문서를 코드로 새로 그리지 않으므로 표·선·글꼴 등 양식의 서식이 그대로 보존되고,
 *     양식의 표 구조가 바뀌어도 이름만 그대로면 코드를 고칠 필요가 없다.
 *
 *   [값 자리 만드는 방법 — 둘 다 지원한다]
 *     1) 누름틀   : 한글의 "입력 > 누름틀". 표 안이든 밖이든 넣을 수 있다.
 *     2) 셀 필드명 : 표 셀 속성의 "필드 이름". 표 안에서만 쓸 수 있다.
 *     양식 만드는 쪽이 어느 방법을 썼든 같은 이름으로 값이 들어간다.
 *
 *   [무엇을 해 주나]
 *     - open(InputStream)                  : 양식 열기
 *     - setField(name, value) / setFields  : 값 넣기(누름틀·셀 필드명 모두)
 *     - fieldNames()                       : 양식에 들어 있는 값 자리 이름 목록
 *     - setPictureImage / clearPictureImage: 도장 그림의 이미지 교체 / 안 보이게
 *     - removeOrphanImages()               : 아무 그림도 안 쓰는 이미지 정리
 *     - write(OutputStream)                : 저장
 *
 *   [주의]
 *     - 누름틀에 값을 넣지 않으면 양식에 적어 둔 안내문이 그대로 인쇄된다.
 *       값이 없는 항목도 빈 문자열로 넣어 안내문을 지운다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.13
 * @version 2.0
 */
public class HwpMo {

    /** 이미지 이름("BIN0001.png")에서 id 가 시작하는 자리 */
    private static final int BIN_NAME_ID_START = 3;
    /** 이미지 이름에서 id 가 끝나는 자리(제외) */
    private static final int BIN_NAME_ID_END = 7;

    private final HWPFile hwpFile;

    private HwpMo(HWPFile hwpFile) {
        this.hwpFile = hwpFile;
    }

    /**
     * HWP 양식을 스트림에서 연다.
     *
     * @param in HWP 입력 스트림(호출자가 닫는다)
     * @return HwpMo 인스턴스
     * @throws IOException 읽기 실패
     */
    public static HwpMo open(InputStream in) throws IOException {
        try {
            return new HwpMo(HWPReader.fromInputStream(in));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("HWP 양식을 열 수 없습니다.", e);
        }
    }

    /** 원본 HWPFile 이 필요할 때(고급 조작용) */
    public HWPFile getHwpFile() {
        return hwpFile;
    }

    // ===== 누름틀(필드) =====

    /**
     * 누름틀에 값을 넣는다. 같은 이름의 누름틀이 여러 개면 모두 같은 값으로 채운다.
     * (하나만 채우면 나머지 누름틀의 안내문이 인쇄물에 그대로 남는다)
     *
     * 값이 null 이거나 비어 있어도 빈 문자열로 넣는다 — 그래야 양식의 안내문이 지워진다.
     *
     * @param name  필드 이름
     * @param value 넣을 값
     * @return 넣었으면 true, 그 이름의 자리가 없으면 false
     * @throws IOException 문자열 처리 실패
     */
    public boolean setField(String name, String value) throws IOException {
        String text = (value == null) ? "" : value;
        boolean filled = setClickHereFields(name, text);
        // 한글에서 값 자리를 만드는 방법이 두 가지다(누름틀 / 표 셀에 필드 이름).
        // 양식 만드는 쪽이 어느 쪽을 썼든 동작하도록 둘 다 채운다.
        filled |= setCellFields(name, text);
        return filled;
    }

    /** 누름틀에 값 넣기. 같은 이름이 여러 개면 모두 채운다. */
    private boolean setClickHereFields(String name, String text) throws IOException {
        int count = countFields(name);
        if (count == 0) {
            return false;
        }
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(text);
        }
        try {
            SetFieldResult result = FieldFinder.setClickHereText(hwpFile, name, new ArrayList<>(values));
            return result == SetFieldResult.SetAllText;
        } catch (Exception e) {
            throw new IOException("누름틀 '" + name + "' 에 값을 넣지 못했습니다.", e);
        }
    }

    /** 표 셀에 붙인 필드 이름으로 찾아 그 칸의 글자를 값으로 바꾼다. */
    private boolean setCellFields(String name, String text) throws IOException {
        List<Cell> cells = CellFinder.findAll(hwpFile, name);
        if (cells.isEmpty()) {
            return false;
        }
        for (Cell cell : cells) {
            setCellText(cell, text);
        }
        return true;
    }

    /** 칸의 첫 문단 글자를 통째로 값으로 바꾼다. 문단끝 같은 제어문자와 원래 글자모양은 유지한다. */
    private void setCellText(Cell cell, String text) throws IOException {
        Paragraph paragraph = cell.getParagraphList().getParagraph(0);
        if (paragraph.getText() == null) {
            paragraph.createText();
        }
        if (paragraph.getCharShape() == null) {
            paragraph.createCharShape();
        }
        if (paragraph.getCharShape().getPositonShapeIdPairList().isEmpty()) {
            paragraph.getCharShape().addParaCharShape(0, 0);
        }

        int lastNormal = -1;
        List<HWPChar> chars = paragraph.getText().getCharList();
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i).getType() == HWPCharType.Normal) {
                lastNormal = i;
            }
        }

        if (lastNormal < 0) {
            // 원래 글자가 없던 칸 — 맨 앞에 넣는다.
            try {
                paragraph.getText().insertString(0, text);
            } catch (UnsupportedEncodingException e) {
                throw new IOException("셀 필드 '" + text + "' 처리에 실패했습니다.", e);
            }
            paragraph.deleteLineSeg();
            return;
        }

        int shapeId = shapeIdAt(paragraph, 0);
        ParaTextSetter.changeText(paragraph, 0, lastNormal, text);
        // changeText 는 바꾼 구간의 글자모양 지정을 지운다. 값이 원래 글꼴을 잃지 않도록 다시 심는다.
        // 글자모양 목록은 위치 오름차순이어야 하므로 맨 앞에 끼워 넣는다(addParaCharShape 는 뒤에 붙인다).
        if (!text.isEmpty() && shapeId >= 0) {
            CharPositionShapeIdPair pair = new CharPositionShapeIdPair();
            pair.setPosition(0);
            pair.setShapeId(shapeId);
            paragraph.getCharShape().getPositonShapeIdPairList().add(0, pair);
        }
    }

    /** position 위치에 적용되는 글자모양 ID(그 위치 이하의 마지막 지정값). 없으면 -1 */
    private int shapeIdAt(Paragraph paragraph, int position) {
        int shapeId = -1;
        for (CharPositionShapeIdPair pair : paragraph.getCharShape().getPositonShapeIdPairList()) {
            if (pair.getPosition() <= position) {
                shapeId = (int) pair.getShapeId();
            } else {
                break;
            }
        }
        return shapeId;
    }

    /**
     * 여러 필드에 값을 한 번에 넣는다.
     *
     * @param values 필드 이름 → 값
     * @return 양식에서 찾지 못한 이름들(전부 채웠으면 빈 목록)
     * @throws IOException 문자열 처리 실패
     */
    public List<String> setFields(Map<String, String> values) throws IOException {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!setField(entry.getKey(), entry.getValue())) {
                missing.add(entry.getKey());
            }
        }
        return missing;
    }

    /**
     * 양식에 들어 있는 값 자리(누름틀 + 표 셀 필드) 이름 목록(중복 포함, 나온 순서대로).
     * 양식과 코드가 어긋났을 때 원인을 찾는 용도.
     *
     * @return 필드 이름 목록
     */
    public List<String> fieldNames() {
        List<String> names = new ArrayList<>();
        for (Section section : hwpFile.getBodyText().getSectionList()) {
            collectFieldNames(section, names);
        }
        return names;
    }

    /** 같은 이름의 누름틀이 몇 개인지 */
    private int countFields(String name) {
        int count = 0;
        for (Section section : hwpFile.getBodyText().getSectionList()) {
            List<String> names = new ArrayList<>();
            collectClickHereNames(section, names);
            for (String each : names) {
                if (each.equals(name)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 문단 목록(표 셀 포함)을 훑어 누름틀 이름 + 셀에 붙은 필드 이름을 모두 모은다. */
    private void collectFieldNames(ParagraphListInterface list, List<String> names) {
        collectClickHereNames(list, names);
        collectCellFieldNames(list, names);
    }

    /** 누름틀 이름만 모은다. */
    private void collectClickHereNames(ParagraphListInterface list, List<String> names) {
        for (Paragraph paragraph : list.getParagraphs()) {
            if (paragraph.getControlList() == null) {
                continue;
            }
            for (Control control : paragraph.getControlList()) {
                if (control.getType() == ControlType.FIELD_CLICKHERE && control instanceof ControlField) {
                    names.add(((ControlField) control).getName());
                }
                if (control.getType() != ControlType.Table) {
                    continue;
                }
                for (Row row : ((ControlTable) control).getRowList()) {
                    for (Cell cell : row.getCellList()) {
                        collectClickHereNames(cell.getParagraphList(), names);
                    }
                }
            }
        }
    }

    /** 표 셀에 붙은 필드 이름을 모은다. */
    private void collectCellFieldNames(ParagraphListInterface list, List<String> names) {
        for (Paragraph paragraph : list.getParagraphs()) {
            if (paragraph.getControlList() == null) {
                continue;
            }
            for (Control control : paragraph.getControlList()) {
                if (control.getType() != ControlType.Table) {
                    continue;
                }
                for (Row row : ((ControlTable) control).getRowList()) {
                    for (Cell cell : row.getCellList()) {
                        String name = cell.getListHeader().getFieldName();
                        if (name != null && !name.isEmpty()) {
                            names.add(name);
                        }
                        collectCellFieldNames(cell.getParagraphList(), names);
                    }
                }
            }
        }
    }

    // ===== 그림(도장) =====

    /**
     * 문서의 첫 번째 그림(도장 자리)의 이미지 내용을 바꾼다.
     * 그림의 위치·크기는 양식에 지정된 대로 유지되고 그림 내용만 바뀐다.
     * 도장이 글자를 밀어내지 않도록 본문과의 배치를 InFrontOfText(글자 위에 겹쳐 그림)로 맞춘다.
     *
     * @param imageBytes 새 이미지 바이트(PNG 등). 비어 있으면 아무것도 하지 않는다.
     * @return 바꿨으면 true
     */
    public boolean setPictureImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return false;
        }
        ControlPicture picture = findFirstPicture();
        if (picture == null) {
            return false;
        }
        EmbeddedBinaryData target =
                findBinaryData(picture.getShapeComponentPicture().getPictureInfo().getBinItemID());
        if (target == null) {
            return false;
        }
        target.setData(imageBytes);
        // 배치가 FitWithText 면 글자가 그림을 피해 흐르면서 줄이 밀린다(도장 자리에서 글자 간격이 벌어짐).
        // 도장은 글자를 밀지 않고 그 위에 겹쳐 찍혀야 하므로 InFrontOfText 로 바꾼다.
        picture.getHeader().getProperty().setTextFlowMethod(TextFlowMethod.InFrontOfText);
        picture.getHeader().getProperty().setLikeWord(false);
        picture.getHeader().getProperty().setAllowOverlap(true);
        return true;
    }

    /**
     * 문서의 첫 번째 그림을 보이지 않게 한다(1x1 투명 PNG 로 교체).
     * 그림 틀은 남지만 아무것도 찍히지 않는다.
     *
     * @return 비웠으면 true
     */
    public boolean clearPictureImage() {
        return setPictureImage(TransparentPixel.PNG);
    }

    /**
     * 아무 그림도 쓰지 않는 이미지 데이터(고아)를 지운다.
     *
     * 한글에서 그림을 "교체"하면 예전 이미지가 문서에 그대로 남는데, DocInfo 에 등록되지 않아
     * 한글 화면에서는 보이지도 지울 수도 없다. 파일 크기만 잡아먹으므로 여기서 정리한다.
     * DocInfo 의 BinData 레코드가 가리키지 않는 이미지는 어떤 그림도 참조할 수 없으므로 지워도 안전하다.
     *
     * @return 지운 개수
     */
    public int removeOrphanImages() {
        Set<Integer> used = new HashSet<>();
        for (BinData record : hwpFile.getDocInfo().getBinDataList()) {
            used.add(record.getBinDataID());
        }
        List<EmbeddedBinaryData> images = hwpFile.getBinData().getEmbeddedBinaryDataList();
        int before = images.size();
        Iterator<EmbeddedBinaryData> it = images.iterator();
        while (it.hasNext()) {
            if (!used.contains(streamIdOf(it.next()))) {
                it.remove();
            }
        }
        return before - images.size();
    }

    /** 본문·표를 통틀어 첫 번째 그림 컨트롤. 없으면 null */
    private ControlPicture findFirstPicture() {
        for (Section section : hwpFile.getBodyText().getSectionList()) {
            ControlPicture found = findPicture(section);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** 문단 목록(표 셀 포함)에서 그림 컨트롤을 재귀로 찾는다. */
    private ControlPicture findPicture(ParagraphListInterface list) {
        for (Paragraph paragraph : list.getParagraphs()) {
            if (paragraph.getControlList() == null) {
                continue;
            }
            for (Control control : paragraph.getControlList()) {
                if (control.getType() == ControlType.Gso
                        && ((GsoControl) control).getGsoType() == GsoControlType.Picture) {
                    return (ControlPicture) control;
                }
                if (control.getType() != ControlType.Table) {
                    continue;
                }
                for (Row row : ((ControlTable) control).getRowList()) {
                    for (Cell cell : row.getCellList()) {
                        ControlPicture found = findPicture(cell.getParagraphList());
                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 그림이 참조하는 실제 이미지 데이터를 찾는다.
     *
     * 주의: BinItemID 는 스트림 번호가 아니라 DocInfo 의 BinData 레코드 순번(1부터)이다.
     * 그 레코드에 들어 있는 binDataID 가 진짜 스트림 번호(BIN0002.png 의 2)다.
     * 이름만 보고 "BIN0001" 을 고르면 엉뚱한 이미지를 바꾸게 된다.
     *
     * @param binItemId 그림의 BinItemID
     * @return 해당 이미지. 없으면 null
     */
    private EmbeddedBinaryData findBinaryData(int binItemId) {
        List<BinData> records = hwpFile.getDocInfo().getBinDataList();
        if (binItemId < 1 || binItemId > records.size()) {
            return null;
        }
        int streamId = records.get(binItemId - 1).getBinDataID();
        for (EmbeddedBinaryData data : hwpFile.getBinData().getEmbeddedBinaryDataList()) {
            if (streamIdOf(data) == streamId) {
                return data;
            }
        }
        return null;
    }

    /** "BIN0002.png" -> 2. 이름 형식이 다르면 -1 */
    private int streamIdOf(EmbeddedBinaryData data) {
        String name = data.getName();
        if (name == null || name.length() < BIN_NAME_ID_END) {
            return -1;
        }
        try {
            return Integer.parseInt(name.substring(BIN_NAME_ID_START, BIN_NAME_ID_END), 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ===== 저장 =====

    /**
     * 결과 HWP 를 스트림에 쓴다.
     *
     * @param out 출력 스트림(호출자가 닫는다)
     * @throws IOException 쓰기 실패
     */
    public void write(OutputStream out) throws IOException {
        try {
            HWPWriter.toStream(hwpFile, out);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("HWP 문서를 저장할 수 없습니다.", e);
        }
    }
}
