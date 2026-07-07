package com.boot.cleanhub.common.notification;

import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <pre>
 *   SSE Emitter 와 이벤트 캐시(유실 복구용)를 보관하는 저장소.
 *   emitterId / eventCacheId 는 "{userId}_{timestamp}" 형식으로, userId prefix 로 묶어 조회한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
public interface EmitterRepository {

    SseEmitter save(String emitterId, SseEmitter emitter);

    void saveEventCache(String eventCacheId, Object event);

    Map<String, SseEmitter> findAllEmitterStartWithByUserId(String userId);

    Map<String, Object> findAllEventCacheStartWithByUserId(String userId);

    void deleteById(String id);

    long count();
}
