package com.boot.cleanhub.common.aop;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

/**
 * <pre>
 *   최근 트레이스 결과를 메모리에 보관하는 경량 저장소(최대 MAX 개, 최신 우선).
 *   테스트 페이지에서 Aspect 가 가로챈 결과를 조회하기 위한 용도.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Component
public class TraceStore {

    private static final int MAX = 20;
    private final Deque<TraceLog> logs = new ConcurrentLinkedDeque<>();

    public void add(TraceLog log) {
        logs.addFirst(log);
        while (logs.size() > MAX) {
            logs.pollLast();
        }
    }

    /** 최신순 트레이스 목록. */
    public List<TraceLog> recent() {
        return new ArrayList<>(logs);
    }

    public void clear() {
        logs.clear();
    }
}
