/**
 * 관리자 메뉴 정의 — 사이드바(AdminLayout)와 대시보드 바로가기 카드(AdminDashboardView)의 공통 출처.
 *
 * 예전에는 두 화면이 각자 메뉴 배열을 들고 있어서 도메인을 추가할 때 사이드바만 고치고
 * 대시보드를 잊는 일이 반복됐다(청소 스케줄·연간 수금현황·회사 정보·약품 재고가 그렇게 빠져 있었다).
 * 메뉴를 여기 한 곳에 두고 두 화면이 같이 읽게 해서 그 어긋남을 구조적으로 없앤다.
 *
 * 새 도메인 추가 = 아래 배열에 한 줄 추가(끝). 라우트는 router/admin/<도메인>Routes.js 에 따로 등록한다.
 *
 * 항목 필드
 *   to     : 이동 경로
 *   label  : 사이드바·카드에 표시할 이름
 *   icon   : 이모지 아이콘
 *   exact  : 활성 표시를 정확히 일치할 때만 할지(하위 경로가 따로 메뉴에 있으면 true)
 *   desc   : 대시보드 카드 설명. <b>이 값이 있는 항목만 카드로 나온다</b>
 *            (대시보드 자신은 카드로 만들 필요가 없어 desc 가 없다)
 */
export const ADMIN_MENU = [
    {
        to: '/admin',
        label: '대시보드',
        icon: '🏠',
        exact: true,
    },
    {
        to: '/admin/clients',
        label: '거래처 관리',
        icon: '🏢',
        exact: false,
        desc: '건물·거래처 등록 및 조회',
    },
    {
        to: '/admin/contracts',
        label: '계약 관리',
        icon: '📄',
        exact: false,
        desc: '정기 청소 월정액 계약 관리',
    },
    {
        to: '/admin/schedule',
        label: '청소 스케줄',
        icon: '🗓️',
        exact: false,
        desc: '진행 중 계약을 요일별로 확인',
    },
    {
        to: '/admin/quotes',
        label: '견적 관리',
        icon: '🧾',
        exact: false,
        desc: '일회성 특수청소 견적 관리',
    },
    {
        to: '/admin/settlements',
        label: '정산(수금) 관리',
        icon: '💰',
        exact: true,
        desc: '월 청구·입금(수금) 관리',
    },
    {
        to: '/admin/settlements/yearly',
        label: '연간 수금현황',
        icon: '📅',
        exact: false,
        desc: '거래처 x 12개월 수금 매트릭스',
    },
    {
        to: '/admin/tax-invoices',
        label: '세금계산서',
        icon: '📑',
        exact: false,
        desc: '기간 집계·엑셀 출력·발행 기록',
    },
    {
        to: '/admin/expenses',
        label: '지출 관리',
        icon: '⛽',
        exact: false,
        desc: '주유 등 경비 관리',
    },
    {
        to: '/admin/supplies',
        label: '약품 재고',
        icon: '🧴',
        exact: false,
        desc: '약품·소모품 재고와 입출고',
    },
    {
        to: '/admin/pricing/review',
        label: '적정가 재산정',
        icon: '📈',
        exact: false,
        desc: '현재 계약 단가 vs 권장가 비교',
    },
    {
        to: '/admin/pricing/policy',
        label: '단가 정책',
        icon: '📐',
        exact: false,
        desc: '계단청소 권장가 단가·주기계수',
    },
    {
        to: '/admin/company',
        label: '회사 정보',
        icon: '🏛️',
        exact: false,
        desc: '사업자 정보·도장 관리',
    },
]

/** 대시보드 바로가기 카드 — 설명(desc)이 붙은 메뉴만 카드로 노출한다. */
export const ADMIN_SHORTCUTS = ADMIN_MENU.filter((menu) => menu.desc)
