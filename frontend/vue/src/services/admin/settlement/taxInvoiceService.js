import { get, post, del, downloadGet } from '@/common/plugins/http/axios'

/**
 * 세금계산서 API — 관리자 전용(/api/admin/tax-invoices).
 * 기간·거래처 집계(청구/수금 기준) + 발행 기록 + 집계표 엑셀 출력.
 */
export const taxInvoiceService = {
    /** 거래처별 기간 집계 → data: { year, fromMonth, toMonth, basis, totalSupply, totalTax, rows[] } */
    aggregate(year, fromMonth, toMonth, basis) {
        return get('/api/admin/tax-invoices/aggregate', { params: { year, fromMonth, toMonth, basis } })
    },

    /** 집계표 엑셀 다운로드(Bearer 자동, 파일 저장) */
    downloadExcel(year, fromMonth, toMonth, basis, fallbackName) {
        return downloadGet('/api/admin/tax-invoices/excel', { params: { year, fromMonth, toMonth, basis }, fallbackName })
    },

    /** 발행 기록 목록 → data: TaxInvoiceResponse[] */
    list() {
        return get('/api/admin/tax-invoices')
    },

    /** 발행 기록 저장 */
    issue(payload) {
        return post('/api/admin/tax-invoices/issue', payload)
    },

    /** 발행 기록 삭제 */
    remove(id) {
        return del(`/api/admin/tax-invoices/${id}`)
    },

    /** 개별 세금계산서(별지11호) 양식 다운로드(Bearer 자동, 파일 저장) */
    downloadForm(id, fallbackName) {
        return downloadGet(`/api/admin/tax-invoices/${id}/form`, { fallbackName })
    },
}

/** 집계 기준 옵션 */
export const TAX_BASIS = [
    { value: 'BILLED', label: '청구 기준' },
    { value: 'PAID', label: '수금 기준' },
]
