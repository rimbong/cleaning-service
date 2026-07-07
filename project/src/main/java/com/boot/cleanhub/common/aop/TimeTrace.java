package com.boot.cleanhub.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 *   메서드 실행 시간/파라미터/예외를 자동 로깅하기 위한 마커 애너테이션.
 *   메서드 또는 클래스(전체 public 메서드)에 부착하면 TimeTraceAspect 가 가로채 측정한다.
 *
 *   레거시(HIS)의 PBox 시그니처/ModelAndView 전제 AOP 대신, 부착한 지점에만 적용되는
 *   현대적·명시적 방식으로 재구현한 것.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeTrace {

    /** 트레이스 라벨(미지정 시 메서드명 사용). */
    String value() default "";
}
