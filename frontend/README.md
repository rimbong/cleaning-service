# frontend/ — 프론트엔드 갈래 (선택지)

이 프레임워크는 백엔드(`../project/`, Spring Boot)를 **공유**하고,
프론트엔드는 **React / Vue 중 하나를 골라** 쓰는 구조다.
(Thymeleaf 서버렌더링은 백엔드 `project/.../templates/` 안에 이미 있음 — 별도 폴더 불필요.)

```
frontend/
├─ react/     ← React 스타터 (선택지 A)
└─ vue/       ← Vue 스타터   (선택지 B)
```

## 핵심 원칙 — "선택지는 둘, 산출물은 하나, 경로는 /app"
- `react/`, `vue/` 는 **소스 스타터**로 둘 다 제공된다(프레임워크니까).
- 실제 프로젝트는 **하나만 선택**해서 개발/빌드한다.
- 어느 걸 고르든 **빌드 산출물은 한 곳**으로 간다:
  ```
  project/src/main/resources/static/app/
  ```
- 백엔드 경로 규칙은 **딱 3개**로 끝난다:
  ```
  /api/**    → 백엔드 API 컨트롤러 (JSON)
  /app/**    → SPA 전부 (자산 /app/js/… + 화면 라우팅 /app/…)
  /test/**   → Thymeleaf 데모(서버렌더링)
  ```
- `/app/**` 리소스 핸들러(WebConfig)가 **파일 있으면 서빙, 없으면 index.html 폴백** →
  딥링크 새로고침(`/app/crud/list` 직접 열기)도 동작. (별도 폴백 컨트롤러 불필요)

## 프론트 설정 규칙 (React·Vue 공통)
빌드/라우팅을 백엔드 `/app` 경로에 맞춘다:
```js
// vite.config.js
export default defineConfig({
  base: '/app/',                                  // 자산·라우터가 /app/ 밑에서 동작
  build: { outDir: '../../project/src/main/resources/static/app', emptyOutDir: true },
  server: { proxy: { '/api': 'http://localhost:70' } },  // dev: API 프록시
})
```
- 라우터는 `createWebHistory(import.meta.env.BASE_URL)` 로 두면 base(`/app/`)를 자동으로 따라감.

## 개발(dev) 흐름 — static 폴더 안 씀
각 프론트는 자체 dev 서버로 돌리고, 백엔드 API 는 프록시(또는 CORS)로 호출한다.
```
백엔드:  http://localhost:70/api/**     (/build spring-boot:run)
React:   http://localhost:5173         (frontend/react> npm run dev)
Vue:     http://localhost:5173         (frontend/vue>   npm run dev)  ← 한 번에 하나만 띄우면 포트 동일해도 됨
```
> CORS 허용 오리진은 백엔드 `application.yml` 의 `cors.allowed-origins` 에서 관리(프록시 쓰면 CORS 불필요).

## 배포(prod) 흐름 — 고른 것만 빌드
```
cd frontend/<react|vue>
npm run build            # → outDir(static/app)로 바로 출력 (emptyOutDir 로 기존 산출물 정리)
```
그 뒤 백엔드를 `clean package` 하면 WAR 하나에 API + SPA(`/app`)가 함께 담긴다.
(운영에서 Nginx 등으로 프론트를 따로 서빙해도 됨 — 그때는 이 빌드 산출물을 웹서버에 배치.)

## 아직 npm 미설치 (골격 단계)
현재는 폴더 골격만 있다. 실제 앱 생성은 각 폴더의 `README.md` 참고.
`static/app/index.html` 은 배선 확인용 임시 랜딩(SPA 빌드로 교체됨).
