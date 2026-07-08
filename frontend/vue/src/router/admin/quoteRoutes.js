// 견적(quote) 관리자 라우트 — /admin 레이아웃의 children 으로 조합됨(경로는 /admin 기준 상대).
export default [
    {
        path: 'quotes',
        name: 'admin-quotes',
        component: () => import('@/views/admin/quote/QuoteListView.vue'),
        meta: { title: '견적 관리' },
    },
    {
        path: 'quotes/new',
        name: 'admin-quote-new',
        component: () => import('@/views/admin/quote/QuoteFormView.vue'),
        meta: { title: '견적 등록' },
    },
    {
        path: 'quotes/:id',
        name: 'admin-quote-detail',
        component: () => import('@/views/admin/quote/QuoteDetailView.vue'),
        meta: { title: '견적 상세' },
        props: true,
    },
    {
        path: 'quotes/:id/edit',
        name: 'admin-quote-edit',
        component: () => import('@/views/admin/quote/QuoteFormView.vue'),
        meta: { title: '견적 수정' },
        props: true,
    },
]
