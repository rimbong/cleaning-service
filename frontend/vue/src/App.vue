<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { useI18n } from 'vue-i18n'

import { setLocale, SUPPORTED_LOCALES } from '@/i18n'
import NotifyHost from '@/components/notify/NotifyHost.vue'

const { t, locale } = useI18n()
</script>

<template>
    <header class="app-header">
        <h1 class="app-title">{{ t('app.title') }}</h1>

        <nav class="app-nav">
            <RouterLink to="/">{{ t('nav.home') }}</RouterLink>
            <RouterLink to="/auth">{{ t('nav.auth') }}</RouterLink>
        </nav>

        <div class="app-lang">
            <span class="app-lang__label">{{ t('lang.label') }}:</span>
            <button
                v-for="lng in SUPPORTED_LOCALES"
                :key="lng"
                type="button"
                class="app-lang__btn"
                :class="{ 'is-active': locale === lng }"
                @click="setLocale(lng)"
            >
                {{ t('lang.' + lng) }}
            </button>
        </div>
    </header>

    <main class="app-main">
        <RouterView />
    </main>

    <!-- 전역 알림 표시 컴포넌트(알럿/컨펌/토스트/스피너) — 앱에서 유일하게 여기 한 번만 둔다 -->
    <NotifyHost />
</template>

<style scoped>
.app-header {
    display: flex;
    align-items: center;
    gap: 1.5rem;
    padding: 0.75rem 1.5rem;
    border-bottom: 1px solid var(--border);
}

.app-title {
    font-size: 1.05rem;
    margin: 0;
    color: var(--text-h);
}

.app-nav {
    display: flex;
    gap: 1rem;
}

.app-nav a {
    color: var(--text);
    text-decoration: none;
}

.app-nav a.router-link-active {
    color: var(--text-h);
    font-weight: 600;
}

.app-lang {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.85rem;
}

.app-lang__label {
    color: var(--text);
}

.app-lang__btn {
    padding: 0.2rem 0.55rem;
    border: 1px solid var(--border);
    border-radius: 6px;
    background: #fff;
    color: var(--text);
    cursor: pointer;
}

.app-lang__btn.is-active {
    background: var(--text-h);
    color: #fff;
    border-color: var(--text-h);
}

.app-main {
    padding: 1.5rem;
}
</style>
