package com.boot.cleanhub.biz.client.dto;

import lombok.Getter;

/**
 * <pre>
 *   거래처 셀렉트용 경량 옵션 — id + 건물명만.
 *   계약/견적 폼의 거래처 드롭다운처럼 "전체 거래처를 한 번에" 채워야 하는 곳에 쓴다.
 *   목록 API(페이징, size 캡)로 채우면 거래처가 많을 때 일부가 누락되므로 별도 옵션 API 로 분리한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.10
 * @version 1.0
 */
@Getter
public class ClientOption {

    private final Long id;
    private final String name;

    public ClientOption(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
