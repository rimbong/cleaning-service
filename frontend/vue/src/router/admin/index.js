// 관리자 영역 라우트 조합.
//  - /admin/login : 레이아웃 밖 + 인증 불필요(관리자 로그인 화면)
//  - /admin/**    : AdminLayout 아래, meta.requiresAdmin 으로 ROLE_ADMIN 요구(가드는 router/index.js).
//  - 도메인별 children 은 modules(clientRoutes/contractRoutes/quoteRoutes)로 분리해 조합한다.
//    새 도메인 추가 = 모듈 파일 하나 만들고 아래 children 에 `...xxxRoutes` 한 줄 추가.
import AdminLayout from '@/layouts/AdminLayout.vue'

import clientRoutes from './clientRoutes'
import contractRoutes from './contractRoutes'
import quoteRoutes from './quoteRoutes'

export default [
    {
        path: '/admin/login',
        name: 'admin-login',
        component: () => import('@/views/admin/AdminLoginView.vue'),
    },
    {
        path: '/admin',
        component: AdminLayout,
        meta: { requiresAdmin: true },
        children: [
            {
                path: '',
                name: 'admin-home',
                component: () => import('@/views/admin/AdminDashboardView.vue'),
                meta: { title: '대시보드' },
            },
            ...clientRoutes,
            ...contractRoutes,
            ...quoteRoutes,
        ],
    },
]
