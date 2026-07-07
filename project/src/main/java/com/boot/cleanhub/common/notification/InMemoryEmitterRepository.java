package com.boot.cleanhub.common.notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * <pre>
 *   ConcurrentHashMap 기반 인메모리 Emitter/이벤트 캐시 저장소.
 *   (분산 환경이 필요하면 Hazelcast/Redis 등으로 교체 가능 — 현재는 단일 인스턴스 데모용)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Repository
public class InMemoryEmitterRepository implements EmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();

    @Override
    public SseEmitter save(String emitterId, SseEmitter emitter) {
        emitters.put(emitterId, emitter);
        return emitter;
    }

    @Override
    public void saveEventCache(String eventCacheId, Object event) {
        eventCache.put(eventCacheId, event);
    }

    @Override
    public Map<String, SseEmitter> findAllEmitterStartWithByUserId(String userId) {
        String prefix = userId + "_";
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<String, Object> findAllEventCacheStartWithByUserId(String userId) {
        String prefix = userId + "_";
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void deleteById(String id) {
        emitters.remove(id);
    }

    @Override
    public long count() {
        return emitters.size();
    }
}
