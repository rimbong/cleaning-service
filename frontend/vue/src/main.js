import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'

import App from './App.vue'
import router from './router'
import i18n from './i18n'
import './style.css'

createApp(App)
    .use(createPinia()) // 전역 상태(Pinia) — store 는 src/stores/ 참고
    .use(router)
    .use(i18n)
    // TanStack Vue Query — 서버 데이터 캐싱/동기화(useQuery). 조회 화면에서 사용.
    .use(VueQueryPlugin)
    .mount('#app')
