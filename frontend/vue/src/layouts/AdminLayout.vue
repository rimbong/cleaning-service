<script setup>
// 관리자 영역 레이아웃 — 좌측 사이드바 + 본문(업무툴 형태).
// 이 레이아웃 아래 라우트는 라우터 가드로 ROLE_ADMIN 인증을 요구한다.
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth/auth'
import { useNotifyStore } from '@/stores/notify/notify'

const auth = useAuthStore()
const notify = useNotifyStore()
const route = useRoute()
const router = useRouter()

// 사이드바 메뉴 정의 (icon 은 이모지로 간단히)
const menu = [
    { to: '/admin', label: '대시보드', icon: '🏠', exact: true },
    { to: '/admin/clients', label: '거래처 관리', icon: '🏢', exact: false },
    { to: '/admin/contracts', label: '계약 관리', icon: '📄', exact: false },
    { to: '/admin/quotes', label: '견적 관리', icon: '🧾', exact: false },
]

// 현재 페이지 제목(라우트 meta.title 우선, 없으면 앱명)
const pageTitle = computed(() => route.meta.title || '관리자')

/** 메뉴 활성 판단 — exact 면 정확히, 아니면 경로 프리픽스 매칭 */
function isActive(item) {
    if (item.exact) {
        return route.path === item.to
    }
    return route.path === item.to || route.path.startsWith(item.to + '/')
}

async function onLogout() {
    if (!(await notify.confirm('로그아웃 하시겠습니까?'))) {
        return
    }
    await auth.logout()
    notify.toast('로그아웃되었습니다.', { type: 'info' })
    router.push({ name: 'admin-login' })
}
</script>

<template>
    <div class="admin">
        <!-- 사이드바 -->
        <aside class="admin-side">
            <div class="admin-brand">
                <span class="admin-brand__logo">🧹</span>
                <span class="admin-brand__name">CleanHub</span>
                <span class="admin-brand__tag">관리자</span>
            </div>

            <nav class="admin-menu">
                <RouterLink
                    v-for="item in menu"
                    :key="item.to"
                    :to="item.to"
                    class="admin-menu__item"
                    :class="{ 'is-active': isActive(item) }"
                >
                    <span class="admin-menu__icon">{{ item.icon }}</span>
                    <span>{{ item.label }}</span>
                </RouterLink>
            </nav>

            <div class="admin-user">
                <div class="admin-user__info">
                    <span class="admin-user__avatar">{{ (auth.user || '?').charAt(0).toUpperCase() }}</span>
                    <div class="admin-user__meta">
                        <div class="admin-user__name">{{ auth.user || '-' }}</div>
                        <div class="admin-user__role">관리자</div>
                    </div>
                </div>
                <button class="admin-user__logout" type="button" @click="onLogout">로그아웃</button>
            </div>
        </aside>

        <!-- 본문 -->
        <div class="admin-body">
            <header class="admin-top">
                <h1 class="admin-top__title">{{ pageTitle }}</h1>
            </header>
            <main class="admin-main">
                <RouterView />
            </main>
        </div>
    </div>
</template>

<style scoped>
.admin {
    display: flex;
    min-height: 100vh;
}

/* ── 사이드바 ── */
.admin-side {
    width: 240px;
    flex-shrink: 0;
    background: #0f1720;
    color: #cbd5e1;
    display: flex;
    flex-direction: column;
    padding: 1rem 0.75rem;
}

.admin-brand {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.6rem 1.1rem;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-brand__logo {
    font-size: 1.3rem;
}

.admin-brand__name {
    font-weight: 700;
    color: #fff;
    font-size: 1.05rem;
}

.admin-brand__tag {
    font-size: 0.7rem;
    background: var(--primary);
    color: #fff;
    padding: 0.1rem 0.4rem;
    border-radius: 999px;
}

.admin-menu {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    margin-top: 1rem;
    flex: 1;
}

.admin-menu__item {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.6rem 0.7rem;
    border-radius: 8px;
    color: #cbd5e1;
    text-decoration: none;
    font-size: 0.92rem;
}

.admin-menu__item:hover {
    background: rgba(255, 255, 255, 0.06);
    color: #fff;
}

.admin-menu__item.is-active {
    background: var(--primary);
    color: #fff;
    font-weight: 600;
}

.admin-menu__icon {
    width: 1.2rem;
    text-align: center;
}

/* ── 사용자 영역 ── */
.admin-user {
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    padding-top: 0.75rem;
}

.admin-user__info {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.3rem 0.6rem;
}

.admin-user__avatar {
    width: 2rem;
    height: 2rem;
    border-radius: 50%;
    background: var(--primary);
    color: #fff;
    display: grid;
    place-items: center;
    font-weight: 700;
    font-size: 0.9rem;
}

.admin-user__name {
    color: #fff;
    font-size: 0.9rem;
    font-weight: 600;
}

.admin-user__role {
    font-size: 0.72rem;
    color: #94a3b8;
}

.admin-user__logout {
    width: 100%;
    margin-top: 0.5rem;
    padding: 0.45rem;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 8px;
    color: #cbd5e1;
    cursor: pointer;
    font-size: 0.85rem;
}

.admin-user__logout:hover {
    background: rgba(255, 255, 255, 0.06);
    color: #fff;
}

/* ── 본문 ── */
.admin-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: var(--muted);
    min-width: 0;
}

.admin-top {
    background: #fff;
    border-bottom: 1px solid var(--border);
    padding: 0 1.5rem;
    height: 56px;
    display: flex;
    align-items: center;
}

.admin-top__title {
    font-size: 1.1rem;
    margin: 0;
}

.admin-main {
    padding: 1.5rem;
    flex: 1;
}
</style>
