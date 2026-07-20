// 권장가 산정(pricing) 관리자 라우트 — /admin 레이아웃의 children (경로는 /admin 기준 상대).
export default [
    {
        path: 'pricing/policy',
        name: 'admin-pricing-policy',
        component: () => import('@/views/admin/pricing/PricingPolicyView.vue'),
        meta: { title: '단가 정책' },
    },
    {
        path: 'pricing/review',
        name: 'admin-pricing-review',
        component: () => import('@/views/admin/pricing/PriceReviewView.vue'),
        meta: { title: '적정가 재산정' },
    },
]
