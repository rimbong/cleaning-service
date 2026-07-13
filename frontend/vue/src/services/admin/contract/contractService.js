import { get, post, put, del, downloadGet } from '@/common/plugins/http/axios'

/**
 * 계약 관리 API — 관리자 전용(/api/admin/contracts).
 * Bearer 토큰은 axios 요청 인터셉터가 자동 첨부한다.
 *
 * 모든 응답은 ApiResponse envelope: { success, code, message, data }.
 * 여기서는 axios 응답(res)을 그대로 반환하고, 화면/스토어에서 res.data.data 로 꺼낸다.
 */
export const contractService = {
    /**
     * 목록 조회(페이징) → data: PageResponse<ContractResponse>
     * @param {Object} params 검색 조건
     * @param {string} [params.keyword]  계약명 검색어
     * @param {number} [params.clientId] 특정 거래처의 계약만 조회
     * @param {number} [params.page]     페이지(1-based)
     * @param {number} [params.size]     페이지 크기
     */
    list({ keyword, clientId, page, size } = {}) {
        const query = {}
        if (clientId != null) {
            query.clientId = clientId
        } else if (keyword) {
            query.keyword = keyword
        }
        if (page != null) {
            query.page = page
        }
        if (size != null) {
            query.size = size
        }
        return get('/api/admin/contracts', { params: query })
    },

    /** 단건 조회 → data: ContractResponse */
    get(id) {
        return get(`/api/admin/contracts/${id}`)
    },

    /** 등록 → data: 생성된 ContractResponse */
    create(payload) {
        return post('/api/admin/contracts', payload)
    },

    /** 수정 → data: 수정된 ContractResponse */
    update(id, payload) {
        return put(`/api/admin/contracts/${id}`, payload)
    },

    /** 삭제 */
    remove(id) {
        return del(`/api/admin/contracts/${id}`)
    },

    /**
     * 계약서(HWP) 다운로드 — 빈 양식을 계약/거래처/회사정보로 채워 받는다.
     * @param {number} id 계약 ID
     * @param {string} fallbackName 서버가 파일명을 안 줄 때 쓸 이름
     * @param {boolean} [withStamp] true 면 회사정보에 등록된 도장을 찍는다
     */
    downloadDocument(id, fallbackName, withStamp = false) {
        return downloadGet(`/api/admin/contracts/${id}/document`, { params: { withStamp }, fallbackName })
    },

    // ===== 계약서 첨부 파일 =====

    /** 첨부 목록(메타데이터) → data: ContractAttachmentResponse[] */
    listAttachments(contractId) {
        return get(`/api/admin/contracts/${contractId}/attachments`)
    },

    /** 첨부 업로드(multipart) → data: 저장된 첨부 메타 */
    uploadAttachment(contractId, file) {
        const form = new FormData()
        form.append('file', file)
        return post(`/api/admin/contracts/${contractId}/attachments`, form, {
            headers: { 'Content-Type': 'multipart/form-data' },
        })
    },

    /** 첨부 다운로드(Bearer 자동 첨부 + blob 저장). 원본 파일명은 서버 헤더로 복원 */
    downloadAttachment(contractId, attachmentId, fallbackName) {
        return downloadGet(`/api/admin/contracts/${contractId}/attachments/${attachmentId}/download`, { fallbackName })
    },

    /** 첨부 삭제 */
    removeAttachment(contractId, attachmentId) {
        return del(`/api/admin/contracts/${contractId}/attachments/${attachmentId}`)
    },

    /** 주간 청소 스케줄(월~일 요일별 거래처 목록) → data: { days: [ { weekday, label, count, items[] } ] } */
    getSchedule() {
        return get('/api/admin/schedule')
    },
}

/** 계약 상태 표시용 옵션(폼 셀렉트·라벨 매핑에 공용) */
export const CONTRACT_STATUSES = [
    { value: 'ACTIVE', label: '진행 중' },
    { value: 'ENDED', label: '종료' },
    { value: 'SUSPENDED', label: '중지' },
]

/** 청소 요일 옵션(월~일). value 는 서버 요일 코드. */
export const WEEKDAYS = [
    { value: 'MON', label: '월' },
    { value: 'TUE', label: '화' },
    { value: 'WED', label: '수' },
    { value: 'THU', label: '목' },
    { value: 'FRI', label: '금' },
    { value: 'SAT', label: '토' },
    { value: 'SUN', label: '일' },
]

/** 청소 주기 옵션 */
export const CLEANING_CYCLES = [
    { value: 'WEEKLY', label: '매주' },
    { value: 'BIWEEKLY', label: '격주' },
    { value: 'MONTHLY', label: '매월' },
]

/** 부가세 기준 옵션 — 세금계산서 집계에서 공급가액·세액 산출에 사용 */
export const VAT_TYPES = [
    { value: 'EXCLUSIVE', label: '부가세 별도' },
    { value: 'INCLUSIVE', label: '부가세 포함' },
    { value: 'FREE', label: '면세' },
]
