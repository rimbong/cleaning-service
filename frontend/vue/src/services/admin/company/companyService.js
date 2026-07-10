import { get, put, post, del } from '@/common/plugins/http/axios'

/**
 * 회사(공급자) 프로필 API — 관리자 전용(/api/admin/company).
 * 세금계산서 발행 주체(운영 회사) 정보 + 도장(인장) 이미지 관리.
 */
export const companyService = {
    /** 회사 프로필 조회 → data: CompanyResponse(hasStamp 포함) */
    get() {
        return get('/api/admin/company')
    },

    /** 회사 프로필 수정 → data: 수정된 CompanyResponse */
    update(payload) {
        return put('/api/admin/company', payload)
    },

    /** 도장 이미지 등록(multipart) → data: 갱신된 CompanyResponse */
    uploadStamp(file) {
        const form = new FormData()
        form.append('file', file)
        return post('/api/admin/company/stamp', form, {
            headers: { 'Content-Type': 'multipart/form-data' },
        })
    },

    /** 도장 이미지 삭제 → data: 갱신된 CompanyResponse */
    removeStamp() {
        return del('/api/admin/company/stamp')
    },

    /** 도장 이미지 미리보기(blob). Bearer 자동 첨부되도록 axios get(responseType blob) 사용. */
    getStampBlob() {
        return get('/api/admin/company/stamp', { responseType: 'blob' })
    },
}
