package com.boot.cleanhub.biz.contract.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.client.repository.ClientRepository;
import com.boot.cleanhub.common.dto.PageResponse;
import com.boot.cleanhub.biz.contract.domain.CleaningCycle;
import com.boot.cleanhub.biz.contract.domain.Contract;
import com.boot.cleanhub.biz.contract.domain.ContractStatus;
import com.boot.cleanhub.biz.contract.domain.VatType;
import com.boot.cleanhub.biz.contract.dto.ContractRequest;
import com.boot.cleanhub.biz.contract.dto.ContractResponse;
import com.boot.cleanhub.biz.contract.dto.ScheduleDay;
import com.boot.cleanhub.biz.contract.dto.ScheduleItem;
import com.boot.cleanhub.biz.contract.dto.WeeklyScheduleResponse;
import com.boot.cleanhub.biz.contract.repository.ContractRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   계약 도메인 서비스 — 등록/조회/수정/삭제.
 *   조회는 읽기 전용 트랜잭션, 변경은 쓰기 트랜잭션으로 구분한다.
 *   계약은 거래처(Client)를 참조하므로 등록/수정 시 거래처 존재를 확인한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final ContractAttachmentService attachmentService;

    /**
     * 계약 목록 조회(페이징).
     * clientId 가 있으면 해당 거래처의 계약만, keyword 가 있으면 계약명 검색, 둘 다 없으면 전체.
     *
     * @param keyword  계약명 검색어(선택)
     * @param clientId 거래처 필터(선택)
     * @param pageable 페이지 요청
     * @return 최신 등록순 계약 페이지
     */
    public PageResponse<ContractResponse> list(String keyword, Long clientId, Pageable pageable) {
        Page<Contract> contracts;
        if (clientId != null) {
            contracts = contractRepository.findByClientId(clientId, pageable);
        } else if (StringUtils.hasText(keyword)) {
            contracts = contractRepository.searchByTitle(keyword.trim(), pageable);
        } else {
            contracts = contractRepository.findAllWithClient(pageable);
        }
        return PageResponse.from(contracts.map(ContractResponse::from));
    }

    /** 요일 코드(월~일) — 스케줄 정렬/표시 순서. */
    private static final String[] WEEKDAY_CODES = { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };
    private static final String[] WEEKDAY_LABELS = { "월", "화", "수", "목", "금", "토", "일" };

    /**
     * 주간 청소 스케줄 — 진행 중(ACTIVE) 계약을 청소 요일별로 분류.
     * 한 계약이 여러 요일(월수금 등)이면 각 요일에 모두 들어간다.
     *
     * @return 월~일 각 요일의 청소 대상 거래처 목록
     */
    public WeeklyScheduleResponse getWeeklySchedule() {
        Map<String, List<ScheduleItem>> byDay = new LinkedHashMap<>();
        for (String code : WEEKDAY_CODES) {
            byDay.put(code, new ArrayList<>());
        }
        for (Contract c : contractRepository.findActiveWithClient()) {
            String weekdays = c.getCleaningWeekdays();
            if (weekdays == null || weekdays.trim().isEmpty()) {
                continue;
            }
            for (String code : weekdays.split(",")) {
                List<ScheduleItem> list = byDay.get(code.trim());
                if (list != null) {
                    list.add(ScheduleItem.of(c));
                }
            }
        }
        List<ScheduleDay> days = new ArrayList<>();
        for (int i = 0; i < WEEKDAY_CODES.length; i++) {
            days.add(new ScheduleDay(WEEKDAY_CODES[i], WEEKDAY_LABELS[i], byDay.get(WEEKDAY_CODES[i])));
        }
        return new WeeklyScheduleResponse(days);
    }

    /**
     * 계약 단건 조회.
     *
     * @param id 계약 ID
     * @return 계약 응답
     * @throws BizException 존재하지 않으면 CONTRACT_NOT_FOUND
     */
    public ContractResponse get(Long id) {
        return ContractResponse.from(findOrThrow(id));
    }

    /**
     * 계약 등록.
     *
     * @param request 등록 요청
     * @return 생성된 계약 응답
     * @throws BizException 거래처가 없으면 CLIENT_NOT_FOUND
     */
    @Transactional
    public ContractResponse create(ContractRequest request) {
        Contract contract = new Contract();
        apply(contract, request);
        return ContractResponse.from(contractRepository.save(contract));
    }

    /**
     * 계약 수정.
     *
     * @param id      계약 ID
     * @param request 수정 요청
     * @return 수정된 계약 응답
     * @throws BizException 계약이 없으면 CONTRACT_NOT_FOUND, 거래처가 없으면 CLIENT_NOT_FOUND
     */
    @Transactional
    public ContractResponse update(Long id, ContractRequest request) {
        Contract contract = findOrThrow(id);
        apply(contract, request);
        // flush 로 @PreUpdate(updatedAt 갱신)를 먼저 반영한 뒤 DTO 생성 — 응답의 updatedAt 이 이번 수정 시각이 되게.
        contractRepository.saveAndFlush(contract);
        return ContractResponse.from(contract);
    }

    /**
     * 계약 삭제.
     * 첨부 파일은 파일시스템에 있어 DB FK cascade 로 지워지지 않으므로, 물리 파일과 첨부 행을 먼저 정리한다.
     *
     * @param id 계약 ID
     * @throws BizException 존재하지 않으면 CONTRACT_NOT_FOUND
     */
    @Transactional
    public void delete(Long id) {
        Contract contract = findOrThrow(id);
        attachmentService.deleteAllByContract(id); // 첨부 파일(디스크) + 행 정리
        contractRepository.delete(contract);
    }

    /** 요청 값을 엔티티에 반영(등록/수정 공통). 거래처 연결과 상태 기본값 처리 포함 */
    private void apply(Contract contract, ContractRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new BizException(ErrorCode.CLIENT_NOT_FOUND));
        contract.setClient(client);
        contract.setTitle(request.getTitle());
        contract.setMonthlyFee(request.getMonthlyFee());
        contract.setBillingDay(request.getBillingDay());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setStatus(request.getStatus() != null ? request.getStatus() : ContractStatus.ACTIVE);
        contract.setMemo(request.getMemo());
        contract.setDocumentLocation(request.getDocumentLocation());
        contract.setPaymentMethod(request.getPaymentMethod());
        contract.setDoorCode(request.getDoorCode());
        contract.setCleaningWeekdays(joinWeekdays(request.getCleaningWeekdays()));
        contract.setCleaningCycle(request.getCleaningCycle() != null ? request.getCleaningCycle() : CleaningCycle.WEEKLY);
        contract.setVatType(request.getVatType() != null ? request.getVatType() : VatType.EXCLUSIVE);
        contract.setInitialFee(request.getInitialFee());
        contract.setCleaningScope(request.getCleaningScope());
        contract.setServiceItems(request.getServiceItems());
        contract.setExtraServices(request.getExtraServices());
        contract.setExtraNotes(request.getExtraNotes());
    }

    /** 유효 요일 코드(월~일). 알 수 없는 값은 버린다. */
    private static final java.util.Set<String> VALID_WEEKDAYS =
            new java.util.LinkedHashSet<>(java.util.Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"));

    /**
     * 요일 코드 목록을 "MON,WED,FRI" 문자열로. 요일 정렬 순서로 정규화하고 유효 코드만 남긴다.
     *
     * @param weekdays 요청 요일 목록(선택)
     * @return 쉼표구분 문자열(비면 null)
     */
    private static String joinWeekdays(java.util.List<String> weekdays) {
        if (weekdays == null || weekdays.isEmpty()) {
            return null;
        }
        java.util.Set<String> picked = new java.util.LinkedHashSet<>();
        for (String w : weekdays) {
            if (w != null) {
                String code = w.trim().toUpperCase();
                if (VALID_WEEKDAYS.contains(code)) {
                    picked.add(code);
                }
            }
        }
        if (picked.isEmpty()) {
            return null;
        }
        // 요일 순서(월~일)로 정렬해 저장
        return VALID_WEEKDAYS.stream().filter(picked::contains).collect(java.util.stream.Collectors.joining(","));
    }

    /** ID 로 조회하되(거래처 포함) 없으면 예외 */
    private Contract findOrThrow(Long id) {
        return contractRepository.findByIdWithClient(id)
                .orElseThrow(() -> new BizException(ErrorCode.CONTRACT_NOT_FOUND));
    }
}
