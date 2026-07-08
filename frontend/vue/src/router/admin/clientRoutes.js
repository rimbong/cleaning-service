// 거래처(client) 관리자 라우트 — /admin 레이아웃의 children 으로 조합됨(경로는 /admin 기준 상대).
export default [
    {
        path: 'clients',
        name: 'admin-clients',
        component: () => import('@/views/admin/client/ClientListView.vue'),
        meta: { title: '거래처 관리' },
    },
    {
        path: 'clients/new',
        name: 'admin-client-new',
        component: () => import('@/views/admin/client/ClientFormView.vue'),
        meta: { title: '거래처 등록' },
    },
    {
        path: 'clients/:id',
        name: 'admin-client-detail',
        component: () => import('@/views/admin/client/ClientDetailView.vue'),
        meta: { title: '거래처 상세' },
        props: true,
    },
    {
        path: 'clients/:id/edit',
        name: 'admin-client-edit',
        component: () => import('@/views/admin/client/ClientFormView.vue'),
        meta: { title: '거래처 수정' },
        props: true,
    },
]
