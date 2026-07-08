package com.boot.cleanhub.contract.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.cleanhub.contract.domain.Contract;

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

    /** 전체 목록(거래처 포함, 최신 등록순) */
    @Query("select c from Contract c join fetch c.client order by c.id desc")
    List<Contract> findAllWithClient();

    /** 계약명 부분일치 검색(대소문자 무시, 거래처 포함, 최신 등록순) */
    @Query("select c from Contract c join fetch c.client"
            + " where lower(c.title) like lower(concat('%', :keyword, '%')) order by c.id desc")
    List<Contract> searchByTitle(@Param("keyword") String keyword);

    /** 특정 거래처의 계약 목록(거래처 포함, 최신 등록순) */
    @Query("select c from Contract c join fetch c.client where c.client.id = :clientId order by c.id desc")
    List<Contract> findByClientId(@Param("clientId") Long clientId);

    /** 단건 조회(거래처 포함) */
    @Query("select c from Contract c join fetch c.client where c.id = :id")
    Optional<Contract> findByIdWithClient(@Param("id") Long id);
}
