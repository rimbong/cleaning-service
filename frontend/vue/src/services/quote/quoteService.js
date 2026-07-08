import { get, post, put, del } from '@/plugins/http/axios'

/**
 * 견적 관리 API — 관리자 전용(/api/admin/quotes).
 * Bearer 토큰은 axios 요청 인터셉터가 자동 첨부한다.
 *
 * 모든 응답은 ApiResponse envelope: { success, code, message, data }.
 * 여기서는 axios 응답(res)을 그대로 반환하고, 화면/스토어에서 res.data.data 로 꺼낸다.
 */
export const quoteService = {
    /** 목록 조회(서비스 내용/고객명 검색) → data: QuoteResponse[] */
    list(keyword) {
        return get('/api/admin/quotes', { params: keyword ? { keyword } : {} })
    },

    /** 단건 조회 → data: QuoteResponse */
    get(id) {
        return get(`/api/admin/quotes/${id}`)
    },

    /** 등록 → data: 생성된 QuoteResponse */
    create(payload) {
        return post('/api/admin/quotes', payload)
    },

    /** 수정 → data: 수정된 QuoteResponse */
    update(id, payload) {
        return put(`/api/admin/quotes/${id}`, payload)
    },

    /** 삭제 */
    remove(id) {
        return del(`/api/admin/quotes/${id}`)
    },
}

/** 견적 상태 표시용 옵션(폼 셀렉트·라벨 매핑에 공용) */
export const QUOTE_STATUSES = [
    { value: 'PENDING', label: '대기' },
    { value: 'ACCEPTED', label: '수락' },
    { value: 'REJECTED', label: '거절' },
]
