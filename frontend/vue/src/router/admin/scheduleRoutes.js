// 청소 스케줄 관리자 라우트 — /admin 레이아웃의 children (경로는 /admin 기준 상대).
export default [
    {
        path: 'schedule',
        name: 'admin-schedule',
        component: () => import('@/views/admin/schedule/WeeklyScheduleView.vue'),
        meta: { title: '청소 스케줄' },
    },
]
