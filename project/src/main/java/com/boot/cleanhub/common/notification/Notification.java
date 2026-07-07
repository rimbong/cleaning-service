package com.boot.cleanhub.common.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <pre>
 *   SSE 로 전송되는 알림 메시지.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.06.30
 * @version 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    private String receiver;          // 알림을 받는 유저 식별자
    private String notificationType;  // 알림 종류(INFO/WARN/...)
    private String content;           // 알림 내용
    private String url;               // 클릭 시 이동할 URL
    private Boolean isRead;           // 열람 여부
    private Long timestamp;           // 생성 시각(epoch ms)
}
