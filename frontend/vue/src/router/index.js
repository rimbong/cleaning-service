import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/home/HomeView.vue'

// 라우트 정의.
//  - 화면은 () => import(...) 지연 로딩(초기 번들 경량화, 실무 권장).
//  - 새 도메인 화면은 views/<도메인>/ 에 두고 아래에 라우트를 추가한다.
const routes = [
    {
        path: '/',
        name: 'home',
        component: HomeView,
    },
    {
        path: '/auth',
        name: 'auth',
        component: () => import('@/views/auth/AuthView.vue'),
    },
]

const router = createRouter({
    // import.meta.env.BASE_URL 은 vite.config 의 base('/app/')를 자동으로 따라간다.
    // → 배포 시 화면 라우팅이 /app/ 하위에서 동작하고, 딥링크는 백엔드가 index.html 로 폴백.
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
})

export default router
