import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
//
// 백엔드(Spring Boot)는 포트 70에서 뜬다. 이 SPA 는 백엔드의 루트(/)로 서빙된다.
//  - base '/'               : 자산·라우터가 루트 밑에서 동작 (대민 /, 관리자 /admin)
//  - build.outDir           : 빌드 결과를 백엔드 정적 루트(static)로 바로 출력
//                             (index.html·assets·favicon → classpath:/static/, WebConfig 가 SPA 폴백)
//  - server.proxy           : dev 서버(:5173)에서 /api·/auth·/test 를 백엔드(:70)로 전달(CORS 회피)
export default defineConfig({
    plugins: [vue()],
    base: '/',
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    build: {
        outDir: '../../project/src/main/resources/static',
        emptyOutDir: true,
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:70',
                changeOrigin: true,
            },
            '/auth': {
                // 정식 인증 모듈(세션 로그인·토큰 발급) — 세션 쿠키가 same-origin 으로 동작
                target: 'http://localhost:70',
                changeOrigin: true,
            },
            '/test': {
                target: 'http://localhost:70',
                changeOrigin: true,
            },
        },
    },
})
