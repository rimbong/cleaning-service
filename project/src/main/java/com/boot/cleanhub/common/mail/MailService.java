package com.boot.cleanhub.common.mail;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   범용 메일 발송 서비스.
 *   레거시(HIS)의 javax.mail 직접 구현(MailMo)을 대체하여, Spring 표준 JavaMailSender 기반으로 구현.
 *
 *   - sendText        : 단순 텍스트 메일
 *   - sendHtml        : HTML 본문 메일
 *   - sendTemplate    : Thymeleaf 템플릿을 렌더링하여 HTML 메일 발송
 *   - sendWithAttachment : 첨부파일 포함 메일
 *
 *   SMTP 설정은 application.yml 의 spring.mail.* (값은 etc.properties 의 mail.* 로 외부화).
 *   실제 발송에는 유효한 SMTP 계정이 필요하다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final MailTemplateRenderer templateRenderer;

    /** 보내는 사람(From) 기본값. */
    @Value("${mail.from:no-reply@example.com}")
    private String defaultFrom;

    /**
     * 단순 텍스트 메일 발송.
     */
    public void sendText(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(defaultFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    /**
     * HTML 본문 메일 발송.
     */
    public void sendHtml(String to, String subject, String html) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
        } catch (Exception e) {
            throw new IllegalStateException("HTML 메일 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Thymeleaf 템플릿을 렌더링하여 HTML 메일 발송.
     *
     * @param templateName templates/ 하위 경로 (예: "mail/welcome")
     * @param variables    템플릿에 바인딩할 변수
     */
    public void sendTemplate(String to, String subject, String templateName, Map<String, Object> variables) {
        String html = templateRenderer.render(templateName, variables);
        sendHtml(to, subject, html);
    }

    /**
     * 첨부파일 포함 메일 발송.
     *
     * @param html     본문(HTML)
     * @param filename 첨부 파일명
     * @param attachment 첨부 데이터 소스 (예: ByteArrayResource)
     */
    public void sendWithAttachment(String to, String subject, String html,
                                   String filename, InputStreamSource attachment) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addAttachment(filename, attachment);
            mailSender.send(mime);
        } catch (Exception e) {
            throw new IllegalStateException("첨부 메일 생성 실패: " + e.getMessage(), e);
        }
    }
}
