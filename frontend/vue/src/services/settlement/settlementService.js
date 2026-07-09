import { get, post, put, del } from '@/plugins/http/axios'

/**
 * 정산 API — 관리자 전용(/api/admin/settlements).
 * 월 청구 자동생성 + 청구/입금 관리. 청구(billing) 1:N 입금(payment).
 */
export const settlementService = {
    /** 월 정산 조회(청구 목록+합계) → data: { year, month, totalBilled, totalPaid, totalUnpaid, count, items[] } */
    getMonth(year, month) {
        return get('/api/admin/settlements', { params: { year, month } })
    },

    /** 월 청구 자동 생성 → data: 생성 건수 */
    generate(year, month) {
        return post(`/api/admin/settlements/generate?year=${year}&month=${month}`)
    },

    /** 청구액/메모 수정 → data: BillingResponse */
    editBilling(billingId, payload) {
        return put(`/api/admin/settlements/${billingId}`, payload)
    },

    /** 청구 삭제 */
    deleteBilling(billingId) {
        return del(`/api/admin/settlements/${billingId}`)
    },

    /** 견적 1회성 청구 생성 */
    createQuoteBilling(quoteId, year, month) {
        return post(`/api/admin/settlements/quote/${quoteId}?year=${year}&month=${month}`)
    },

    /** 청구의 입금 목록 → data: PaymentResponse[] */
    listPayments(billingId) {
        return get(`/api/admin/settlements/${billingId}/payments`)
    },

    /** 입금 등록 */
    addPayment(billingId, payload) {
        return post(`/api/admin/settlements/${billingId}/payments`, payload)
    },

    /** 입금 삭제 */
    deletePayment(paymentId) {
        return del(`/api/admin/settlements/payments/${paymentId}`)
    },
}
