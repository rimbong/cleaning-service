import { get, post } from '@/common/plugins/http/axios'

/**
 * 인증 관련 API 호출 모음.
 *
 * 백엔드는 두 개의 보안 존으로 나뉜다(URL 기준 — SessionSecurityConfig/JwtApiSecurityConfig 참고):
 *  - 세션 존(/auth/...): 로그인·토큰 발급. 세션 쿠키는 브라우저가 자동 관리(코드에서 안 다룸).
 *  - JWT 존(/api/...) : Bearer 토큰 필요. 첨부는 axios 요청 인터셉터가 자동 처리.
 */
export const authService = {
    /**
     * 세션 로그인.
     * 시큐리티 폼 로그인 필터가 처리하므로 JSON 이 아니라 form-encoded 로 보내야 한다.
     */
    sessionLogin(username, password) {
        const body = new URLSearchParams({ username, password })
        return post('/auth/login', body, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        })
    },

    /** 세션 로그아웃(JSESSIONID 무효화) */
    sessionLogout() {
        return post('/auth/logout')
    },

    /** 현재 세션 사용자 확인 → data: { username, roles } */
    me() {
        return get('/auth/api/me')
    },

    /**
     * 세션 인증으로 JWT 발급.
     * access 는 응답 body(data: { accessToken, username, roles }), refresh 는 HttpOnly 쿠키로 내려온다.
     * @param {boolean} rememberMe true=자동로그인(영속 쿠키)/false=세션 쿠키
     */
    issueTokens(rememberMe = false) {
        return post(`/auth/api/token?rememberMe=${rememberMe}`)
    },

    /**
     * access 토큰 갱신(= 자동로그인 복원). refresh 는 HttpOnly 쿠키에서 서버가 읽으므로 body 가 없다.
     * @param {boolean} rememberMe 회전 시 재발급될 쿠키의 영속 여부 유지
     * @returns data: { accessToken, username, roles }
     */
    refresh(rememberMe = true) {
        return post(`/api/auth/refresh?rememberMe=${rememberMe}`)
    },

    /** JWT 로그아웃 = refresh 토큰 폐기(DB 삭제 + 쿠키 삭제). 쿠키는 서버가 읽어 처리 */
    revokeTokens() {
        return post('/api/auth/logout')
    },

    /** JWT 존 보호 API 데모(인증만 되면 OK, 응답은 평문 텍스트) */
    hello() {
        return get('/api/hello')
    },

    /** 관리자 전용 API — ROLE_ADMIN 필요(user1 은 403, admin1 은 200) */
    adminHello() {
        return get('/api/admin')
    },
}
