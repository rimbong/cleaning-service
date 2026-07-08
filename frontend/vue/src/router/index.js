import { createRouter, createWebHistory } from 'vue-router'

import { useAuthStore } from '@/stores/auth/auth'

import publicRoutes from './public'
import adminRoutes from './admin'

// 라우트는 영역(대민/관리자)으로 최상위 분리하고, 관리자 하위는 도메인별 모듈로 나눈다.
//  - 대민(고객) 영역: publicRoutes (PublicLayout 아래, 로그인 불필요)
//  - 관리자 영역: adminRoutes (/admin/**, ROLE_ADMIN 요구 — 아래 가드)
//  - 화면은 각 모듈에서 () => import(...) 지연 로딩. base 는 vite base('/')를 따른다.
const routes = [
    ...publicRoutes,
    ...adminRoutes,
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
