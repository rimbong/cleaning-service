// 약품/소모품 재고(supply) 관리자 라우트 — /admin 레이아웃의 children (경로는 /admin 기준 상대).
export default [
    {
        path: 'supplies',
        name: 'admin-supplies',
        component: () => import('@/views/admin/supply/SupplyListView.vue'),
        meta: { title: '약품 재고' },
    },
    {
        path: 'supplies/guide',
        name: 'admin-supply-guide',
        component: () => import('@/views/admin/supply/SupplyGuideView.vue'),
        meta: { title: '약품 사용 가이드' },
    },
    {
        path: 'supplies/new',
        name: 'admin-supply-new',
        component: () => import('@/views/admin/supply/SupplyFormView.vue'),
        meta: { title: '품목 등록' },
    },
    {
        path: 'supplies/:id/edit',
        name: 'admin-supply-edit',
        component: () => import('@/views/admin/supply/SupplyFormView.vue'),
        meta: { title: '품목 수정' },
        props: true,
    },
    {
        path: 'supplies/:id/history',
        name: 'admin-supply-history',
        component: () => import('@/views/admin/supply/SupplyHistoryView.vue'),
        meta: { title: '입출고 이력' },
        props: true,
    },
]
