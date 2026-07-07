# Vue2 → Vue3 가이드 (유지보수용)

Vue2(Options API)만 다뤄본 사람이 이 스타터(Vue3 + Composition API)를 읽고 고칠 수 있도록
**Vue2와 달라진 점**만 추려서 정리한 문서다. 실제 코드는 `src/` 참고.

---

## 0. 한 줄 요약

- Vue3의 가장 큰 변화 = **Composition API**(+ `<script setup>` 문법).
- Vue2 방식(Options API: `data/methods/computed`)도 Vue3에서 **여전히 동작**하지만,
  요즘 프로젝트/이 스타터는 **Composition API**를 쓴다.
- 전역 상태관리 **Vuex → Pinia**, 라우터 **vue-router 3 → 4** 로 바뀌었다.

---

## 1. Options API vs Composition API (골격 비교)

같은 컴포넌트를 두 방식으로 쓴 것:

```vue
<!-- Vue2 스타일 (Options API) -->
<script>
export default {
    data() {
        return { count: 0 }
    },
    computed: {
        double() { return this.count * 2 }
    },
    methods: {
        inc() { this.count++ }
    },
    mounted() {
        console.log('마운트됨')
    }
}
</script>
```

```vue
<!-- Vue3 스타일 (Composition API + <script setup>) -->
<script setup>
import { ref, computed, onMounted } from 'vue'

const count = ref(0)                       // data
const double = computed(() => count.value * 2)  // computed
function inc() { count.value++ }           // methods
onMounted(() => console.log('마운트됨'))    // mounted
</script>
```

- `<script setup>` 안에 **선언한 변수·함수는 자동으로 템플릿에 노출**된다(`return` 불필요).
- `this` 가 사라진다. `this.count` → 그냥 `count`(단, JS에선 `count.value`).

---

## 2. 개념 매핑 표

| Vue2 (Options API) | Vue3 (Composition API) | 메모 |
|---|---|---|
| `data() { return { x: 1 } }` | `const x = ref(1)` | 반응형 값. **JS에선 `x.value`** |
| (객체 상태) | `const obj = reactive({ a: 1 })` | 객체 전용. `.value` 없이 `obj.a` |
| `computed: { g() {} }` | `const g = computed(() => …)` | |
| `methods: { f() {} }` | `function f() {}` | 그냥 함수 |
| `watch: { x(v) {} }` | `watch(x, (v) => …)` | |
| `props: ['a']` | `const props = defineProps(['a'])` | `<script setup>` 매크로 |
| `this.$emit('e')` | `const emit = defineEmits(['e']); emit('e')` | |
| `created()` | (setup 본문에서 바로 실행) | setup 이 created 보다 먼저 |
| `mounted()` | `onMounted(() => …)` | |
| `beforeDestroy()` | `onBeforeUnmount(() => …)` | |
| `destroyed()` | `onUnmounted(() => …)` | |
| `this.$route` / `this.$router` | `useRoute()` / `useRouter()` | vue-router 4 |
| `this.$store` (Vuex) | `useXxxStore()` (Pinia) | 아래 5장 |
| **mixins** | **composables** | 아래 4장 |

---

## 3. 반응성: `ref` vs `reactive`, 그리고 `.value`

Vue2는 `data()`에 넣으면 알아서 반응형이 됐다. Vue3는 **명시적으로** 반응형으로 만든다.

| | 용도 | 접근 |
|---|---|---|
| `ref(x)` | 값 하나(숫자·문자열·객체 등 아무거나) | JS: `x.value` / 템플릿: `x` |
| `reactive({…})` | 객체/배열 | `obj.a` (`.value` 없음) |

**제일 자주 실수하는 것 — `.value`:**
```js
const count = ref(0)
count.value++            // ✅ JS 코드에선 .value 필요
// count++               // ❌ 동작 안 함
```
```html
<p>{{ count }}</p>       <!-- ✅ 템플릿에선 자동으로 벗겨짐(.value 안 씀) -->
```

> 초보 팁: **값 하나면 그냥 `ref`** 쓰면 된다. `reactive`는 destructure(구조분해)하면 반응성이 깨지는 함정이 있어서, 헷갈리면 `ref`로 통일해도 무방하다.

---

## 4. "로직 재사용" — 믹스인(Vue2) → 컴포저블(Vue3)

### "로직 재사용"이 뭔가
같은 동작이 여러 컴포넌트에서 반복될 때, **복붙하지 않고 한 곳에 빼서 공유**하는 것.
예) "API 부를 때 로딩 켜고 → 끝나면 끄고 → 에러나면 저장" 이 패턴은 데이터 화면마다 똑같이 반복된다.

### Vue2 방식: 믹스인 (그리고 그 문제점)
믹스인 = `data/methods` 등을 담은 객체를 컴포넌트에 **섞어 넣는(merge)** 방식.
```js
// loadingMixin.js (Vue2)
export default {
    data() { return { isPending: false } },
    methods: { start() { this.isPending = true } }
}
// 컴포넌트: mixins: [loadingMixin]  → this.isPending, this.start() 사용
```
문제:
- **출처 불명**: 템플릿의 `isPending`이 어느 믹스인에서 온 건지 안 보인다.
- **이름 충돌**: 믹스인 둘이 같은 `isPending`을 정의하면 조용히 덮어써진다.

### Vue3 방식: 컴포저블 (개선판)
**함수로 만들어 명시적으로 호출**하고, 반환값을 **내가 이름 붙여** 쓴다. → 출처가 명확하고 충돌 없음.
```js
// composables/http/useRequest.js (이 프로젝트 실제 코드)
import { ref } from 'vue'
export function useRequest() {
    const data = ref(null)
    const error = ref(null)
    const isPending = ref(false)
    const exec = async (promise) => { /* 로딩 관리 */ }
    return { data, error, isPending, exec }   // 명시적으로 돌려줌
}
```
```js
// 컴포넌트에서 사용 — 어디서 왔는지 명확
const { data, isPending, exec } = useRequest()
```

**규칙 두 가지**
1. 이름은 **`use`로 시작**(관례).
2. **`<script setup>`(setup) 안에서 호출**.

> 정리: **믹스인을 "함수 호출 + 구조분해"로 대체한 것이 컴포저블.** Vue2의 믹스인을 안 써봤다면, "재사용 로직은 `useXxx()` 함수로 만든다"만 기억하면 된다.

---

## 5. 전역 상태관리: Vuex → Pinia

**맞다 — Vuex는 Pinia로 대체됐다**(Pinia가 Vue 공식 추천, Vuex는 유지보수 모드).

전역 상태 = 여러 화면이 공유하는 데이터(로그인 사용자, 알림/토스트 등).

| | Vuex (Vue2) | Pinia (Vue3) |
|---|---|---|
| 상태 | `state` | `state` |
| 파생값 | `getters` | `getters` |
| 상태 변경 | **`mutations`(동기) + `actions`(비동기)** 로 분리 | **`actions`만** (mutations 없음, 더 단순) |
| 사용 | `this.$store.commit(...)` | `const store = useXxxStore(); store.doIt()` |

```js
// Pinia 예시 (store/counter.js)
import { defineStore } from 'pinia'
export const useCounterStore = defineStore('counter', {
    state: () => ({ count: 0 }),
    getters: { double: (s) => s.count * 2 },
    actions: { inc() { this.count++ } },   // Vuex와 달리 mutation 없이 바로 변경
})
```

### 컴포저블 vs Pinia — 언제 뭘 쓰나
- **컴포저블(`useXxx`)**: 재사용 "로직". 보통 **컴포넌트마다 상태가 새로 생김**(예: 화면 A의 로딩 ≠ 화면 B의 로딩).
- **Pinia store**: **앱 전체가 공유하는 하나의 상태**(예: 로그인 사용자, 전역 알림 큐).

> 이 스타터의 실제 예: `src/stores/notify.js`(전역 알럿/컨펌/토스트/스피너) + 표시 담당
> `src/components/notify/NotifyHost.vue`. confirm 이 Promise 를 반환해 `await notify.confirm(...)`
> 으로 쓰는 패턴을 눈여겨볼 것(구식 콜백 방식의 개선판).

---

## 6. 라우터: vue-router 3 → 4 (작은 차이)

| | Vue2 (vue-router 3) | Vue3 (vue-router 4) |
|---|---|---|
| 생성 | `new VueRouter({ routes })` | `createRouter({ history: createWebHistory(), routes })` |
| 히스토리 모드 | `mode: 'history'` | `history: createWebHistory(base)` |
| 컴포넌트에서 | `this.$route`, `this.$router` | `useRoute()`, `useRouter()` |

이 프로젝트: `src/router/index.js` 참고. `createWebHistory(import.meta.env.BASE_URL)`의
`BASE_URL`은 `vite.config.js`의 `base('/app/')`를 자동으로 따라간다.

---

## 7. 이 스타터 코드에 대입해서 읽기

| 파일 | Vue2 사고방식으로 보면 |
|---|---|
| `src/main.js` | `new Vue({...}).$mount('#app')` → `createApp(App).use(...).mount('#app')` |
| `src/App.vue` | 루트 컴포넌트. `<script setup>` + `useI18n()` 컴포저블 사용 |
| `src/views/HomeView.vue` | 화면 컴포넌트. `ref`/`computed`/컴포저블로 상태 관리 |
| `src/composables/http/useRequest.js` | (Vue2의 믹스인 자리) 요청 상태 로직 재사용 |
| `src/plugins/http/axios.js` | 그냥 유틸 함수 모음(컴포저블 아님) |
| `src/i18n/index.js` | vue-i18n 설정 |

---

## 8. 자주 헷갈리는 것 체크리스트

- [ ] JS에서 `ref` 값 읽고 쓸 땐 **`.value`** (템플릿에선 생략).
- [ ] `<script setup>`에선 `return` 안 해도 템플릿에서 변수·함수 바로 씀.
- [ ] `this`는 없다. `this.foo` → `foo`.
- [ ] 재사용 로직 = **컴포저블(`useXxx`)**, 전역 공유 상태 = **Pinia store**.
- [ ] `methods`는 그냥 함수, `computed`는 `computed(() => …)`, `mounted`는 `onMounted(…)`.
- [ ] props 받을 땐 `defineProps`, 이벤트는 `defineEmits`.

---

## 참고 링크
- Vue3 공식(한국어): https://ko.vuejs.org/
- Composition API FAQ: https://ko.vuejs.org/guide/extras/composition-api-faq.html
- Pinia: https://pinia.vuejs.org/
