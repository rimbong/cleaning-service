import { get, post, put, del } from '@/plugins/http/axios'

/**
 * 지출 관리 API — 관리자 전용(/api/admin/expenses). 주유 등 경비(정산과 독립).
 */
export const expenseService = {
    /** 목록(거래처/주유소명 검색) → data: ExpenseResponse[] */
    list(keyword) {
        return get('/api/admin/expenses', { params: keyword ? { keyword } : {} })
    },

    /** 단건 → data: ExpenseResponse */
    get(id) {
        return get(`/api/admin/expenses/${id}`)
    },

    /** 등록 */
    create(payload) {
        return post('/api/admin/expenses', payload)
    },

    /** 수정 */
    update(id, payload) {
        return put(`/api/admin/expenses/${id}`, payload)
    },

    /** 삭제 */
    remove(id) {
        return del(`/api/admin/expenses/${id}`)
    },
}

/** 지출 분류 옵션 */
export const EXPENSE_CATEGORIES = [
    { value: 'FUEL', label: '주유' },
    { value: 'ETC', label: '기타' },
]
