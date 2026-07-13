package com.boot.cleanhub.util.hwp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import kr.dogfoot.hwplib.object.docinfo.BinData;
import kr.dogfoot.hwplib.reader.HWPReader;
import kr.dogfoot.hwplib.tool.objectfinder.FieldFinder;
import kr.dogfoot.hwplib.tool.objectfinder.SetFieldResult;
import kr.dogfoot.hwplib.writer.HWPWriter;

/**
 * <pre>
 *   HwpMo — hwplib(한글 HWP) 저수준 API 를 감싼 "양식 채우기" 헬퍼.
 *   PoiMo(엑셀)와 같은 역할을 HWP 에서 한다.
 *
 *   [쓰는 방식]
 *     빈 양식(.hwp)에 한글에서 미리 넣어 둔 누름틀(필드)에 이름으로 값을 넣는다.
 *     문서를 코드로 새로 그리지 않으므로 표·선·글꼴 등 양식의 서식이 그대로 보존되고,
 *     양식의 표 구조가 바뀌어도 누름틀 이름만 그대로면 코드를 고칠 필요가 없다.
 *
 *   [무엇을 해 주나]
 *     - open(InputStream)                  : 양식 열기
 *     - setField(name, value) / setFields  : 누름틀에 값 넣기(이름으로 찾음)
 *     - fieldNames()                       : 양식에 들어 있는 누름틀 이름 목록
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
     * @param name  누름틀 이름
     * @param value 넣을 값
     * @return 넣었으면 true, 그 이름의 누름틀이 없으면 false
     * @throws IOException 문자열 처리 실패
     */
    public boolean setField(String name, String value) throws IOException {
        int count = countFields(name);
        if (count == 0) {
            return false;
        }
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(value == null ? "" : value);
        }
        try {
            SetFieldResult result = FieldFinder.setClickHereText(hwpFile, name, new ArrayList<>(values));
            return result == SetFieldResult.SetAllText;
        } catch (Exception e) {
            throw new IOException("누름틀 '" + name + "' 에 값을 넣지 못했습니다.", e);
        }
    }

    /**
     * 여러 누름틀에 값을 한 번에 넣는다.
     *
     * @param values 누름틀 이름 → 값
     * @return 양식에서 찾지 못한 누름틀 이름들(전부 채웠으면 빈 목록)
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
     * 양식에 들어 있는 누름틀 이름 목록(중복 포함, 나온 순서대로).
     * 양식과 코드가 어긋났을 때 원인을 찾는 용도.
     *
     * @return 누름틀 이름 목록
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
        for (String each : fieldNames()) {
            if (each.equals(name)) {
                count++;
            }
        }
        return count;
    }

    /** 문단 목록(표 셀 포함)을 훑어 누름틀 이름을 모은다. */
    private void collectFieldNames(ParagraphListInterface list, List<String> names) {
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
                        collectFieldNames(cell.getParagraphList(), names);
                    }
                }
            }
        }
    }

    // ===== 그림(도장) =====

    /**
     * 문서의 첫 번째 그림(도장 자리)의 이미지 내용을 바꾼다.
     * 그림의 위치·크기는 양식에 지정된 대로 유지되고 그림 내용만 바뀐다.
     * 도장이 글자를 밀어내지 않도록 배치는 "글 앞으로"로 맞춘다.
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
        // 양식의 그림 배치가 "어울림"이면 글자가 도장을 피해가며 줄이 밀린다. 도장은 글자 위에 겹쳐 찍혀야 한다.
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
