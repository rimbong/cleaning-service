package com.boot.cleanhub.common.aop;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   @TimeTrace 가 부착된 메서드(또는 클래스의 메서드)를 가로채
 *   실행 시간 · 파라미터 · 정상/예외 여부를 로깅하고 TraceStore 에 적재한다.
 *
 *   @Around 어드바이스로 메서드 전후를 감싸 측정하며, 예외는 기록 후 그대로 재전파한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Aspect
@Component
@RequiredArgsConstructor
public class TimeTraceAspect {

    private static final Logger log = LoggerFactory.getLogger(TimeTraceAspect.class);

    private final TraceStore traceStore;

    @Around("@annotation(timeTrace) || @within(timeTrace)")
    public Object trace(ProceedingJoinPoint pjp, TimeTrace timeTrace) throws Throwable {
        // 클래스 레벨/메서드 레벨 모두 지원하기 위해, 바인딩된 애너테이션이 없을 수 있어 라벨은 안전하게 처리
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        String label = (timeTrace != null && !timeTrace.value().isEmpty()) ? timeTrace.value() : sig.getName();
        String args = safeArgs(pjp.getArgs());

        long start = System.nanoTime();
        log.info("[TimeTrace] ▶ {} ({}) args={}", label, method, args);
        try {
            Object result = pjp.proceed();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.info("[TimeTrace] ✔ {} 완료 - {}ms", label, elapsedMs);
            traceStore.add(new TraceLog(label, method, args, elapsedMs, "OK", null, System.currentTimeMillis()));
            return result;
        } catch (Throwable t) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.warn("[TimeTrace] ✘ {} 실패 - {}ms - {}", label, elapsedMs, t.toString());
            traceStore.add(new TraceLog(label, method, args, elapsedMs, "ERROR", t.toString(), System.currentTimeMillis()));
            throw t;
        }
    }

    private String safeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            return Arrays.toString(args);
        } catch (Exception e) {
            return "[unprintable]";
        }
    }
}
