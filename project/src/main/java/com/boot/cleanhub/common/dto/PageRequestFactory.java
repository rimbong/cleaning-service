package com.boot.cleanhub.common.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * <pre>
 *   요청 파라미터(page/size)를 Spring Data 의 Pageable 로 변환하는 공통 팩토리.
 *
 *   - page 는 1-based 로 받는다(첫 페이지=1). 내부적으로 0-based 로 변환.
 *   - 잘못되거나 비어 있는 값은 기본값으로 보정하고, size 는 상한으로 캡한다
 *     (과도한 size 로 한 번에 대량 조회하는 것을 방지).
 *   - 정렬은 각 저장소 쿼리의 order by 로 처리하므로 여기서는 정렬을 싣지 않는다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public final class PageRequestFactory {

    /** 기본 페이지 크기 */
    public static final int DEFAULT_SIZE = 10;
    /** 페이지 크기 상한 */
    public static final int MAX_SIZE = 200;

    private PageRequestFactory() {
    }

    /**
     * page/size 요청 값으로 Pageable 생성.
     *
     * @param page 1-based 페이지 번호(null/1미만이면 1)
     * @param size 페이지 크기(null/1미만이면 기본값, 상한 초과 시 상한)
     * @return 0-based PageRequest
     */
    public static Pageable of(Integer page, Integer size) {
        int pageNumber = (page == null || page < 1) ? 1 : page;
        int pageSize;
        if (size == null || size < 1) {
            pageSize = DEFAULT_SIZE;
        } else {
            pageSize = Math.min(size, MAX_SIZE);
        }
        return PageRequest.of(pageNumber - 1, pageSize);
    }
}
