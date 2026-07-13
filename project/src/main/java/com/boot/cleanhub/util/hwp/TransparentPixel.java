package com.boot.cleanhub.util.hwp;

/**
 * <pre>
 *   1x1 투명 PNG 상수.
 *   양식에 박힌 그림(도장) 틀은 그대로 두고 "아무것도 안 찍힌" 상태로 만들 때 쓴다.
 *   그림 컨트롤 자체를 지우면 문단의 그림 앵커까지 손봐야 해서, 이미지 내용만 투명으로 바꾸는 편이 안전하다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.13
 * @version 1.0
 */
public final class TransparentPixel {

    /** 1x1 RGBA(알파 0) PNG 원본 바이트 */
    public static final byte[] PNG = {
        (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D, (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x08, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1F, (byte) 0x15, (byte) 0xC4,
        (byte) 0x89, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0B, (byte) 0x49, (byte) 0x44, (byte) 0x41,
        (byte) 0x54, (byte) 0x78, (byte) 0xDA, (byte) 0x63, (byte) 0x60, (byte) 0x00, (byte) 0x02, (byte) 0x00,
        (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0xE9, (byte) 0xFA, (byte) 0xDC, (byte) 0xD8,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x45, (byte) 0x4E, (byte) 0x44,
        (byte) 0xAE, (byte) 0x42, (byte) 0x60, (byte) 0x82
    };

    private TransparentPixel() {
        throw new AssertionError("인스턴스화 금지");
    }
}
