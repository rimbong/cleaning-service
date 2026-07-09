// 대민(고객) 영역 라우트 — PublicLayout 아래, 로그인 불필요.
//  향후 대민 기능(견적요청·후기 등)이 늘면 여기(또는 modules)로 추가한다.
import PublicLayout from '@/common/layouts/PublicLayout.vue'
import HomeView from '@/views/public/home/HomeView.vue'

export default [
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
                component: () => import('@/views/public/auth/AuthView.vue'),
            },
        ],
    },
]
