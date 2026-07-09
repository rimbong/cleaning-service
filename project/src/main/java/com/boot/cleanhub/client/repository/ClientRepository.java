package com.boot.cleanhub.client.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.cleanhub.client.domain.Client;

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
}
