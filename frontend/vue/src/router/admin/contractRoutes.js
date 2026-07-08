// 계약(contract) 관리자 라우트 — /admin 레이아웃의 children 으로 조합됨(경로는 /admin 기준 상대).
export default [
    {
        path: 'contracts',
        name: 'admin-contracts',
        component: () => import('@/views/admin/contract/ContractListView.vue'),
        meta: { title: '계약 관리' },
    },
    {
        path: 'contracts/new',
        name: 'admin-contract-new',
        component: () => import('@/views/admin/contract/ContractFormView.vue'),
        meta: { title: '계약 등록' },
    },
    {
        path: 'contracts/:id',
        name: 'admin-contract-detail',
        component: () => import('@/views/admin/contract/ContractDetailView.vue'),
        meta: { title: '계약 상세' },
        props: true,
    },
    {
        path: 'contracts/:id/edit',
        name: 'admin-contract-edit',
        component: () => import('@/views/admin/contract/ContractFormView.vue'),
        meta: { title: '계약 수정' },
        props: true,
    },
]
