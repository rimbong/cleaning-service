package com.boot.cleanhub.client.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boot.cleanhub.client.domain.Client;
import com.boot.cleanhub.client.dto.ClientRequest;
import com.boot.cleanhub.client.dto.ClientResponse;
import com.boot.cleanhub.client.repository.ClientRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   거래처(건물) 도메인 서비스 — 등록/조회/수정/삭제.
 *   조회는 읽기 전용 트랜잭션, 변경은 쓰기 트랜잭션으로 구분한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.07
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;

    /**
     * 거래처 목록 조회(건물명 검색 지원).
     *
     * @param keyword 건물명 검색어(비어 있으면 전체)
     * @return 최신 등록순 거래처 목록
     */
    public List<ClientResponse> list(String keyword) {
        List<Client> clients;
        if (StringUtils.hasText(keyword)) {
            clients = clientRepository.findByNameContainingIgnoreCaseOrderByIdDesc(keyword.trim());
        } else {
            clients = clientRepository.findAllByOrderByIdDesc();
        }
        return clients.stream()
                .map(ClientResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 거래처 단건 조회.
     *
     * @param id 거래처 ID
     * @return 거래처 응답
     * @throws BizException 존재하지 않으면 CLIENT_NOT_FOUND
     */
    public ClientResponse get(Long id) {
        return ClientResponse.from(findOrThrow(id));
    }

    /**
     * 거래처 등록.
     *
     * @param request 등록 요청
     * @return 생성된 거래처 응답
     */
    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = new Client();
        apply(client, request);
        return ClientResponse.from(clientRepository.save(client));
    }

    /**
     * 거래처 수정.
     *
     * @param id      거래처 ID
     * @param request 수정 요청
     * @return 수정된 거래처 응답
     * @throws BizException 존재하지 않으면 CLIENT_NOT_FOUND
     */
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = findOrThrow(id);
        apply(client, request);
        return ClientResponse.from(client); // 영속 상태라 flush 시 자동 반영
    }

    /**
     * 거래처 삭제.
     *
     * @param id 거래처 ID
     * @throws BizException 존재하지 않으면 CLIENT_NOT_FOUND
     */
    @Transactional
    public void delete(Long id) {
        Client client = findOrThrow(id);
        clientRepository.delete(client);
    }

    /** 요청 값을 엔티티에 반영(등록/수정 공통) */
    private void apply(Client client, ClientRequest request) {
        client.setName(request.getName());
        client.setAddress(request.getAddress());
        client.setManagerName(request.getManagerName());
        client.setManagerPhone(request.getManagerPhone());
        client.setCleaningType(request.getCleaningType());
        client.setContractStartDate(request.getContractStartDate());
        client.setMemo(request.getMemo());
    }

    /** ID 로 조회하되 없으면 예외 */
    private Client findOrThrow(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.CLIENT_NOT_FOUND));
    }
}
