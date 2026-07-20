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

/**
 * 약품 pH 구분 — 서버 PhType 과 값이 일치해야 한다.
 * 세제 선택의 원칙은 "오염의 성질과 반대되는 pH를 쓴다".
 */
export const PH_TYPES = [
    { value: 'ACID', label: '산성', range: 'pH 0~6', soil: '물때·석회, 요석, 녹, 백화' },
    { value: 'NEUTRAL', label: '중성', range: 'pH 6~8', soil: '먼지, 가벼운 기름·손때' },
    { value: 'ALKALI', label: '알칼리성', range: 'pH 8~14', soil: '기름때·찌든때, 음식물, 그을음' },
    { value: 'OXIDIZER', label: '표백·산화계', range: '종류마다 다름', soil: '곰팡이, 얼룩 색소, 살균' },
    { value: 'ENZYME', label: '효소계', range: '약알칼리~중성', soil: '단백질, 배수구 유기물' },
    { value: 'ETC', label: '기타', range: '', soil: '' },
]

/**
 * 절대 섞으면 안 되는 조합 — 보유 약품에 이 둘이 함께 있으면 화면에서 경고한다.
 * 청소 사고는 대부분 "더 강하게" 섞다가 난다.
 */
export const DANGEROUS_PAIRS = [
    {
        a: 'OXIDIZER',
        b: 'ACID',
        gas: '염소가스',
        detail: '염소계 표백제(락스)와 산성 세제·식초·구연산을 섞으면 염소가스가 나옵니다. 소량으로도 중독·질식 위험이 있습니다.',
    },
    {
        a: 'OXIDIZER',
        b: 'ALKALI',
        gas: '클로라민 가스',
        detail: '락스와 암모니아가 든 세제를 섞으면 클로라민 가스가 나옵니다. 호흡곤란·폐 손상 위험이 있습니다.',
    },
]

/** 입출고 구분 옵션 — ADJUST 는 "세어본 실제 수량"을 입력받는다(차이만 이력에 남음) */
export const SUPPLY_TX_TYPES = [
    { value: 'IN', label: '입고' },
    { value: 'OUT', label: '사용' },
    { value: 'ADJUST', label: '조정(실사)' },
]
