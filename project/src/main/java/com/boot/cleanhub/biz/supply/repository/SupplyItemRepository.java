package com.boot.cleanhub.biz.supply.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.cleanhub.biz.supply.domain.SupplyItem;

/**
 * <pre>
 *   약품/소모품 품목 저장소. 목록은 품목명 오름차순(창고 정리 순서에 가깝다).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
public interface SupplyItemRepository extends JpaRepository<SupplyItem, Long> {

    /** 전체(품목명순) 페이지 */
    Page<SupplyItem> findAllByOrderByNameAscIdAsc(Pageable pageable);

    /** 품목명 검색(품목명순) 페이지 */
    Page<SupplyItem> findByNameContainingIgnoreCaseOrderByNameAscIdAsc(String name, Pageable pageable);

    /** 전체(품목명순) — 엑셀 출력용(페이징 없이 전부) */
    List<SupplyItem> findAllByOrderByNameAscIdAsc();

    /** 품목명 검색(품목명순) — 엑셀 출력용 */
    List<SupplyItem> findByNameContainingIgnoreCaseOrderByNameAscIdAsc(String name);

    /**
     * 이름+규격이 같은 품목이 이미 있는지 검사한다.
     * 같은 품목이 두 행으로 등록되면 재고가 조용히 쪼개지므로 등록/수정 시 막는다.
     * 저장된 spec 은 NULL 허용이라 SQL 의 `= NULL` 함정을 피하려고 COALESCE 로 빈 문자열 취급한다.
     *
     * @param name      품목명
     * @param spec      규격 — <b>null 이 아닌 값</b>을 넘긴다(규격 없음은 빈 문자열)
     * @param excludeId 검사에서 제외할 id — 신규 등록이면 존재하지 않는 값(-1)을 넘긴다
     * @return 중복이면 true
     */
    @Query("SELECT COUNT(i) > 0 FROM SupplyItem i "
            + "WHERE i.name = :name AND COALESCE(i.spec, '') = :spec AND i.id <> :excludeId")
    boolean existsDuplicate(@Param("name") String name, @Param("spec") String spec, @Param("excludeId") Long excludeId);
}
