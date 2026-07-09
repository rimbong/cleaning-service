<script setup>
// 대민(고객) 영역 레이아웃 — 상단 헤더 + 본문.
// 견적 요청·진행 조회·후기 등 로그인 없이 접근하는 화면을 감싼다.
import { RouterLink, RouterView } from 'vue-router'
import { useI18n } from 'vue-i18n'

import { setLocale, SUPPORTED_LOCALES } from '@/common/i18n'

const { t, locale } = useI18n()
</script>

<template>
    <div class="public">
        <header class="pub-header">
            <RouterLink to="/" class="pub-brand">{{ t('app.title') }}</RouterLink>

            <nav class="pub-nav">
                <RouterLink to="/">{{ t('nav.home') }}</RouterLink>
                <RouterLink to="/auth">{{ t('nav.auth') }}</RouterLink>
            </nav>

            <div class="pub-right">
                <RouterLink to="/admin" class="pub-admin-link">{{ t('nav.adminArea') }}</RouterLink>
                <div class="pub-lang">
                    <button
                        v-for="lng in SUPPORTED_LOCALES"
                        :key="lng"
                        type="button"
                        class="pub-lang__btn"
                        :class="{ 'is-active': locale === lng }"
                        @click="setLocale(lng)"
                    >
                        {{ t('lang.' + lng) }}
                    </button>
                </div>
            </div>
        </header>

        <main class="pub-main">
            <RouterView />
        </main>
    </div>
</template>

<style scoped>
.pub-header {
    display: flex;
    align-items: center;
    gap: 1.5rem;
    padding: 0.75rem 1.5rem;
    border-bottom: 1px solid var(--border);
}

.pub-brand {
    font-size: 1.1rem;
    font-weight: 700;
    color: var(--primary);
    text-decoration: none;
}

.pub-nav {
    display: flex;
    gap: 1rem;
}

.pub-nav a {
    color: var(--text);
    text-decoration: none;
}

.pub-nav a.router-link-active {
    color: var(--text-h);
    font-weight: 600;
}

.pub-right {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 1rem;
}

.pub-admin-link {
    font-size: 0.85rem;
    color: var(--text);
    text-decoration: none;
    padding: 0.3rem 0.7rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
}

.pub-admin-link:hover {
    border-color: var(--primary);
    color: var(--primary);
}

.pub-lang {
    display: flex;
    gap: 0.4rem;
    font-size: 0.85rem;
}

.pub-lang__btn {
    padding: 0.2rem 0.55rem;
    border: 1px solid var(--border);
    border-radius: 6px;
    background: #fff;
    color: var(--text);
    cursor: pointer;
}

.pub-lang__btn.is-active {
    background: var(--primary);
    color: var(--primary-fg);
    border-color: var(--primary);
}

.pub-main {
    padding: 1.5rem;
    max-width: 1100px;
    margin: 0 auto;
}
</style>
