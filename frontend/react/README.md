# frontend/react — React 스타터 (선택지 A)

아직 npm 프로젝트가 생성되지 않은 **골격 폴더**다. 이 갈래를 쓸 때 아래로 초기화한다.

## 1. 생성 (Vite)
```
cd frontend/react
npm create vite@latest . -- --template react   # 또는 react-ts
npm install
```

## 2. dev 서버 + 백엔드 API 연동
```
npm run dev            # http://localhost:5173
```
`vite.config.js` 에 프록시를 두면 CORS 없이 백엔드 호출:
```js
server: {
  port: 5173,
  proxy: { '/api': 'http://localhost:70' }   // /api → 백엔드(:70)
}
```

## 3. 빌드 → 백엔드 정적 폴더로 배포 (경로 /app 통일)
백엔드가 `/app/**` 로 SPA 를 서빙하므로 **base 를 `/app/` 로** 맞춰야 자산·라우팅 경로가 맞는다.
```js
// vite.config.js
export default defineConfig({
  base: '/app/',
  build: {
    outDir: '../../project/src/main/resources/static/app',
    emptyOutDir: true,
  },
})
```
```
npm run build          # → project/src/main/resources/static/app/ 로 바로 출력
```
- 화면 라우팅은 `/app/...`, 자산은 `/app/js|css/...`, 딥링크 새로고침은 백엔드가 index.html 폴백.

## 커밋 정책
`node_modules/`, `dist/` 는 `.gitignore` 로 제외(소스만 커밋).
