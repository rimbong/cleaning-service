<script setup>
// 관리자 대시보드 — 현재는 진입점 안내. (통계/집계는 이후 단계에서 추가)
import { RouterLink } from 'vue-router'

import { useAuthStore } from '@/stores/common/auth/auth'
import { ADMIN_SHORTCUTS } from '@/common/layouts/adminMenu'

const auth = useAuthStore()

// 바로가기 카드 — 사이드바와 같은 정의를 쓴다(adminMenu.js).
// 여기에 목록을 따로 두면 도메인을 추가할 때 사이드바만 고치고 대시보드를 잊게 된다.
const shortcuts = ADMIN_SHORTCUTS
</script>

<template>
    <section class="dash">
        <div class="dash-hello">
            <h2>안녕하세요, {{ auth.user }}님 👋</h2>
            <p>CleanHub 관리자 대시보드입니다.</p>
        </div>

        <div class="dash-cards">
            <RouterLink v-for="s in shortcuts" :key="s.to" :to="s.to" class="dash-card">
                <span class="dash-card__icon">{{ s.icon }}</span>
                <div>
                    <div class="dash-card__title">{{ s.label }}</div>
                    <div class="dash-card__desc">{{ s.desc }}</div>
                </div>
            </RouterLink>
        </div>

        <p class="dash-note">※ 정산·대시보드 통계는 다음 단계에서 추가됩니다.</p>
    </section>
</template>

<style scoped>
.dash-hello h2 {
    margin: 0 0 0.25rem;
}

.dash-hello p {
    color: var(--text);
    margin: 0 0 1.5rem;
}

.dash-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 1rem;
}

.dash-card {
    display: flex;
    align-items: center;
    gap: 0.9rem;
    padding: 1.1rem;
    background: #fff;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    text-decoration: none;
    color: inherit;
    transition: border-color 0.15s;
}

.dash-card:hover {
    border-color: var(--primary);
}

.dash-card__icon {
    font-size: 1.6rem;
}

.dash-card__title {
    font-weight: 600;
    color: var(--text-h);
}

.dash-card__desc {
    font-size: 0.82rem;
    color: var(--text);
}

.dash-note {
    margin-top: 1.5rem;
    font-size: 0.82rem;
    color: var(--text);
}
</style>
