package com.boot.cleanhub.biz.contract.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.client.repository.ClientRepository;
import com.boot.cleanhub.common.dto.PageResponse;
import com.boot.cleanhub.biz.contract.domain.Contract;
import com.boot.cleanhub.biz.contract.domain.ContractStatus;
import com.boot.cleanhub.biz.contract.dto.ContractRequest;
import com.boot.cleanhub.biz.contract.dto.ContractResponse;
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
    }

    /** ID 로 조회하되(거래처 포함) 없으면 예외 */
    private Contract findOrThrow(Long id) {
        return contractRepository.findByIdWithClient(id)
                .orElseThrow(() -> new BizException(ErrorCode.CONTRACT_NOT_FOUND));
    }
}
