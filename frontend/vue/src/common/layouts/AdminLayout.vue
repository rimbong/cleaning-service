<script setup>
// 관리자 영역 레이아웃 — 좌측 사이드바 + 본문(업무툴 형태).
// 이 레이아웃 아래 라우트는 라우터 가드로 ROLE_ADMIN 인증을 요구한다.
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/common/auth/auth'
import { useNotifyStore } from '@/stores/common/notify/notify'

const auth = useAuthStore()
const notify = useNotifyStore()
const route = useRoute()
const router = useRouter()

// 사이드바 메뉴 정의 (icon 은 이모지로 간단히)
const menu = [
    { to: '/admin', label: '대시보드', icon: '🏠', exact: true },
    { to: '/admin/clients', label: '거래처 관리', icon: '🏢', exact: false },
    { to: '/admin/contracts', label: '계약 관리', icon: '📄', exact: false },
    { to: '/admin/schedule', label: '청소 스케줄', icon: '🗓️', exact: false },
    { to: '/admin/quotes', label: '견적 관리', icon: '🧾', exact: false },
    { to: '/admin/settlements', label: '정산(수금) 관리', icon: '💰', exact: true },
    { to: '/admin/settlements/yearly', label: '연간 수금현황', icon: '📅', exact: false },
    { to: '/admin/tax-invoices', label: '세금계산서', icon: '📑', exact: false },
    { to: '/admin/expenses', label: '지출 관리', icon: '⛽', exact: false },
    { to: '/admin/company', label: '회사 정보', icon: '🏛️', exact: false },
]

// 현재 페이지 제목(라우트 meta.title 우선, 없으면 앱명)
const pageTitle = computed(() => route.meta.title || '관리자')

// 모바일에서 사이드바(메뉴) 열림 여부 — 좁은 화면에서는 오버레이로 토글한다.
const sidebarOpen = ref(false)

function toggleSidebar() {
    sidebarOpen.value = !sidebarOpen.value
}

function closeSidebar() {
    sidebarOpen.value = false
}

// 다른 메뉴로 이동하면 오버레이 사이드바는 닫는다.
watch(() => route.path, () => {
    sidebarOpen.value = false
})

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
        <!-- 모바일 오버레이 배경(사이드바 열렸을 때만) -->
        <div v-if="sidebarOpen" class="admin-backdrop" @click="closeSidebar"></div>

        <!-- 사이드바 -->
        <aside class="admin-side" :class="{ 'is-open': sidebarOpen }">
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
                    @click="closeSidebar"
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
                <button class="admin-hamburger" type="button" aria-label="메뉴 열기" @click="toggleSidebar">☰</button>
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
    height: 100vh;      /* 뷰포트 높이에 고정 — 문서 전체가 스크롤되지 않게 */
    overflow: hidden;   /* 스크롤은 각 영역(사이드바/본문)이 자체적으로 담당 */
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
    height: 100vh;      /* 항상 화면 높이만큼 — 본문이 길어도 함께 늘어나지 않음 */
    overflow-y: auto;   /* 메뉴가 화면보다 길면 사이드바 안에서만 스크롤 */
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
    height: 100vh;      /* 사이드바와 같은 높이 — 내부에서 헤더 고정 + 본문 스크롤 */
    overflow: hidden;
}

.admin-top {
    background: #fff;
    border-bottom: 1px solid var(--border);
    padding: 0 1.5rem;
    height: 56px;
    flex-shrink: 0;     /* 헤더는 항상 고정 높이로 상단에 유지 */
    display: flex;
    align-items: center;
}

.admin-top__title {
    font-size: 1.1rem;
    margin: 0;
}

/* 햄버거 — 모바일에서만 노출(데스크톱은 사이드바 상시 표시) */
.admin-hamburger {
    display: none;
    margin-right: 0.75rem;
    padding: 0.3rem 0.6rem;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: #fff;
    color: var(--text-h);
    font-size: 1.1rem;
    line-height: 1;
    cursor: pointer;
}

/* 모바일 오버레이 배경 — 데스크톱에서는 표시되지 않음 */
.admin-backdrop {
    display: none;
}

.admin-main {
    padding: 1.5rem;
    flex: 1;
    overflow-y: auto;   /* 본문만 스크롤 — 사이드바/헤더는 제자리에 고정 */
    min-height: 0;      /* flex 자식이 줄어들 수 있게(스크롤 컨테이너 성립 조건) */
}

/* ── 모바일: 사이드바를 오버레이로 접기 ── */
@media (max-width: 880px) {
    .admin-hamburger {
        display: inline-flex;
        align-items: center;
    }

    /* 사이드바는 화면 밖에 두고, 열릴 때만 슬라이드 인(오버레이) */
    .admin-side {
        position: fixed;
        top: 0;
        left: 0;
        bottom: 0;
        z-index: 50;
        transform: translateX(-100%);
        transition: transform 0.22s ease;
    }

    .admin-side.is-open {
        transform: translateX(0);
        box-shadow: 0 0 24px rgba(0, 0, 0, 0.25);
    }

    .admin-backdrop {
        display: block;
        position: fixed;
        inset: 0;
        z-index: 40;
        background: rgba(0, 0, 0, 0.4);
    }
}
</style>
