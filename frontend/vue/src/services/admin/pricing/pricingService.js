import { get, post, put } from '@/common/plugins/http/axios'

/**
 * 계단·공용부 정기청소 권장가 산정 API — 관리자 전용(/api/admin/pricing).
 *
 * 여기서 나오는 금액은 <b>권장가(참고용)</b>다. 실제 계약 금액은 흥정·경쟁 상황에 따라
 * 달라지므로 화면에서 자동으로 덮어쓰지 않고, 사용자가 '적용'을 눌렀을 때만 옮긴다.
 */
export const pricingService = {
    /** 단가 정책 조회 → data: PricingPolicyResponse */
    getPolicy() {
        return get('/api/admin/pricing/policy')
    },

    /** 단가 정책 수정(최저임금 인상 등) */
    updatePolicy(payload) {
        return put('/api/admin/pricing/policy', payload)
    },

    /**
     * 권장가 산정 → data: PriceEstimateResponse
     * @param {Object} spec
     * @param {number} spec.floors          지상 층수
     * @param {number} spec.householdCount  세대수
     * @param {number} [spec.sharedToilets] 공용 화장실 수
     * @param {number} [spec.extraFloors]   지하·옥상 추가층
     * @param {boolean} [spec.hasElevator]  엘리베이터 유무
     * @param {string} spec.cycle           PricingCycle (MONTHLY_1 등)
     */
    estimate(spec) {
        return post('/api/admin/pricing/estimate', spec)
    },

    /**
     * 적정가 재산정 → data: PriceReviewResponse
     * 진행 중 계약의 현재 월정액과 지금 기준 권장가를 비교한다(금액을 바꾸지는 않는다).
     */
    review() {
        return get('/api/admin/pricing/review')
    },
}

/** 산정용 청소 주기 — 서버 PricingCycle 과 값이 일치해야 한다 */
export const PRICING_CYCLES = [
    { value: 'MONTHLY_1', label: '월 1회' },
    { value: 'MONTHLY_2', label: '월 2회 (격주)' },
    { value: 'MONTHLY_3', label: '월 3회' },
    { value: 'WEEKLY_1', label: '주 1회 (월 4회)' },
    { value: 'WEEKLY_2', label: '주 2회' },
    { value: 'WEEKLY_3', label: '주 3회' },
]
