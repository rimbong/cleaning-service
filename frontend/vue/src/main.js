import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'

import App from './App.vue'
import router from './router'
import i18n from './common/i18n'
import { useAuthStore } from '@/stores/common/auth/auth'
import { installDatePickerOpener } from '@/common/plugins/datePicker/datePicker'
import './style.css'

/**
 * vue-query 전역 설정.
 *
 * staleTime 기본값을 0 으로 둔다. 즉 화면에 들어갈 때마다 서버에 다시 묻는다.
 *
 * 이유: 이 앱은 로컬 PC 에서 로컬 DB 를 읽는다. 실측한 응답 시간이 6~64ms 라
 * 캐시로 아끼는 시간을 사람이 느낄 수 없다. 반면 조회하는 값은 대부분 금액이거나
 * "오늘 어디를 청소하는가" 같은 운영 정보라, 옛 값을 보여주는 비용이 훨씬 크다.
 * 실제로 캐시를 지우지 않아 옛 값이 남는 문제가 재고·재산정 화면에서 반복됐다.
 *
 * 캐시 자체는 남아 있어서 화면을 다시 열면 이전 값을 먼저 보여주고 뒤에서 갱신한다
 * (깜빡임 없음). 무효화(invalidateQueries)는 즉시 반영을 위한 보조 수단이고,
 * 빠뜨려도 화면을 나갔다 들어오면 맞는 값이 된다.
 *
 * 예외를 두려면 해당 useQuery 에만 staleTime 을 적는다. 기준은 두 가지를 모두 만족할 때다.
 *   1) 조회가 눈에 띄게 느리다(수백 ms 이상)
 *   2) 잠깐 옛 값이어도 업무상 문제가 없다(금액·재고·일정이 아닌 참조 데이터)
 * 지금은 두 조건을 모두 만족하는 화면이 없어 전부 기본값을 쓴다.
 */
const VUE_QUERY_OPTIONS = {
    queryClientConfig: {
        defaultOptions: {
            queries: {
                staleTime: 0,
            },
        },
    },
}

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
        // TanStack Vue Query — 서버 데이터 조회(useQuery). 로딩·에러 상태와 중복 요청 제거를 맡는다.
        .use(VueQueryPlugin, VUE_QUERY_OPTIONS)
        .mount('#app')
})
