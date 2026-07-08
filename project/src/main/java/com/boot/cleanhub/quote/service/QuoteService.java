package com.boot.cleanhub.quote.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boot.cleanhub.client.domain.Client;
import com.boot.cleanhub.client.repository.ClientRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.quote.domain.Quote;
import com.boot.cleanhub.quote.domain.QuoteStatus;
import com.boot.cleanhub.quote.dto.QuoteRequest;
import com.boot.cleanhub.quote.dto.QuoteResponse;
import com.boot.cleanhub.quote.repository.QuoteRepository;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   견적 도메인 서비스 — 등록/조회/수정/삭제.
 *   조회는 읽기 전용 트랜잭션, 변경은 쓰기 트랜잭션으로 구분한다.
 *   거래처(client) 연결은 선택이라, clientId 가 있을 때만 존재를 확인해 연결한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final ClientRepository clientRepository;

    /**
     * 견적 목록 조회(서비스 내용/고객명 검색 지원).
     *
     * @param keyword 검색어(비어 있으면 전체)
     * @return 최신 등록순 견적 목록
     */
    public List<QuoteResponse> list(String keyword) {
        List<Quote> quotes;
        if (StringUtils.hasText(keyword)) {
            quotes = quoteRepository.search(keyword.trim());
        } else {
            quotes = quoteRepository.findAllWithClient();
        }
        return quotes.stream()
                .map(QuoteResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 견적 단건 조회.
     *
     * @param id 견적 ID
     * @return 견적 응답
     * @throws BizException 존재하지 않으면 QUOTE_NOT_FOUND
     */
    public QuoteResponse get(Long id) {
        return QuoteResponse.from(findOrThrow(id));
    }

    /**
     * 견적 등록.
     *
     * @param request 등록 요청
     * @return 생성된 견적 응답
     * @throws BizException 지정한 거래처가 없으면 CLIENT_NOT_FOUND
     */
    @Transactional
    public QuoteResponse create(QuoteRequest request) {
        Quote quote = new Quote();
        apply(quote, request);
        return QuoteResponse.from(quoteRepository.save(quote));
    }

    /**
     * 견적 수정.
     *
     * @param id      견적 ID
     * @param request 수정 요청
     * @return 수정된 견적 응답
     * @throws BizException 견적이 없으면 QUOTE_NOT_FOUND, 지정한 거래처가 없으면 CLIENT_NOT_FOUND
     */
    @Transactional
    public QuoteResponse update(Long id, QuoteRequest request) {
        Quote quote = findOrThrow(id);
        apply(quote, request);
        // flush 로 @PreUpdate(updatedAt) 반영 후 DTO 생성 — 응답의 updatedAt 이 이번 수정 시각이 되게.
        quoteRepository.saveAndFlush(quote);
        return QuoteResponse.from(quote);
    }

    /**
     * 견적 삭제.
     *
     * @param id 견적 ID
     * @throws BizException 존재하지 않으면 QUOTE_NOT_FOUND
     */
    @Transactional
    public void delete(Long id) {
        Quote quote = findOrThrow(id);
        quoteRepository.delete(quote);
    }

    /** 요청 값을 엔티티에 반영(등록/수정 공통). 거래처 연결(선택)과 상태 기본값 처리 포함 */
    private void apply(Quote quote, QuoteRequest request) {
        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new BizException(ErrorCode.CLIENT_NOT_FOUND));
        }
        quote.setClient(client);
        quote.setCustomerName(request.getCustomerName());
        quote.setCustomerPhone(request.getCustomerPhone());
        quote.setAddress(request.getAddress());
        quote.setTitle(request.getTitle());
        quote.setAmount(request.getAmount());
        quote.setQuoteDate(request.getQuoteDate());
        quote.setValidUntil(request.getValidUntil());
        quote.setStatus(request.getStatus() != null ? request.getStatus() : QuoteStatus.PENDING);
        quote.setMemo(request.getMemo());
    }

    /** ID 로 조회하되(거래처 포함) 없으면 예외 */
    private Quote findOrThrow(Long id) {
        return quoteRepository.findByIdWithClient(id)
                .orElseThrow(() -> new BizException(ErrorCode.QUOTE_NOT_FOUND));
    }
}
