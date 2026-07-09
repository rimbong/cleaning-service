<script setup>
// 관리자 로그인 — 세션 로그인 → 관리자 권한 확인 → JWT 발급 → 관리자 홈으로.
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/common/stores/auth/auth'
import { useNotifyStore } from '@/common/stores/notify/notify'

const auth = useAuthStore()
const notify = useNotifyStore()
const route = useRoute()
const router = useRouter()

// 개발 편의 기본값(운영에선 비움) — admin 계정은 Flyway V2 시드
const form = reactive({
    username: 'admin',
    password: 'admin1234',
    remember: true,
})
const loading = ref(false)

async function onSubmit() {
    loading.value = true
    try {
        await auth.sessionLogin(form.username, form.password)
        if (!auth.isAdmin) {
            // 관리자 아님 → 세션 정리하고 거부
            await auth.logout()
            notify.bar('관리자 권한이 없는 계정입니다.', { color: 'red' })
            return
        }
        await auth.issueTokens(form.remember) // 이후 /api/admin/** 호출용 Bearer 확보 + 자동로그인 쿠키
        notify.toast('로그인되었습니다.', { type: 'success' })
        // 원래 가려던 곳(redirect 쿼리)이 있으면 그리로, 없으면 관리자 홈
        const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/admin'
        router.replace(redirect)
    } catch (e) {
        notify.bar(e.response?.data?.message ?? '로그인에 실패했습니다.', { color: 'red' })
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="login">
        <div class="login-card">
            <div class="login-brand">
                <span class="login-brand__logo">🧹</span>
                <span class="login-brand__name">CleanHub</span>
            </div>
            <h1 class="login-title">관리자 로그인</h1>
            <p class="login-desc">견적·정산 관리 시스템</p>

            <form class="login-form" @submit.prevent="onSubmit">
                <label class="login-field">
                    <span>아이디</span>
                    <input v-model="form.username" autocomplete="username" placeholder="관리자 아이디" />
                </label>
                <label class="login-field">
                    <span>비밀번호</span>
                    <input v-model="form.password" type="password" autocomplete="current-password" placeholder="비밀번호" />
                </label>
                <label class="login-remember">
                    <input v-model="form.remember" type="checkbox" />
                    <span>로그인 유지</span>
                </label>
                <button class="login-btn" type="submit" :disabled="loading">
                    {{ loading ? '로그인 중…' : '로그인' }}
                </button>
            </form>
        </div>
    </div>
</template>

<style scoped>
.login {
    min-height: 100vh;
    display: grid;
    place-items: center;
    background: linear-gradient(135deg, #0f1720 0%, #0d9488 160%);
    padding: 1.5rem;
}

.login-card {
    width: 100%;
    max-width: 380px;
    background: #fff;
    border-radius: 16px;
    padding: 2rem;
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}

.login-brand {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    justify-content: center;
    margin-bottom: 1.25rem;
}

.login-brand__logo {
    font-size: 1.5rem;
}

.login-brand__name {
    font-weight: 700;
    font-size: 1.2rem;
    color: var(--primary);
}

.login-title {
    text-align: center;
    font-size: 1.15rem;
    margin: 0 0 0.25rem;
}

.login-desc {
    text-align: center;
    color: var(--text);
    font-size: 0.85rem;
    margin: 0 0 1.5rem;
}

.login-form {
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
}

.login-field {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    font-size: 0.85rem;
    color: var(--text);
}

.login-field input {
    padding: 0.6rem 0.7rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    font: inherit;
    color: var(--text-h);
}

.login-field input:focus {
    outline: none;
    border-color: var(--primary);
    box-shadow: 0 0 0 3px var(--primary-soft);
}

.login-remember {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.85rem;
    color: var(--text);
    cursor: pointer;
}

.login-remember input {
    cursor: pointer;
}

.login-btn {
    margin-top: 0.5rem;
    padding: 0.7rem;
    background: var(--primary);
    color: var(--primary-fg);
    border: none;
    border-radius: var(--radius);
    font-size: 0.95rem;
    font-weight: 600;
    cursor: pointer;
}

.login-btn:hover:not(:disabled) {
    background: var(--primary-hover);
}

.login-btn:disabled {
    opacity: 0.6;
    cursor: default;
}
</style>
