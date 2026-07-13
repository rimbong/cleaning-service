package com.boot.cleanhub.biz.contract.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.company.dto.CompanyResponse;
import com.boot.cleanhub.biz.company.service.CompanyService;
import com.boot.cleanhub.biz.contract.domain.Contract;
import com.boot.cleanhub.biz.contract.repository.ContractRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.util.date.DateUtil;
import com.boot.cleanhub.util.format.KoreanNumberMo;
import com.boot.cleanhub.util.hwp.HwpMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   계약서(HWP) 생성 서비스.
 *
 *   원본 계약서와 서식이 완전히 같도록, 문서를 코드로 그리지 않고
 *   빈 양식(templates/contract_resource.hwp)에 한글로 넣어 둔 누름틀에 값만 채운다.
 *   -- 세금계산서(tax_resource.xls) 와 같은 방식이다.
 *
 *   누름틀은 이름으로 찾으므로, 양식의 표 구조가 바뀌어도 이름만 유지되면 이 코드는 그대로 동작한다.
 *   양식을 고쳐서 누름틀 이름이 달라지면 기동 시가 아니라 다운로드 시 경고 로그로 드러난다.
 *
 *   도장은 양식의 그림 자리를 회사정보에 등록된 도장 이미지로 바꿔 찍는다.
 *   도장을 넣지 않을 때는 그 그림을 투명하게 비운다(양식의 자리표시용 그림이 인쇄되지 않게).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.13
 * @version 2.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ContractDocumentService.class);

    /** 계약서 빈 양식(클래스패스) */
    private static final String TEMPLATE_PATH = "templates/contract_resource.hwp";

    /** 양식의 날짜 표기 — "2026 년 7월 13일" */
    private static final DateTimeFormatter CONTRACT_DATE = DateTimeFormatter.ofPattern("yyyy 년 M월 d일");

    /** 계약기간 표기의 시작일 — "2026 년 7월 13 일부터" */
    private static final DateTimeFormatter PERIOD_FROM = DateTimeFormatter.ofPattern("yyyy 년 M월 d 일부터");

    /** 계약기간 표기의 종료일 — "2027 년 7월 12일까지" */
    private static final DateTimeFormatter PERIOD_TO = DateTimeFormatter.ofPattern("yyyy 년 M월 d일까지");

    /** 금액이 없거나 0 일 때의 표기 — 칸을 비워 두지 않고 0원임을 명시한다 */
    private static final String ZERO_FEE = "0 원정, (0 원)";

    private final ContractRepository contractRepository;
    private final CompanyService companyService;

    /**
     * 계약서 HWP 를 만든다.
     *
     * @param contractId 계약 ID
     * @param withStamp  true 면 회사정보에 등록된 도장을 찍는다(등록된 도장이 없으면 안 찍는다)
     * @return HWP 파일 바이트
     * @throws BizException 계약이 없으면 CONTRACT_NOT_FOUND, 양식 처리 실패면 FILE_UPLOAD_FAILED
     */
    public byte[] buildContractDocument(Long contractId, boolean withStamp) {
        Contract contract = contractRepository.findByIdWithClient(contractId)
                .orElseThrow(() -> new BizException(ErrorCode.CONTRACT_NOT_FOUND));
        CompanyResponse company = companyService.get();

        try (InputStream in = new ClassPathResource(TEMPLATE_PATH).getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            HwpMo hwp = HwpMo.open(in);
            fill(hwp, contract, company);
            stamp(hwp, withStamp);
            // 양식을 한글로 편집하면 쓰지 않는 이미지가 남을 수 있다. 파일만 무거워지므로 정리하고 내보낸다.
            hwp.removeOrphanImages();
            hwp.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("계약서 양식 처리 실패 (계약 {})", contractId, e);
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /** 다운로드 파일명 — "계약서_OO빌딩_20261113.hwp" */
    public String buildFileName(Long contractId) {
        Contract contract = contractRepository.findByIdWithClient(contractId)
                .orElseThrow(() -> new BizException(ErrorCode.CONTRACT_NOT_FOUND));
        String clientName = contract.getClient() != null ? contract.getClient().getName() : "계약";
        String date = formatOrEmpty(contract.getStartDate(), DateUtil.YYYYMMDD_FORMATTER);
        return "계약서_" + clientName + (date.isEmpty() ? "" : "_" + date) + ".hwp";
    }

    /**
     * 양식의 누름틀을 채운다.
     *
     * 값이 없는 항목도 빈 문자열로 넣는다 — 안 그러면 양식에 적어 둔 안내문("주소" 등)이 그대로 인쇄된다.
     *
     * @param hwp      양식
     * @param contract 계약
     * @param company  회사정보
     * @throws IOException 문자열 처리 실패
     */
    private void fill(HwpMo hwp, Contract contract, CompanyResponse company) throws IOException {
        Client client = contract.getClient();
        Map<String, String> values = new LinkedHashMap<>();

        // 고객(갑)
        values.put("clientAddress", client != null ? client.getAddress() : null);
        values.put("clientName", client != null ? client.getName() : null);
        values.put("clientPhone", client != null ? client.getManagerPhone() : null);
        values.put("clientOwner", client != null ? client.getRepresentativeName() : null);

        // 용역회사(을)
        values.put("companyBizType", company.getBusinessType());
        values.put("companyName", company.getCompanyName());
        values.put("companyAddress", company.getAddress());
        values.put("companyNamePhone", joinNonEmpty(company.getCompanyName(), company.getPhone()));
        values.put("companyOwner", company.getOwnerName());

        // 청소 내역
        values.put("cleaningScope", cleaningScopeText(contract));
        values.put("serviceItems", contract.getServiceItems());
        values.put("extraServices", contract.getExtraServices());
        // 계약의 memo 는 관리자 내부용이라 고객에게 나가는 계약서에 넣지 않는다. 계약서에 적을 내용은 extraNotes.
        values.put("extraNotes", contract.getExtraNotes());

        // 고객 부담
        values.put("contractPeriod", periodText(contract));
        values.put("initialFee", initialFeeText(contract));
        values.put("monthlyFee", monthlyFeeText(contract));
        values.put("paymentMethod", contract.getPaymentMethod());

        // 서명부
        values.put("contractDate", formatOrEmpty(contract.getStartDate(), CONTRACT_DATE));

        List<String> missing = hwp.setFields(values);
        if (!missing.isEmpty()) {
            // 양식(누름틀 이름)과 코드가 어긋난 상태. 문서는 나가지만 그 칸은 안내문이 그대로 남는다.
            log.warn("계약서 양식에 없는 누름틀 {} — 양식의 누름틀 이름을 확인하세요. (양식에 있는 이름: {})",
                    missing, hwp.fieldNames());
        }
    }

    /**
     * 도장을 찍거나 지운다.
     *
     * 양식에는 도장 자리에 그림 한 개가 자리표시용으로 들어 있다. 그대로 두면 그 그림이 인쇄되므로,
     * 도장을 찍을 때만 회사 도장 이미지로 바꾸고 그 외에는 투명하게 비운다.
     *
     * @param hwp       채우는 중인 문서
     * @param withStamp 도장 포함 요청 여부
     */
    private void stamp(HwpMo hwp, boolean withStamp) {
        byte[] stampImage = withStamp ? companyService.getStampBytes() : null;
        if (stampImage != null) {
            hwp.setPictureImage(stampImage);
        } else {
            hwp.clearPictureImage();
        }
    }

    /** 청소 범위 + 주 N 회 — 예: "지하1층~지상4층 건물내부 (주 5 회)" */
    private String cleaningScopeText(Contract contract) {
        String scope = nullToEmpty(contract.getCleaningScope());
        int days = weekdayCount(contract.getCleaningWeekdays());
        if (days == 0) {
            return scope;
        }
        return (scope.isEmpty() ? "" : scope + " ") + "(주 " + days + " 회)";
    }

    /** "MON,WED,FRI" 형태에서 요일 개수 */
    private int weekdayCount(String weekdays) {
        if (!StringUtils.hasText(weekdays)) {
            return 0;
        }
        return weekdays.split(",").length;
    }

    /** 계약기간 — 종료일이 없으면 무기한 */
    private String periodText(Contract contract) {
        LocalDate start = contract.getStartDate();
        LocalDate end = contract.getEndDate();
        if (start == null) {
            return "";
        }
        String from = formatOrEmpty(start, PERIOD_FROM);
        if (end == null) {
            return from + " (무기한)";
        }
        return from + "  " + formatOrEmpty(end, PERIOD_TO);
    }

    /**
     * 초도청소비 — 안 받는 계약이 많아 값이 비는데, 그렇다고 칸을 비워 두면 계약서가 허전하다.
     * 값이 없거나 0 이면 원본 양식처럼 0원으로 명시한다("일금 영 원정" 은 어색해서 쓰지 않는다).
     */
    private String initialFeeText(Contract contract) {
        Long fee = contract.getInitialFee();
        if (fee == null || fee == 0L) {
            return ZERO_FEE;
        }
        return moneyText(fee);
    }

    /** 금액 — "일금 삼십만 원정, (300,000 원)". 금액이 없으면 빈 문자열 */
    private String moneyText(Long amount) {
        if (amount == null) {
            return "";
        }
        return "일금 " + KoreanNumberMo.toHangul(amount) + " 원정, ("
                + NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + " 원)";
    }

    /** 월 용역비 — 금액 뒤에 청구일을 덧붙인다 */
    private String monthlyFeeText(Contract contract) {
        String money = moneyText(contract.getMonthlyFee());
        if (contract.getBillingDay() == null || money.isEmpty()) {
            return money;
        }
        return money + ", 매월 " + contract.getBillingDay() + " 일";
    }

    /** DateUtil.formatSafe 는 실패 시 null 을 주므로, 양식에 "null" 이 찍히지 않게 빈 문자열로 바꾼다. */
    private String formatOrEmpty(LocalDate date, DateTimeFormatter formatter) {
        return nullToEmpty(DateUtil.formatSafe(date, formatter));
    }

    /** 두 값을 공백으로 잇되 빈 값은 건너뛴다 */
    private String joinNonEmpty(String first, String second) {
        if (!StringUtils.hasText(first)) {
            return nullToEmpty(second);
        }
        if (!StringUtils.hasText(second)) {
            return first;
        }
        return first + " " + second;
    }

    private String nullToEmpty(String value) {
        return (value == null) ? "" : value;
    }
}
