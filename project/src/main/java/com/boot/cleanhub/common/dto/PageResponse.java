package com.boot.cleanhub.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

/**
 * <pre>
 *   목록 페이징 표준 응답.
 *   ApiResponse 의 data 자리에 담겨, 프론트가 목록 + 페이지 정보를 일관되게 소비한다.
 *
 *   page 는 1-based(첫 페이지=1) 로 노출한다 — 화면 표기와 맞추기 위함
 *   (Spring Data 의 Page.getNumber() 는 0-based 라 +1 해서 담는다).
 * </pre>
 *
 * @param <T> 목록 요소 타입(주로 응답 DTO)
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class PageResponse<T> {

    /** 현재 페이지의 목록 */
    private final List<T> content;
    /** 현재 페이지 번호(1-based) */
    private final int page;
    /** 페이지 크기 */
    private final int size;
    /** 전체 건수 */
    private final long totalElements;
    /** 전체 페이지 수 */
    private final int totalPages;
    /** 첫 페이지 여부 */
    private final boolean first;
    /** 마지막 페이지 여부 */
    private final boolean last;

    private PageResponse(List<T> content, int page, int size, long totalElements, int totalPages,
            boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    /**
     * Spring Data 의 Page 를 표준 응답으로 변환.
     *
     * @param page 조회 결과 페이지(요소가 이미 응답 DTO 로 매핑되어 있어야 함)
     * @param <T>  요소 타입
     * @return 페이징 응답
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
