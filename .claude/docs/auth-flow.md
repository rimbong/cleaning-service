# CleanHub 인증 흐름 (코드레벨)

> 세션 앵커 + JWT(API) + refresh HttpOnly 쿠키 구조의 전체 흐름을 파일·메서드 단위로 정리.
> 새 세션에서 이 문서를 근거로 질문/작업을 이어간다. 최종 갱신: 2026-07-08

## 구성요소 지도

### 백엔드 (`project/src/main/java/com/boot/cleanhub/`)
| 파일 | 역할 |
|---|---|
| `config/SessionSecurityConfig` | `/auth/**`·`/admin` 세션 인증 체인 (폼 로그인) |
| `config/JwtApiSecurityConfig` | `/api/**` JWT 체인 (STATELESS, Bearer) |
| `filter/JwtRequestFilter` | 매 `/api` 요청의 Bearer 토큰 검증 |
| `auth/controller/AuthApiController` | `/auth/api/me`(세션확인), `/auth/api/token`(JWT 발급) |
| `auth/controller/TokenRefreshController` | `/api/auth/refresh`(갱신), `/api/auth/logout`(폐기) |
| `auth/service/TokenService` | 토큰 생성 + refresh DB 저장(사용자당 1개 upsert) |
| `auth/support/RefreshTokenCookie` | refresh HttpOnly 쿠키 write/clear |
| `auth/domain/{AuthUser,RefreshToken}` | 세션 로그인 사용자 / refresh 토큰(DB) |
| `util/jwt/JwtUtil` | JWT 생성·파싱(서명/만료 검증), roles·token_type 클레임 |

### 프론트 (`frontend/vue/src/`)
| 파일 | 역할 |
|---|---|
| `plugins/http/axios.js` | Bearer 자동첨부(요청) + 401 자동갱신(응답) 인터셉터, `withCredentials` |
| `stores/auth/auth.js` | 로그인 상태·access(메모리)·rememberMe, login/restore/refreshTokens/logout |
| `services/auth/authService.js` | 인증 API 호출 (URL 매핑) |
| `main.js` | 앱 시작 시 `restore()` (자동로그인 복원) 후 mount |
| `views/admin/AdminLoginView.vue` | 관리자 로그인 화면(로그인 유지 체크박스) |
| `router/index.js` | `/admin/**` 가드 (미인증 → 로그인) |

### 자산 요약
| 자산 | 저장 위치 | 수명 | 담당 |
|---|---|---|---|
| **세션**(JSESSIONID) | HttpOnly 쿠키 | 브라우저 세션 | 로그인 상태(브라우저 열린 동안) |
| **access**(JWT) | 스토어 메모리 | 1시간 | `/api/**` Bearer 인증 |
| **refresh**(JWT) | HttpOnly 쿠키 + DB | 7일 | 자동로그인·access 재발급 |

---

## 시나리오 1 — 최초 로그인

```
[화면] AdminLoginView.onSubmit()
  │
  ├─ 1) auth.sessionLogin(id, pw)                          stores/auth/auth.js
  │     └─ authService.sessionLogin → POST /auth/login (form-encoded)
  │           [백엔드] SessionSecurityConfig 폼 로그인 필터
  │             → AuthUserDetailsService.loadUserByUsername(DB 조회)
  │             → BCryptPasswordEncoder 로 비번 대조
  │             → ★세션 생성(JSESSIONID 쿠키)
  │             → 성공 핸들러가 JSON {username} 반환
  │        └─ authService.me() → GET /auth/api/me
  │           [백엔드] AuthApiController.me() → 세션에서 roles 추출
  │        └─ store: user, roles 설정
  │
  ├─ 2) if (!auth.isAdmin) → 거부 (auth.logout())
  │
  └─ 3) auth.issueTokens(remember)                          stores/auth/auth.js
        └─ authService.issueTokens → POST /auth/api/token?rememberMe=true
              [백엔드] AuthApiController.issueToken()  ← 세션 있어야 호출됨
                → TokenService.issue(username, roles)
                    → JwtUtil.generateAccessToken() / generateRefreshToken()
                    → RefreshTokenRepository.save()   ★refresh DB 저장
                → RefreshTokenCookie.write()          ★Set-Cookie refreshToken (HttpOnly)
                → body: AuthResult { accessToken, username, roles }  (refresh 없음!)
           └─ store.applyAuth: ★accessToken=메모리, user, roles
        → router.replace('/admin')
```
**생기는 3가지:** 세션(JSESSIONID 쿠키), refreshToken(HttpOnly 쿠키), accessToken(스토어 메모리).

---

## 시나리오 2 — API 호출 (거래처 목록)

```
[화면] ClientListView  useQuery(['clients'])
  └─ clientService.list() → get('/api/admin/clients')
       │
       ├─ [axios 요청 인터셉터]  plugins/http/axios.js
       │    isApiZoneUrl('/api/') → true
       │    authBridge.getAccessToken() = store.accessToken
       │    → config.headers.Authorization = 'Bearer ' + access   ★자동 첨부
       │
       └─ [백엔드] JwtApiSecurityConfig 체인 (/api/**)
            → JwtRequestFilter.doFilterInternal()
                → Authorization 헤더에서 Bearer 추출
                → JwtUtil.parseClaims()  ★서명+만료 검증(위조/만료 걸림)
                → token_type=access 확인 (refresh 토큰으로 API 호출 차단)
                → roles → SecurityContext 에 인증 등록
            → /api/admin/** = hasRole("ADMIN") 검사
            → ClientAdminController.list() → ClientService.list() → Repository
            → ApiResponse 반환
```
**포인트:** 화면 코드엔 토큰 첨부 코드가 없다. 인터셉터가 처리.

---

## 시나리오 3 — access 만료 → 자동 갱신 (핵심)

```
[화면] 아무 API 호출 → access 만료됨 → 백엔드 401
  │
  └─ [axios 응답 인터셉터]  plugins/http/axios.js
       401 && /api/ && AUTH_URLS 아님 && 재시도 안 함
       │
       ├─ authBridge.refresh() = store.refreshTokens()      stores/auth/auth.js
       │    └─ doRefresh() → authService.refresh(rememberMe)
       │         → POST /api/auth/refresh?rememberMe=true
       │           (body 없음! refreshToken 쿠키는 브라우저가 자동 전송)
       │              [백엔드] TokenRefreshController.refreshAccessToken()
       │                → @CookieValue 로 refreshToken 쿠키 읽음
       │                → RefreshTokenRepository.findByToken (DB 대조)  ★폐기된 토큰 차단
       │                → JwtUtil.parseClaims (서명+만료 검증)
       │                → [회전] TokenService.issue → 새 refresh 쿠키 재발급
       │                → AuthResult { accessToken, username, roles }
       │         └─ store.applyAuth: ★새 accessToken
       │
       └─ 원래 실패했던 요청을 새 access 로 "1회 재시도" → 성공
```
**single-flight:** 동시 다발 401 이어도 `refreshPromise` 하나 공유 → 갱신 1번만.

---

## 시나리오 4 — 새로고침/브라우저 재시작 → 자동로그인 복원

```
[앱 시작]  main.js
  │
  └─ auth.restore()  = refreshTokens()   ← 시나리오 3의 refresh 와 같은 함수!
       └─ POST /api/auth/refresh (refreshToken 쿠키 자동 전송)
            ├─ 쿠키 있고 유효 → 새 access + user + roles 복원 → 로그인 유지 ✅
            └─ 쿠키 없음/만료 → 401 → clearAuth() → 비로그인
       └─ .finally(() => app.mount())   ★복원 끝난 뒤 마운트
  │
  └─ [라우터 가드]  router/index.js  beforeEach
       requiresAdmin 이면 → auth.hasTokens && auth.isAdmin 검사
       (restore 가 먼저 끝났으므로 복원된 상태로 판단 → 안 튕김)
```
**핵심:** 세션·access는 사라져도 refreshToken 쿠키는 남아 `restore()`가 access를 다시 받는다. **세션이 없어도 복원됨**(E2E 검증 완료).

---

## 시나리오 5 — 로그아웃

```
[화면] AdminLayout.onLogout → auth.logout()   stores/auth/auth.js
  ├─ authService.revokeTokens() → POST /api/auth/logout
  │    [백엔드] TokenRefreshController.logout()
  │      → 쿠키의 refreshToken → RefreshTokenRepository.delete (★DB 삭제)
  │      → RefreshTokenCookie.clear (★쿠키 Max-Age=0 삭제)
  ├─ authService.sessionLogout() → POST /auth/logout
  │    [백엔드] SessionSecurityConfig → ★세션 무효화
  └─ clearAuth() → 메모리 상태 초기화 (user/roles/access = null)
```
**3중 정리:** DB refresh 삭제 + 쿠키 삭제 + 세션 무효화 + 메모리 초기화.

---

## 설계 근거 메모
- **왜 세션 + JWT 하이브리드**: 세션(HttpOnly, 서버보관)은 강제 로그아웃 등 서버 통제권 제공. JWT는 `/api` stateless 인증. SPA + 자동로그인에 적합.
- **왜 refresh를 HttpOnly 쿠키**: refresh는 장기 신분증이라 탈취 시 피해 큼. HttpOnly=JS 접근 불가(XSS 안전), DB 대조로 폐기 가능. WEB_PORTAL(refresh 기반 자동로그인)과 같은 철학이되 HttpOnly로 더 안전.
- **왜 access는 메모리**: 자주 쓰는 토큰이 새어나가지 않게. 새로고침 시 사라져도 refresh 쿠키로 즉시 복원.
- **rememberMe(로그인 유지)**: true=영속쿠키(재시작해도 유지)/false=세션쿠키(브라우저 닫으면 소멸). 회전 시 이 값을 유지하려 스토어가 rememberMe를 기억해 refresh에 전달.

## 관련 설정 (etc.properties)
- `jwt.access-token-expiration-ms=3600000`(1h) / `jwt.refresh-token-expiration-ms=604800000`(7d)
- `jwt.refresh-token-rotation`(기본 true=회전) / `jwt.refresh-cookie.secure`(개발 false, 운영 env REFRESH_COOKIE_SECURE=true)
