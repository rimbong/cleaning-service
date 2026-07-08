import { get, post, put, del, downloadGet } from '@/plugins/http/axios'

/**
 * 계약 관리 API — 관리자 전용(/api/admin/contracts).
 * Bearer 토큰은 axios 요청 인터셉터가 자동 첨부한다.
 *
 * 모든 응답은 ApiResponse envelope: { success, code, message, data }.
 * 여기서는 axios 응답(res)을 그대로 반환하고, 화면/스토어에서 res.data.data 로 꺼낸다.
 */
export const contractService = {
    /**
     * 목록 조회 → data: ContractResponse[]
     * @param {Object} params 검색 조건
     * @param {string} [params.keyword]  계약명 검색어
     * @param {number} [params.clientId] 특정 거래처의 계약만 조회
     */
    list({ keyword, clientId } = {}) {
        const query = {}
        if (clientId != null) {
            query.clientId = clientId
        } else if (keyword) {
            query.keyword = keyword
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
}

/** 계약 상태 표시용 옵션(폼 셀렉트·라벨 매핑에 공용) */
export const CONTRACT_STATUSES = [
    { value: 'ACTIVE', label: '진행 중' },
    { value: 'ENDED', label: '종료' },
    { value: 'SUSPENDED', label: '중지' },
]
