import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

import { authService } from '@/services/auth/authService'
import { configureAuth } from '@/plugins/http/axios'

/**
 * 인증 전역 상태 스토어.
 *
 * ── 인증 구조 (세션 앵커 + JWT API + refresh HttpOnly 쿠키) ──
 *  - 세션(HttpOnly JSESSIONID): 브라우저가 열려 있는 동안의 로그인 상태(서버 보관).
 *  - access(JWT, 메모리): /api/** 호출용 Bearer. 이 스토어에만 있고 새로고침 시 사라진다.
 *  - refresh(HttpOnly 쿠키 + DB): "로그인 유지(자동로그인)"의 장기 신분증.
 *    → 자바스크립트가 못 읽는다(XSS 안전). 브라우저가 자동 전송하므로 여기서 다루지 않는다.
 *
 * ── 흐름 ──
 *  login()    → 세션 로그인 + JWT 발급(access=메모리, refresh=쿠키)
 *  restore()  → 앱 시작/새로고침/재시작 시 refresh 쿠키로 access 재발급(자동로그인)
 *  이후 /api/** → axios 인터셉터가 Bearer 자동 첨부, 401 이면 refreshTokens()로 자동 갱신
 *  logout()   → refresh 폐기(DB+쿠키) + 세션 종료
 */
export const useAuthStore = defineStore('auth', () => {
    // ── 상태 ──
    const user = ref(null)          // 로그인 사용자명(null = 미로그인)
    const roles = ref([])           // 권한 목록(예: ['ROLE_ADMIN'])
    const accessToken = ref(null)   // JWT access (메모리 전용)
    // "로그인 유지" 선택 — 갱신(회전) 시 재발급 쿠키의 영속 여부를 유지하는 데 쓴다.
    //  기본 true: 앱 재시작 후 복원(restore) 시엔 "쿠키가 살아남았다=영속이었다"는 뜻이므로 true 가 맞다.
    const rememberMe = ref(true)

    const isLoggedIn = computed(() => user.value !== null)
    const hasTokens = computed(() => accessToken.value !== null)
    const isAdmin = computed(() => roles.value.includes('ROLE_ADMIN'))

    // axios 인터셉터에 토큰 접근/갱신 함수 등록(순환 import 회피).
    configureAuth({
        getAccessToken: () => accessToken.value,
        refresh: refreshTokens,
    })

    /** 발급/갱신 응답(AuthResult: {accessToken, username, roles})을 스토어에 반영 */
    function applyAuth(data) {
        accessToken.value = data.accessToken
        user.value = data.username
        roles.value = data.roles || []
    }

    /**
     * 로그인 = 세션 로그인 → JWT 발급(access=메모리, refresh=쿠키).
     * @param {string} username
     * @param {string} password
     * @param {boolean} remember "로그인 유지" 여부
     */
    async function login(username, password, remember = false) {
        await authService.sessionLogin(username, password)
        rememberMe.value = remember
        const res = await authService.issueTokens(remember)
        applyAuth(res.data.data)
        return res.data
    }

    /** 세션 로그인만(연습 화면용) — user 설정, 권한은 me()로 조회 */
    async function sessionLogin(username, password) {
        const res = await authService.sessionLogin(username, password)
        user.value = res.data.data.username
        try {
            const meRes = await authService.me()
            roles.value = meRes.data.data.roles || []
        } catch (e) {
            roles.value = []
        }
        return res.data
    }

    /** JWT 발급(연습 화면용) — 세션 인증 상태에서 호출 */
    async function issueTokens(remember = false) {
        rememberMe.value = remember
        const res = await authService.issueTokens(remember)
        applyAuth(res.data.data)
        return res.data
    }

    // 진행 중인 갱신 Promise(single-flight).
    let refreshPromise = null

    /**
     * access 갱신 — 인터셉터가 401 시 자동 호출. refresh 쿠키를 서버가 읽어 새 access 발급.
     * @returns {Promise<boolean>} 성공 여부(false = 쿠키 없음/만료 → 재로그인 필요)
     */
    function refreshTokens() {
        if (refreshPromise) {
            return refreshPromise
        }
        refreshPromise = doRefresh().finally(() => {
            refreshPromise = null
        })
        return refreshPromise
    }

    async function doRefresh() {
        try {
            const res = await authService.refresh(rememberMe.value)
            applyAuth(res.data.data)
            return true
        } catch (e) {
            clearAuth()
            return false
        }
    }

    /**
     * 앱 시작 시 자동로그인 복원 — refresh 쿠키가 있으면 access 재발급.
     * 라우터 가드보다 먼저 완료되도록 main.js 에서 await 한다.
     * @returns {Promise<boolean>} 로그인 복원 성공 여부
     */
    function restore() {
        return refreshTokens()
    }

    /** 로그아웃 — refresh 폐기(DB+쿠키) + 세션 종료 */
    async function logout() {
        try {
            await authService.revokeTokens()
        } catch (e) {
            // 폐기 실패해도 로컬·세션 정리는 진행
        }
        try {
            await authService.sessionLogout()
        } finally {
            clearAuth()
        }
    }

    /** 인증 상태 초기화(세션 쿠키는 서버가 처리) */
    function clearAuth() {
        user.value = null
        roles.value = []
        accessToken.value = null
    }

    /** (데모용) access 훼손 — 인터셉터 자동 갱신 확인용 */
    function corruptAccessToken() {
        accessToken.value = 'broken.' + (accessToken.value || 'token')
    }

    return {
        user,
        roles,
        accessToken,
        rememberMe,
        isLoggedIn,
        hasTokens,
        isAdmin,
        login,
        sessionLogin,
        issueTokens,
        refreshTokens,
        restore,
        logout,
        clearAuth,
        corruptAccessToken,
    }
})
