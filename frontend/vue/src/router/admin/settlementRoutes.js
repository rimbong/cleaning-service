// 정산(월 청구/수금) 관리자 라우트 — /admin 레이아웃의 children (경로는 /admin 기준 상대).
export default [
    {
        path: 'settlements',
        name: 'admin-settlements',
        component: () => import('@/views/admin/settlement/SettlementView.vue'),
        meta: { title: '정산(수금) 관리' },
    },
]
