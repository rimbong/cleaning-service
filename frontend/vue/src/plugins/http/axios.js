import axios from 'axios'

/**
 * 프로젝트 공통 Axios 인스턴스.
 *
 * - baseURL 을 두지 않는다: 각 요청에 '/api/...' 처럼 절대경로를 준다.
 *   개발 시에는 Vite 프록시가 /api·/auth·/test 를 백엔드(:70)로 전달하고,
 *   배포 시에는 같은 오리진(백엔드가 /app 으로 SPA 서빙)이라 그대로 동작한다.
 * - 인증 토큰 주입/공통 에러 처리가 필요하면 아래 인터셉터에 추가한다.
 */
const instance = axios.create({
    headers: {
        'Content-Type': 'application/json',
    },
})

/* ============================================================
 * JWT 인증 연동 (인터셉터)
 *
 * 이 모듈은 "토큰을 어디에 어떻게 붙이고, 만료되면 어떻게 살리는가"만 담당하고,
 * 토큰 자체의 보관/발급/갱신 로직은 auth 스토어(stores/auth.js)가 담당한다.
 * 순환 import 를 피하려고 스토어가 기동 시 configureAuth() 로 자기 함수들을
 * 여기에 "등록"해 주는 구조다(의존 방향: store → axios 단방향).
 * ============================================================ */

/** auth 스토어가 등록하는 연동 함수들 { getAccessToken, refresh } */
let authBridge = null

/** 자동 갱신을 시도하면 안 되는 URL(여기서 401 이면 그냥 실패가 맞음 — 무한루프 방지) */
const AUTH_URLS = ['/api/auth/refresh', '/api/auth/logout']

/**
 * auth 스토어가 자신의 토큰 접근/갱신 함수를 등록한다(stores/auth.js 에서 호출).
 * @param {{getAccessToken: Function, refresh: Function}} bridge
 */
export function configureAuth(bridge) {
    authBridge = bridge
}

/** URL 이 JWT 존(/api/**)인지 판별 — '/api/' 프리픽스 비교(쿼리스트링 무관, /apidocs 류 오매칭 방지) */
function isApiZoneUrl(url) {
    return typeof url === 'string' && url.startsWith('/api/')
}

// [요청 인터셉터] /api/** 요청에 Authorization: Bearer 자동 첨부.
//  컴포넌트가 매번 헤더를 신경 쓸 필요가 없어진다.
instance.interceptors.request.use((config) => {
    if (authBridge && isApiZoneUrl(config.url)) {
        const token = authBridge.getAccessToken()
        if (token && !config.headers.Authorization) {
            config.headers.Authorization = 'Bearer ' + token
        }
    }
    return config
})

// [응답 인터셉터] /api/** 가 401(access 만료)이면:
//  refresh 로 새 access 를 받아 → 실패했던 원래 요청을 "1회" 자동 재시도.
//  (구 프로젝트에서 주석으로만 남아 있던 '401 자동 갱신+재시도' 로직의 완성본)
instance.interceptors.response.use(
    (response) => response,
    async (error) => {
        const original = error.config
        const status = error.response ? error.response.status : null

        // 쿼리스트링을 떼고 경로만 비교(?a=b 가 붙어도 제외 목록이 뚫리지 않게)
        const path = original && original.url ? original.url.split('?')[0] : ''

        const shouldRetry = status === 401
            && authBridge
            && isApiZoneUrl(path)
            && !AUTH_URLS.includes(path)  // 갱신/폐기 자신의 401 은 재시도 금지
            && !original._retried         // 재시도는 딱 1회(무한루프 방지)

        if (shouldRetry) {
            original._retried = true
            // 스토어가 /api/auth/refresh 수행. 동시 다발 401 이어도 스토어 쪽
            // single-flight 로 실제 갱신은 1번만 일어난다(stores/auth.js 참고).
            const refreshed = await authBridge.refresh()
            if (refreshed) {
                // 새 access 로 교체해 원래 요청 재실행
                original.headers = {
                    ...original.headers,
                    Authorization: 'Bearer ' + authBridge.getAccessToken(),
                }
                return instance(original)
            }
            // 갱신도 실패(= refresh 만료/폐기) → 원래 에러로 진행(화면이 재로그인 유도)
        }
        return Promise.reject(error)
    },
)

/**
 * API 요청 언어(Accept-Language 헤더)를 설정한다.
 * 백엔드가 이 헤더를 보고 에러/메시지를 해당 언어로 localize 한다(i18n 의 setLocale 에서 호출).
 * @param {string} locale 'ko' | 'en'
 */
export function setApiLocale(locale) {
    instance.defaults.headers.common['Accept-Language'] = locale
}

/** 범용 래퍼 — 컴포넌트/서비스에서 이걸 사용한다. */
export const get = (url, config) => instance.get(url, config)
export const post = (url, data, config) => instance.post(url, data, config)
export const put = (url, data, config) => instance.put(url, data, config)
export const del = (url, config) => instance.delete(url, config)

/**
 * blob 응답을 브라우저 파일 다운로드로 트리거한다(공통 로직).
 * 서버가 준 Content-Disposition 의 filename 을 우선 사용하고, 없으면 fallback 이름.
 * @param {import('axios').AxiosResponse} res  responseType:'blob' 로 받은 응답
 * @param {string} fallbackName 헤더에 파일명이 없을 때 쓸 기본 이름
 */
function triggerBlobDownload(res, fallbackName) {
    let fileName = fallbackName || 'download'
    const disposition = res.headers['content-disposition']
    if (disposition) {
        // filename="..." 형태에서 추출(서버가 브라우저별로 인코딩해 주므로 디코딩 시도)
        const match = disposition.match(/filename="?([^"]+)"?/)
        if (match && match[1]) {
            try {
                fileName = decodeURIComponent(match[1])
            } catch (e) {
                fileName = match[1] // 디코딩 실패 시 원문 사용
            }
        }
    }
    const blobUrl = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = blobUrl
    link.setAttribute('download', fileName)
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(blobUrl) // 메모리 누수 방지
}

/**
 * GET 방식 파일 다운로드(+진행률).
 * 인증(Bearer)·에러 처리는 공통 인스턴스의 인터셉터가 그대로 적용된다.
 *
 * @param {string} url   다운로드 URL
 * @param {object} [options]
 * @param {object} [options.params]     쿼리 파라미터
 * @param {(percent:number)=>void} [options.onProgress]  진행률(0~100) 콜백 — progress bar 연동
 * @param {string} [options.fallbackName]  Content-Disposition 없을 때 파일명
 * @returns {Promise<void>}
 */
export function downloadGet(url, options = {}) {
    const { params, onProgress, fallbackName } = options
    return instance
        .get(url, {
            params,
            responseType: 'blob',
            onDownloadProgress: (e) => {
                // e.total 은 서버가 Content-Length 를 줄 때만 존재(스트리밍이면 없을 수 있음)
                if (onProgress && e.total) {
                    onProgress(Math.round((e.loaded * 100) / e.total))
                }
            },
        })
        .then((res) => triggerBlobDownload(res, fallbackName))
}

/**
 * POST 방식 파일 다운로드(폼 파라미터 전송, +진행률).
 * onDownloadProgress 는 요청 메서드와 무관하게 "응답 본문 수신"을 추적하므로 POST 도 지원된다.
 * (GET/POST 차이는 파라미터 전달 방식일 뿐, 다운로드 진행률 추적은 동일)
 *
 * @param {string} url
 * @param {object} [options]
 * @param {object} [options.params]     form-urlencoded 로 전송할 값
 * @param {(percent:number)=>void} [options.onProgress]  진행률(0~100) 콜백
 * @param {string} [options.fallbackName]
 * @returns {Promise<void>}
 */
export function downloadPost(url, options = {}) {
    const { params = {}, onProgress, fallbackName } = options
    const body = new URLSearchParams()
    Object.keys(params).forEach((k) => body.append(k, params[k]))
    return instance
        .post(url, body, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
            responseType: 'blob',
            onDownloadProgress: (e) => {
                if (onProgress && e.total) {
                    onProgress(Math.round((e.loaded * 100) / e.total))
                }
            },
        })
        .then((res) => triggerBlobDownload(res, fallbackName))
}

export default instance
