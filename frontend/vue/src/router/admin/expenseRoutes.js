// 지출(expense) 관리자 라우트 — /admin 레이아웃의 children (경로는 /admin 기준 상대).
export default [
    {
        path: 'expenses',
        name: 'admin-expenses',
        component: () => import('@/views/admin/expense/ExpenseListView.vue'),
        meta: { title: '지출 관리' },
    },
    {
        path: 'expenses/new',
        name: 'admin-expense-new',
        component: () => import('@/views/admin/expense/ExpenseFormView.vue'),
        meta: { title: '지출 등록' },
    },
    {
        path: 'expenses/:id/edit',
        name: 'admin-expense-edit',
        component: () => import('@/views/admin/expense/ExpenseFormView.vue'),
        meta: { title: '지출 수정' },
        props: true,
    },
]
