import { get, put } from '@/common/plugins/http/axios'

/**
 * 회사(공급자) 프로필 API — 관리자 전용(/api/admin/company). 단일 설정이라 조회/수정만.
 * 세금계산서 발행 주체(운영 회사) 정보.
 */
export const companyService = {
    /** 회사 프로필 조회 → data: CompanyResponse */
    get() {
        return get('/api/admin/company')
    },

    /** 회사 프로필 수정 → data: 수정된 CompanyResponse */
    update(payload) {
        return put('/api/admin/company', payload)
    },
}
