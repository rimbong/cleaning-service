package com.boot.cleanhub.biz.contract.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.cleanhub.biz.contract.domain.Contract;

/**
 * <pre>
 *   계약 저장소.
 *   목록/상세 조회 시 거래처(client)를 함께 로딩하려고 fetch join 을 쓴다
 *   (LAZY 연관을 N+1 없이 한 번에 가져와 응답 DTO 에서 건물명을 바로 노출하기 위함).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
public interface ContractRepository extends JpaRepository<Contract, Long> {

    /**
     * 전체 목록(거래처 포함, 최신 등록순) 페이지.
     * client 는 to-one 연관이라 fetch join + 페이징을 함께 써도 메모리 페이징 경고가 없다.
     * count 쿼리는 fetch join 없이 별도로 지정한다.
     */
    @Query(value = "select c from Contract c join fetch c.client order by c.id desc",
            countQuery = "select count(c) from Contract c")
    Page<Contract> findAllWithClient(Pageable pageable);

    /** 계약명 부분일치 검색(대소문자 무시, 거래처 포함, 최신 등록순) 페이지 */
    @Query(value = "select c from Contract c join fetch c.client"
            + " where lower(c.title) like lower(concat('%', :keyword, '%')) order by c.id desc",
            countQuery = "select count(c) from Contract c"
                    + " where lower(c.title) like lower(concat('%', :keyword, '%'))")
    Page<Contract> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    /** 특정 거래처의 계약 목록(거래처 포함, 최신 등록순) 페이지 */
    @Query(value = "select c from Contract c join fetch c.client where c.client.id = :clientId order by c.id desc",
            countQuery = "select count(c) from Contract c where c.client.id = :clientId")
    Page<Contract> findByClientId(@Param("clientId") Long clientId, Pageable pageable);

    /** 단건 조회(거래처 포함) */
    @Query("select c from Contract c join fetch c.client where c.id = :id")
    Optional<Contract> findByIdWithClient(@Param("id") Long id);

    /** 특정 거래처에 걸린 계약 수(거래처 삭제 가능 여부 판단용) */
    long countByClientId(Long clientId);

    /**
     * 특정 기간(월)에 유효한 ACTIVE 계약 — 정산 월 청구 자동 생성 대상.
     * 시작일<=월말 AND (종료일 없음 OR 종료일>=월초).
     */
    @Query("select c from Contract c where c.status = com.boot.cleanhub.biz.contract.domain.ContractStatus.ACTIVE"
            + " and c.startDate <= :monthEnd and (c.endDate is null or c.endDate >= :monthStart)")
    List<Contract> findActiveInPeriod(@Param("monthStart") java.time.LocalDate monthStart,
            @Param("monthEnd") java.time.LocalDate monthEnd);
}
