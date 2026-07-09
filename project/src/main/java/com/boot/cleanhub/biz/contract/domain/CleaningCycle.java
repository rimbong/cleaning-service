package com.boot.cleanhub.biz.contract.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   청소 주기 — 정기 청소가 얼마나 자주 실행되는지.
 *   수정환경.xls 의 "격주","첫주" 같은 표기를 구조화한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum CleaningCycle {

    WEEKLY("매주"),
    BIWEEKLY("격주"),
    MONTHLY("매월");

    private final String label;
}
