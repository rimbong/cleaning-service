package com.boot.cleanhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * <pre>
 *   스케줄링 + 비동기 설정.
 *   레거시(HIS) ScheduleConfiguration(@EnableScheduling/@EnableAsync + 스케줄러 스레드풀)을 계승하되,
 *   Executors.newScheduledThreadPool 대신 Spring 친화적인 ThreadPoolTaskScheduler 를 사용하고,
 *   @Async 전용 실행기(ThreadPoolTaskExecutor)도 함께 정의한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig implements SchedulingConfigurer {

    /** @Scheduled 작업용 스케줄러 풀. */
    @Override
    public void configureTasks(@NonNull ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.initialize();
        taskRegistrar.setScheduler(scheduler);
    }

    /** @Async("taskExecutor") 비동기 작업용 실행기. */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
