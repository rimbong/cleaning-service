# frontend/vue — Vue3 프론트 (선택지 B)

Vite 로 스캐폴딩한 **깨끗한 최소 Vue3 스타터**. 백엔드(`../../project`, Spring Boot :70)의
`/app` 경로로 서빙되도록 배선돼 있다.

## 스택
- Vue 3.5 · vue-router · axios (최소 구성)
- 빌드: Vite (Node 20.19+ / 22.12+ 필요)

## 구조
```
src/
├─ main.js                     앱 진입점(라우터 장착)
├─ App.vue                     상단 헤더 + <RouterView> 셸
├─ router/index.js             라우터(base 는 vite base '/app/' 자동 추종)
├─ plugins/http/axios.js       공통 axios 인스턴스 + get/post/put/del 래퍼
├─ composables/http/useRequest.js  요청 반응형 상태(data/error/isPending) 컴포저블
├─ views/HomeView.vue          백엔드 표준응답(ApiResponse) 연동 데모
└─ style.css                   최소 전역 스타일
```

## 개발
```
# 1) 백엔드 먼저 기동 (repo 루트에서)
#    /build spring-boot:run     → http://localhost:70

# 2) 프론트 dev 서버 (이 폴더에서)
npm install        # 최초 1회
npm run dev        # http://localhost:5173/app/
```
- dev 서버는 `/api`·`/test` 요청을 백엔드(:70)로 **프록시**한다(CORS 불필요).
- Home 화면이 `GET /test/api/ok` 를 호출해 `{success, code, message, data}` 응답을 보여준다.

## 배포(빌드)
```
npm run build      # → ../../project/src/main/resources/static/app 로 바로 출력
```
- `base:'/app/'` 라 자산·라우팅이 `/app/` 하위에서 동작.
- 백엔드를 패키징하면 WAR 하나에 API + SPA(`/app`)가 함께 담긴다.
- 딥링크 새로고침(`/app/...`)은 백엔드 `WebConfig` 리소스 핸들러가 `index.html` 로 폴백.

## 다음 이관 대상(레거시에서 선별)
필요해질 때 `spring_boot_vue3` 레거시에서 **정제해서** 옮긴다(통짜 복사 금지):
- pinia 스토어(알림/스피너), vue-i18n(ko/en), 폼/파일 다운로드 axios 헬퍼
- 기능 화면(crud·jwt·db·excel…)은 붙일 때 **하나씩** 리뷰하며 이관

## 커밋 정책
`node_modules/`, `dist/` 는 `.gitignore` 로 제외(소스만 커밋).
