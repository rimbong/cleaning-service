import { get, post, put, del } from '@/plugins/http/axios'

/**
 * 거래처(건물) 관리 API — 관리자 전용(/api/admin/clients).
 * Bearer 토큰은 axios 요청 인터셉터가 자동 첨부한다.
 *
 * 모든 응답은 ApiResponse envelope: { success, code, message, data }.
 * 여기서는 axios 응답(res)을 그대로 반환하고, 화면/스토어에서 res.data.data 로 꺼낸다.
 */
export const clientService = {
    /** 목록 조회(건물명 검색) → data: ClientResponse[] */
    list(keyword) {
        return get('/api/admin/clients', { params: keyword ? { keyword } : {} })
    },

    /** 단건 조회 → data: ClientResponse */
    get(id) {
        return get(`/api/admin/clients/${id}`)
    },

    /** 등록 → data: 생성된 ClientResponse */
    create(payload) {
        return post('/api/admin/clients', payload)
    },

    /** 수정 → data: 수정된 ClientResponse */
    update(id, payload) {
        return put(`/api/admin/clients/${id}`, payload)
    },

    /** 삭제 */
    remove(id) {
        return del(`/api/admin/clients/${id}`)
    },
}

/** 청소 종류 표시용 옵션(폼 셀렉트·라벨 매핑에 공용) */
export const CLEANING_TYPES = [
    { value: 'REGULAR', label: '정기 청소' },
    { value: 'SPECIAL', label: '특수 청소' },
]
