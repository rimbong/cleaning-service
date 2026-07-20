import { get, post, put, del, downloadGet } from '@/common/plugins/http/axios'

/**
 * 약품/소모품 재고 API — 관리자 전용(/api/admin/supplies).
 * 재고 수량은 서버가 입출고 이력 합계로 계산해 내려준다(currentQuantity).
 */
export const supplyService = {
    /**
     * 품목 목록(품목명 검색, 페이징) → data: PageResponse<SupplyItemResponse>
     * @param {Object} [params]
     * @param {string} [params.keyword] 검색어
     * @param {number} [params.page]    페이지(1-based)
     * @param {number} [params.size]    페이지 크기
     */
    list({ keyword, page, size } = {}) {
        const query = {}
        if (keyword) {
            query.keyword = keyword
        }
        if (page != null) {
            query.page = page
        }
        if (size != null) {
            query.size = size
        }
        return get('/api/admin/supplies', { params: query })
    },

    /** 품목 단건 → data: SupplyItemResponse */
    get(id) {
        return get(`/api/admin/supplies/${id}`)
    },

    /** 품목 등록 */
    create(payload) {
        return post('/api/admin/supplies', payload)
    },

    /** 품목 수정 */
    update(id, payload) {
        return put(`/api/admin/supplies/${id}`, payload)
    },

    /** 품목 삭제(입출고 이력이 있으면 서버가 거부) */
    remove(id) {
        return del(`/api/admin/supplies/${id}`)
    },

    /** 품목별 입출고 이력(최신순, 페이징) → data: PageResponse<SupplyTransactionResponse> */
    history(id, { page, size } = {}) {
        const query = {}
        if (page != null) {
            query.page = page
        }
        if (size != null) {
            query.size = size
        }
        return get(`/api/admin/supplies/${id}/transactions`, { params: query })
    },

    /** 입고/사용/조정 등록 — quantity 는 항상 양수(부호는 서버가 붙인다) */
    addTransaction(id, payload) {
        return post(`/api/admin/supplies/${id}/transactions`, payload)
    },

    /** 잘못 등록한 입출고 이력 삭제 */
    removeTransaction(id, transactionId) {
        return del(`/api/admin/supplies/${id}/transactions/${transactionId}`)
    },

    /** 재고 현황 엑셀 다운로드(품목명 검색 반영, Bearer 자동) */
    downloadExcel(keyword, fallbackName) {
        return downloadGet('/api/admin/supplies/excel', { params: keyword ? { keyword } : {}, fallbackName })
    },
}

/** 입출고 구분 옵션 — ADJUST 는 "세어본 실제 수량"을 입력받는다(차이만 이력에 남음) */
export const SUPPLY_TX_TYPES = [
    { value: 'IN', label: '입고' },
    { value: 'OUT', label: '사용' },
    { value: 'ADJUST', label: '조정(실사)' },
]
