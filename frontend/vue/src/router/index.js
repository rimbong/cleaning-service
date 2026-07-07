import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth/auth'

import PublicLayout from '@/layouts/PublicLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import HomeView from '@/views/home/HomeView.vue'

// 라우트 정의 — 두 영역으로 분리한다.
//  - 대민(고객) 영역: PublicLayout 아래, 로그인 불필요.
//  - 관리자 영역: AdminLayout 아래(/admin/**), meta.requiresAdmin 으로 ROLE_ADMIN 요구.
//    관리자 로그인(/admin/login)만 예외로 레이아웃 밖 + 인증 불필요.
//  - 화면은 () => import(...) 지연 로딩(초기 번들 경량화). base 는 vite base('/app/')를 따른다.
const routes = [
    // ── 대민 영역 ──
    {
        path: '/',
        component: PublicLayout,
        children: [
            {
                path: '',
                name: 'home',
                component: HomeView,
            },
            {
                path: 'auth',
                name: 'auth',
                component: () => import('@/views/auth/AuthView.vue'),
            },
        ],
    },

    // ── 관리자 로그인 (레이아웃 밖, 인증 불필요) ──
    {
        path: '/admin/login',
        name: 'admin-login',
        component: () => import('@/views/admin/AdminLoginView.vue'),
    },

    // ── 관리자 영역 (ROLE_ADMIN 필요) ──
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
        ],
    },
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
})

// 관리자 영역 가드 — 미인증(관리자 토큰 없음)이면 로그인 화면으로.
//  ※ 토큰은 메모리 보관이라 새로고침 시 사라진다 → 재로그인 유도(현재 설계).
router.beforeEach((to) => {
    if (to.matched.some((r) => r.meta.requiresAdmin)) {
        const auth = useAuthStore()
        if (!auth.hasTokens || !auth.isAdmin) {
            return { name: 'admin-login', query: { redirect: to.fullPath } }
        }
    }
    return true
})

export default router
