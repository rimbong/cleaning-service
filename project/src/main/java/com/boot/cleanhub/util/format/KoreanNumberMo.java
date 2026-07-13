package com.boot.cleanhub.util.format;

/**
 * <pre>
 *   KoreanNumberMo — 금액을 한글로 읽는 문자열로 바꾼다.
 *   계약서의 "일금 삼십만 원정" 같은 표기에 쓴다.
 *
 *   [표기 규칙]
 *     - 네 자리씩 끊어 만/억/조 단위를 붙인다. (예: 12,345,678 -> 천이백삼십사만오천육백칠십팔)
 *     - 십·백·천 앞의 1 은 읽지 않는다. (예: 15 -> 십오, 100 -> 백, 1,000,000 -> 백만)
 *     - 0 은 "영".
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.13
 * @version 1.0
 */
public final class KoreanNumberMo {

    /** 0~9 한글 숫자 */
    private static final char[] DIGITS = { '영', '일', '이', '삼', '사', '오', '육', '칠', '팔', '구' };

    /** 네 자리 그룹 안의 자릿수 단위 — 천, 백, 십, (일) */
    private static final String[] SUB_UNITS = { "천", "백", "십", "" };

    /** 네 자리 그룹 단위 — 낮은 자리부터 */
    private static final String[] GROUP_UNITS = { "", "만", "억", "조", "경" };

    /** 한 그룹의 자릿수(만 단위) */
    private static final int GROUP_SIZE = 4;

    private KoreanNumberMo() {
        throw new AssertionError("인스턴스화 금지");
    }

    /**
     * 금액을 한글 표기로 바꾼다.
     *
     * @param amount 금액(음수면 앞에 "마이너스")
     * @return 한글 표기(예: 300000 -> "삼십만")
     */
    public static String toHangul(long amount) {
        if (amount == 0) {
            return String.valueOf(DIGITS[0]);
        }
        if (amount < 0) {
            return "마이너스" + toHangul(-amount);
        }

        // 네 자리씩 끊어 낮은 자리부터 담는다.
        StringBuilder result = new StringBuilder();
        long rest = amount;
        int groupIndex = 0;
        while (rest > 0 && groupIndex < GROUP_UNITS.length) {
            int group = (int) (rest % 10000);
            if (group > 0) {
                result.insert(0, groupToHangul(group) + GROUP_UNITS[groupIndex]);
            }
            rest /= 10000;
            groupIndex++;
        }
        return result.toString();
    }

    /** 네 자리 이하 수(1~9999)를 한글로. 십·백·천 앞의 1 은 생략한다. */
    private static String groupToHangul(int group) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < GROUP_SIZE; i++) {
            int digit = (group / (int) Math.pow(10, GROUP_SIZE - 1 - i)) % 10;
            if (digit == 0) {
                continue;
            }
            String unit = SUB_UNITS[i];
            if (digit == 1 && !unit.isEmpty()) {
                sb.append(unit);
            } else {
                sb.append(DIGITS[digit]).append(unit);
            }
        }
        return sb.toString();
    }
}
