package com.boot.cleanhub.quote.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.cleanhub.quote.domain.Quote;

/**
 * <pre>
 *   견적 저장소.
 *   거래처(client)는 선택 연결이라 left join fetch 로 함께 로딩한다(연결 없는 견적도 조회되게).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    /** 전체 목록(거래처 포함, 최신 등록순) 페이지 */
    @Query(value = "select q from Quote q left join fetch q.client order by q.id desc",
            countQuery = "select count(q) from Quote q")
    Page<Quote> findAllWithClient(Pageable pageable);

    /** 서비스 내용(title) 또는 고객명 부분일치 검색(대소문자 무시, 거래처 포함, 최신 등록순) 페이지 */
    @Query(value = "select q from Quote q left join fetch q.client"
            + " where lower(q.title) like lower(concat('%', :keyword, '%'))"
            + " or lower(q.customerName) like lower(concat('%', :keyword, '%')) order by q.id desc",
            countQuery = "select count(q) from Quote q"
                    + " where lower(q.title) like lower(concat('%', :keyword, '%'))"
                    + " or lower(q.customerName) like lower(concat('%', :keyword, '%'))")
    Page<Quote> search(@Param("keyword") String keyword, Pageable pageable);

    /** 단건 조회(거래처 포함) */
    @Query("select q from Quote q left join fetch q.client where q.id = :id")
    Optional<Quote> findByIdWithClient(@Param("id") Long id);
}
