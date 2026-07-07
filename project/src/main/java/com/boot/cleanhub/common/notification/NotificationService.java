package com.boot.cleanhub.common.notification;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   SSE 기반 실시간 알림 서비스.
 *   레거시(HIS) com.project.main.notification 의 SSE 패턴(더미 이벤트로 503 방지,
 *   Last-Event-ID 기반 유실 이벤트 재전송, userId prefix emitter 조회)을 계승하되,
 *   PBox/SuperService/레거시 에러코드 의존을 제거하고 Spring 표준으로 재구현.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** SSE 연결 타임아웃(기본 1시간). */
    private static final long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    /** 더미 이벤트/실제 알림에 사용하는 이벤트 이름. 클라이언트는 이 이름으로 수신한다. */
    private static final String EVENT_NAME = "notification";

    private final EmitterRepository emitterRepository;

    /**
     * SSE 구독. emitter 를 생성·저장하고, 503 방지용 더미 이벤트를 보낸다.
     * lastEventId 가 있으면 그 이후의 미수신 이벤트를 재전송하여 유실을 예방한다.
     */
    public SseEmitter subscribe(String userId, String lastEventId) {
        String emitterId = userId + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(DEFAULT_TIMEOUT));

        emitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
        emitter.onTimeout(() -> emitterRepository.deleteById(emitterId));

        // [1] 503 방지를 위한 더미 이벤트
        sendToEmitter(emitter, emitterId, "EventStream Created. userId=" + userId);

        // [2] 미수신 이벤트 재전송(유실 복구)
        if (lastEventId != null && !lastEventId.isEmpty()) {
            Map<String, Object> events = emitterRepository.findAllEventCacheStartWithByUserId(userId);
            events.entrySet().stream()
                    .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                    .forEach(entry -> sendToEmitter(emitter, entry.getKey(), entry.getValue()));
        }

        return emitter;
    }

    /**
     * 특정 수신자(receiver)의 모든 구독 emitter 에 알림을 전송하고, 이벤트 캐시에 저장한다.
     *
     * @return 전송 대상 emitter 수
     */
    public int send(Notification notification) {
        Map<String, SseEmitter> emitters =
                emitterRepository.findAllEmitterStartWithByUserId(notification.getReceiver());

        emitters.forEach((key, emitter) -> {
            emitterRepository.saveEventCache(key, notification);   // 유실 복구용 캐시
            sendToEmitter(emitter, key, notification);             // 실제 전송
        });
        return emitters.size();
    }

    private void sendToEmitter(SseEmitter emitter, String id, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(id)
                    .name(EVENT_NAME)
                    .data(data));
        } catch (IOException e) {
            emitterRepository.deleteById(id);
            log.warn("SSE 전송 실패 - emitterId={}, msg={}", id, e.getMessage());
        }
    }
}
