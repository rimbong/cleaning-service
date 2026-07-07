package com.boot.cleanhub.common.mail;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   Thymeleaf 템플릿을 HTML 문자열로 렌더링한다. (메일 본문 생성용)
 *   templates/ 하위 경로를 templateName 으로 받는다. (예: "mail/welcome" → templates/mail/welcome.html)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class MailTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        return templateEngine.process(templateName, context);
    }
}
