import { get, post, put, del } from '@/common/plugins/http/axios'

/**
 * 거래처(건물) 관리 API — 관리자 전용(/api/admin/clients).
 * Bearer 토큰은 axios 요청 인터셉터가 자동 첨부한다.
 *
 * 모든 응답은 ApiResponse envelope: { success, code, message, data }.
 * 여기서는 axios 응답(res)을 그대로 반환하고, 화면/스토어에서 res.data.data 로 꺼낸다.
 */
export const clientService = {
    /**
     * 목록 조회(건물명 검색, 페이징) → data: PageResponse<ClientResponse>
     * @param {Object} [params]
     * @param {string} [params.keyword] 건물명 검색어
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
        return get('/api/admin/clients', { params: query })
    },

    /** 셀렉트 옵션 전체(페이징 없이 id+건물명) → data: ClientOption[]. 계약/견적 폼 드롭다운용 */
    options() {
        return get('/api/admin/clients/options')
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

/** 세금계산서 발행 방식 옵션 */
export const TAX_INVOICE_TYPES = [
    { value: 'ELECTRONIC', label: '전자세금계산서' },
    { value: 'EMAIL', label: '이메일 발송' },
    { value: 'LABOR', label: '인건비 처리' },
    { value: 'NONE', label: '발행 안 함' },
]
