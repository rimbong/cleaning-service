<script setup>
// 인증(세션+JWT) 연습 화면.
//
// 허브의 /test/auth 스모크 페이지와 같은 백엔드 흐름을 "Vue 실무 패턴"으로 구현:
//  - 상태는 Pinia auth 스토어(전역) — 화면은 스토어 액션만 호출
//  - Bearer 첨부·401 자동 갱신은 axios 인터셉터가 처리 → 이 화면엔 그 코드가 "없다"는 게 포인트
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { useAuthStore } from '@/stores/common/auth/auth'
import { useNotifyStore } from '@/stores/common/notify/notify'
import { authService } from '@/services/common/auth/authService'

const { t } = useI18n()
const auth = useAuthStore()
const notify = useNotifyStore()

// 로그인 폼 — 데모 계정 기본값
const form = reactive({
    username: 'user1',
    password: '1234',
})

// 화면 하단 진행 로그(이 화면 전용 상태라 로컬 ref)
const logs = ref([])

function log(title, data) {
    logs.value.unshift({
        id: Date.now() + Math.random(),
        time: new Date().toLocaleTimeString(),
        title,
        body: typeof data === 'string' ? data : JSON.stringify(data, null, 2),
    })
}

/** ① 세션 로그인 */
async function doLogin() {
    try {
        const body = await auth.sessionLogin(form.username, form.password)
        notify.toast(t('auth.msg.loggedIn'), { type: 'success' })
        log('① POST /auth/login', body)
    } catch (e) {
        notify.bar(e.response?.data?.message ?? t('auth.msg.loginFailed'), { color: 'red' })
        log('① 로그인 실패', e.response?.data ?? e.message)
    }
}

/** ② 세션 확인 */
async function doMe() {
    try {
        const res = await authService.me()
        log('② GET /auth/api/me', res.data)
    } catch (e) {
        log('② 실패(로그인 필요)', e.response?.data ?? e.message)
    }
}

/** ③ JWT 발급 — 세션이 인증 증명(비밀번호 재입력 없음) */
async function doIssue() {
    try {
        const body = await auth.issueTokens()
        notify.toast(t('auth.msg.issued'), { type: 'success' })
        log('③ POST /auth/api/token', body)
    } catch (e) {
        notify.bar(t('auth.msg.needLogin'), { color: 'red' })
        log('③ 발급 실패(세션 로그인 먼저)', e.response?.data ?? e.message)
    }
}

/** ④ JWT 존 API 호출 — Bearer 는 인터셉터가 자동 첨부 */
async function doApi() {
    try {
        const res = await authService.hello()
        log('④ GET /api/hello', res.data)
    } catch (e) {
        log('④ 실패(토큰 발급 먼저)', e.response?.data ?? e.message)
    }
}

/**
 * ④-2 관리자 API — ROLE_ADMIN 필요.
 *  user1(ROLE_USER) 로 로그인·발급했다면 403, admin1(ROLE_ADMIN)이면 200.
 *  화면 코드는 동일한데 "누구로 로그인했는가"에 따라 결과가 갈린다(= 인가).
 */
async function doAdminApi() {
    try {
        const res = await authService.adminHello()
        notify.toast(t('auth.msg.adminOk'), { type: 'success' })
        log('④-2 GET /api/admin', res.data)
    } catch (e) {
        const status = e.response?.status
        notify.bar(status === 403 ? t('auth.msg.forbidden') : t('auth.msg.needTokens'),
            { color: status === 403 ? 'red' : 'yellow' })
        log('④-2 GET /api/admin 실패(' + status + ')', e.response?.data ?? e.message)
    }
}

/**
 * ⑤ 자동 갱신 데모 — access 를 일부러 훼손하고 API 를 부른다.
 *  흐름: 훼손된 토큰 → 401 → [응답 인터셉터] refresh 로 새 access → 원래 요청 자동 재시도 → 200
 *  이 함수 안에 갱신 코드가 전혀 없는데도 성공하는 것이 인터셉터의 증명이다.
 */
async function doAutoRefreshDemo() {
    if (!auth.hasTokens) {
        notify.bar(t('auth.msg.needTokens'), { color: 'yellow' })
        return
    }
    auth.corruptAccessToken()
    log('⑤ access 훼손', auth.accessToken.substring(0, 30) + '…')
    try {
        const res = await authService.hello()
        notify.toast(t('auth.msg.autoRefreshed'), { type: 'success' })
        log('⑤ GET /api/hello (인터셉터가 401→갱신→재시도)', res.data
            + '\n\n새 access: ' + auth.accessToken.substring(0, 30) + '…')
    } catch (e) {
        log('⑤ 실패(refresh 도 무효 → 재로그인 필요)', e.response?.data ?? e.message)
    }
}

/** ⑥ 로그아웃 — JWT 폐기 + 세션 종료 */
async function doLogout() {
    if (!(await notify.confirm(t('auth.msg.confirmLogout')))) {
        return
    }
    try {
        await auth.logout()
        notify.toast(t('auth.msg.loggedOut'), { type: 'info' })
        log('⑥ 로그아웃(토큰 폐기 + 세션 종료)', 'OK')
    } catch (e) {
        // 백엔드가 죽어 있어도 로컬 상태는 스토어(finally)에서 정리됨 — 안내만
        notify.bar(t('auth.msg.loggedOut'), { color: 'yellow' })
        log('⑥ 로그아웃(서버 응답 실패 — 로컬 상태만 정리됨)', e.message)
    }
}
</script>

<template>
    <section class="auth">
        <h2>{{ t('auth.heading') }}</h2>
        <p class="desc">{{ t('auth.desc') }}</p>

        <!-- 현재 상태 — 스토어 값을 그대로 구독(반응형) -->
        <div class="status">
            <span class="chip" :class="auth.isLoggedIn ? 'chip--on' : ''">
                {{ t('auth.session') }}: {{ auth.isLoggedIn ? auth.user : t('auth.none') }}
            </span>
            <span class="chip" :class="auth.hasTokens ? 'chip--on' : ''">
                JWT: {{ auth.hasTokens ? auth.accessToken.substring(0, 24) + '…' : t('auth.none') }}
            </span>
        </div>

        <!-- ① 로그인 폼 -->
        <form class="login-form" @submit.prevent="doLogin">
            <input v-model="form.username" :placeholder="t('auth.username')" autocomplete="username" />
            <input v-model="form.password" type="password" :placeholder="t('auth.password')" autocomplete="current-password" />
            <button class="btn btn--primary" type="submit">{{ t('auth.login') }}</button>
        </form>

        <!-- ②~⑥ 단계 버튼 -->
        <div class="btns">
            <button class="btn" type="button" @click="doMe">{{ t('auth.me') }}</button>
            <button class="btn" type="button" @click="doIssue">{{ t('auth.issue') }}</button>
            <button class="btn" type="button" @click="doApi">{{ t('auth.callApi') }}</button>
            <button class="btn" type="button" @click="doAdminApi">{{ t('auth.callAdmin') }}</button>
            <button class="btn" type="button" @click="doAutoRefreshDemo">{{ t('auth.autoRefresh') }}</button>
            <button class="btn btn--danger" type="button" @click="doLogout">{{ t('auth.logout') }}</button>
        </div>

        <!-- 진행 로그 -->
        <h3 class="log-title">{{ t('auth.log') }}</h3>
        <div v-if="!logs.length" class="hint">-</div>
        <pre v-for="item in logs" :key="item.id" class="log-item">[{{ item.time }}] {{ item.title }}
{{ item.body }}</pre>
    </section>
</template>

<style scoped>
.auth {
    max-width: 820px;
}

.desc {
    color: var(--text);
    margin: 0.25rem 0 1rem;
}

.status {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1rem;
}

.chip {
    padding: 0.3rem 0.75rem;
    border: 1px solid var(--border);
    border-radius: 999px;
    font-size: 0.85rem;
    color: var(--text);
    background: #f8f8fa;
}

.chip--on {
    background: #ecfdf5;
    border-color: #15803d;
    color: #15803d;
    font-weight: 600;
}

.login-form {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 0.75rem;
}

.login-form input {
    padding: 0.45rem 0.6rem;
    border: 1px solid var(--border);
    border-radius: 8px;
    font: inherit;
}

.btns {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1.25rem;
}

.btn {
    padding: 0.45rem 0.9rem;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: #f8f8fa;
    cursor: pointer;
}

.btn--primary {
    background: var(--text-h);
    border-color: var(--text-h);
    color: #fff;
}

.btn--danger {
    color: #b91c1c;
    border-color: #fca5a5;
}

.log-title {
    margin: 0 0 0.5rem;
    font-size: 1rem;
}

.log-item {
    padding: 0.75rem 1rem;
    margin: 0 0 0.5rem;
    background: #0f172a;
    color: #e2e8f0;
    border-radius: 8px;
    font-size: 0.8rem;
    white-space: pre-wrap;
    word-break: break-all;
}

.hint {
    color: var(--text);
}
</style>
