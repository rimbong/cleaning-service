import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'

import App from './App.vue'
import router from './router'
import i18n from './common/i18n'
import { useAuthStore } from '@/stores/common/auth/auth'
import { installDatePickerOpener } from '@/common/plugins/datePicker/datePicker'
import './style.css'

const app = createApp(App)
app.use(createPinia()) // 전역 상태(Pinia) — store 는 src/stores/ 참고

// 날짜 input 사용성: 박스 아무 곳이나 클릭해도 달력이 열리게(아이콘 전용 X)
installDatePickerOpener()

// 자동로그인 복원: refresh(HttpOnly 쿠키)로 access 재발급을 "먼저" 시도한 뒤 마운트한다.
// → 라우터 가드(첫 내비게이션)가 복원된 로그인 상태를 보고 판단하므로,
//   새로고침·브라우저 재시작 시에도 관리자 페이지에서 튕기지 않는다.
const auth = useAuthStore()
auth.restore().finally(() => {
    app.use(router)
        .use(i18n)
        // TanStack Vue Query — 서버 데이터 캐싱/동기화(useQuery). 조회 화면에서 사용.
        .use(VueQueryPlugin)
        .mount('#app')
})
