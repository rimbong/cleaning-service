package com.boot.cleanhub.biz.client.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.client.dto.ClientOption;

/**
 * <pre>
 *   거래처(건물) 저장소.
 *   기본 CRUD 는 JpaRepository 가 제공하고, 목록 검색용 메서드만 추가한다.
 *   목록은 페이징(Pageable)으로 조회한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.07
 * @version 1.1
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

    /** 건물명 부분일치 검색(대소문자 무시), 최신 등록순 페이지 */
    Page<Client> findByNameContainingIgnoreCaseOrderByIdDesc(String name, Pageable pageable);

    /** 전체 목록(최신 등록순) 페이지 */
    Page<Client> findAllByOrderByIdDesc(Pageable pageable);

    /** 셀렉트 옵션용 — 전체 거래처를 id+건물명만, 건물명 순으로(페이징 없이 전량) */
    @Query("select new com.boot.cleanhub.biz.client.dto.ClientOption(c.id, c.name) from Client c order by c.name")
    List<ClientOption> findAllOptions();
}
