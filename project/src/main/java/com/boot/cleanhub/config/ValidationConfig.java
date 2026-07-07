package com.boot.cleanhub.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   Bean Validation(@Valid) 메시지를 Spring MessageSource(i18n/messages*.properties)로 해석하도록 연결.
 *   덕분에 DTO 애너테이션의 message = "{키}" 가 로케일(Accept-Language/?lang=)에 맞춰 번역된다.
 *   (별도 ValidationMessages.properties 를 두지 않고 기존 messages 파일 하나로 통합 관리)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.01
 * @version 1.0
 */
@Configuration
@RequiredArgsConstructor
public class ValidationConfig {

    private final MessageSource messageSource;

    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
