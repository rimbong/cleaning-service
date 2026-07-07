import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

import { authService } from '@/services/auth/authService'
import { configureAuth } from '@/plugins/http/axios'

/**
 * 인증 전역 상태 스토어.
 *
 * ── 왜 전역(store)인가 ──
 * 로그인 상태·토큰은 "앱 전체가 공유하는 하나의 상태"다(네비 표시, 모든 API 호출이 사용).
 * 화면 전용 데이터(ref 로컬)와 달리 이런 것이 Pinia 의 올바른 용도.
 *
 * ── 전체 흐름(백엔드 auth 테스트케이스와 1:1) ──
 *  sessionLogin()  → 세션 존 로그인(쿠키는 브라우저 자동 관리)
 *  issueTokens()   → 세션 인증으로 JWT(access/refresh) 발급 → 이 스토어에 보관
 *  이후 /api/**    → axios 요청 인터셉터가 Bearer 자동 첨부
 *  access 만료     → axios 응답 인터셉터가 refreshTokens() 로 자동 갱신 + 재시도
 *  logout()        → JWT 폐기(/api/auth/logout) + 세션 종료(/auth/logout)
 *
 * ── 토큰 보관 위치(데모 선택) ──
 * 여기서는 메모리(스토어)에만 둔다 → 새로고침하면 사라짐(다시 발급받으면 됨).
 * localStorage 는 XSS 에 취약하고, 쿠키는 CSRF 고려가 필요 — 실서비스에서는
 * "refresh 는 HttpOnly 쿠키 + access 는 메모리" 조합이 흔히 권장된다.
 */
export const useAuthStore = defineStore('auth', () => {
    // ── 상태 ──
    const user = ref(null)          // 세션 로그인 사용자명(null = 미로그인)
    const accessToken = ref(null)   // JWT access (짧은 수명)
    const refreshToken = ref(null)  // JWT refresh (긴 수명, 갱신용)

    const isLoggedIn = computed(() => user.value !== null)
    const hasTokens = computed(() => accessToken.value !== null)

    // axios 인터셉터에 토큰 접근/갱신 함수 등록(스토어 최초 사용 시 1회 실행).
    // 이 등록 덕분에 컴포넌트는 Bearer 첨부·401 갱신을 전혀 신경 쓰지 않는다.
    configureAuth({
        getAccessToken: () => accessToken.value,
        refresh: refreshTokens,
    })

    /** ① 세션 로그인 — 성공 시 user 설정(쿠키는 브라우저가 보관) */
    async function sessionLogin(username, password) {
        const res = await authService.sessionLogin(username, password)
        user.value = res.data.data.username
        return res.data
    }

    /** ② 세션 인증으로 JWT 발급 — 스토어에 보관 */
    async function issueTokens() {
        const res = await authService.issueTokens()
        accessToken.value = res.data.data.accessToken
        refreshToken.value = res.data.data.refreshToken
        return res.data
    }

    // 진행 중인 갱신 Promise(single-flight 용). 반응형일 필요 없어 ref 로 만들지 않는다.
    let refreshPromise = null

    /**
     * ③ access 갱신 — 응답 인터셉터가 401 을 만나면 자동 호출한다(수동 호출도 가능).
     *
     * single-flight: access 만료 상태에서 /api/** 요청 여러 개가 "동시에" 401 을 맞으면
     * 요청 수만큼 갱신이 병렬 호출될 수 있다 → 첫 호출의 Promise 를 공유해
     * 갱신은 1번만 수행하고 나머지는 그 결과를 기다린다.
     * (백엔드를 refresh 회전 방식으로 바꿔도 "두 번째 갱신이 401 → 토큰 전체 삭제"로
     *  로그인이 튕기는 경쟁 버그가 생기지 않도록 하는 안전장치이기도 하다)
     *
     * @returns {Promise<boolean>} 성공 여부(false = refresh 만료/폐기 → 재로그인 필요)
     */
    function refreshTokens() {
        if (!refreshToken.value) {
            return Promise.resolve(false)
        }
        if (refreshPromise) {
            return refreshPromise // 이미 갱신 중 → 같은 결과를 공유
        }
        refreshPromise = doRefresh().finally(() => {
            refreshPromise = null // 끝나면 다음 만료를 위해 초기화
        })
        return refreshPromise
    }

    /** 실제 갱신 1회 수행(refreshTokens 의 single-flight 안쪽) */
    async function doRefresh() {
        try {
            const res = await authService.refresh(refreshToken.value)
            accessToken.value = res.data.data.accessToken
            // 응답의 refresh 도 항상 저장 — 백엔드 회전(rotation) 옵션이 켜져 있으면
            // 새 토큰이, 꺼져 있으면 같은 토큰이 온다(두 모드 모두 호환).
            if (res.data.data.refreshToken) {
                refreshToken.value = res.data.data.refreshToken
            }
            return true
        } catch (e) {
            clearTokens() // 만료/폐기된 refresh → 토큰 상태 정리(재로그인 유도)
            return false
        }
    }

    /** ④ 로그아웃 — JWT 폐기 + 세션 종료를 각각 수행 */
    async function logout() {
        if (refreshToken.value) {
            try {
                await authService.revokeTokens(refreshToken.value)
            } catch (e) {
                // 폐기 실패해도 로컬 상태는 정리하고 진행(이미 만료된 경우 등)
            }
        }
        try {
            await authService.sessionLogout()
        } finally {
            user.value = null
            clearTokens()
        }
    }

    /** 토큰 상태만 비움(세션과 별개) */
    function clearTokens() {
        accessToken.value = null
        refreshToken.value = null
    }

    /** (데모용) access 를 일부러 훼손 — 인터셉터의 자동 갱신을 눈으로 확인하는 버튼에서 사용 */
    function corruptAccessToken() {
        accessToken.value = 'broken.' + (accessToken.value || 'token')
    }

    return {
        user,
        accessToken,
        refreshToken,
        isLoggedIn,
        hasTokens,
        sessionLogin,
        issueTokens,
        refreshTokens,
        logout,
        clearTokens,
        corruptAccessToken,
    }
})
