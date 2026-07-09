// 회사(공급자) 프로필 설정 라우트 — /admin 레이아웃의 children (경로는 /admin 기준 상대).
export default [
    {
        path: 'company',
        name: 'admin-company',
        component: () => import('@/views/admin/company/CompanyView.vue'),
        meta: { title: '회사 정보' },
    },
]
