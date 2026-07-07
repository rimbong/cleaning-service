package com.boot.cleanhub.client.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.cleanhub.client.domain.Client;

/**
 * <pre>
 *   거래처(건물) 저장소.
 *   기본 CRUD 는 JpaRepository 가 제공하고, 목록 검색용 메서드만 추가한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.07
 * @version 1.0
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

    /** 건물명 부분일치 검색(대소문자 무시), 최신 등록순 */
    List<Client> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    /** 전체 목록(최신 등록순) */
    List<Client> findAllByOrderByIdDesc();
}
