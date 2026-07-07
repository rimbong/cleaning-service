import { ref, reactive, computed } from 'vue'
import { defineStore } from 'pinia'

/**
 * 전역 알림(알럿/컨펌/토스트/스피너) Pinia 스토어.
 *
 * ── 왜 store 인가? ──
 * 알림 UI(모달/토스트)는 화면 어디서든 띄울 수 있어야 하고, 표시 자체는 App.vue 에 한 번만
 * 둔 컴포넌트(NotifyHost)가 담당한다. 즉 "여러 화면이 공유하는 하나의 상태" → 전역 store 가 정답.
 * (반면 게시판 목록 같은 화면 전용 데이터는 컴포넌트 로컬 상태(ref)로 충분 — store 불필요)
 *
 * ── Vuex 와 비교 ──
 * Vuex: mutations(동기)/actions(비동기) 구분, commit("Notification/onOpenAlert", ...) 문자열 호출.
 * Pinia: 그냥 함수(actions)만 있고 직접 호출. 아래처럼 setup 스토어면 ref/computed 문법 그대로 사용.
 *
 * ── 레거시(구 프로젝트) 대비 개선 ──
 * 1) 콜백 방식({ text, fn }) → Promise 방식으로 변경:
 *      구:  onOpenConfirm({ text: '삭제?', fn: () => remove() })
 *      신:  if (await notify.confirm('삭제?')) { remove() }
 * 2) 구 스토어는 closingFunc 하나를 alert/confirm/alertBar 가 공유 → 동시에 뜨면 콜백이 섞이는
 *    버그 소지가 있었다. 여기서는 알림마다 자기 resolve 를 들고 있어 섞이지 않는다.
 * 3) 상태를 그대로 되돌려주는 getter(getIsShowAlert 등) 제거 — state 를 바로 쓰면 된다.
 */
export const useNotifyStore = defineStore('notify', () => {
    // ───────────────────────── Spinner ─────────────────────────
    // 단순 boolean 이 아니라 "카운터" 방식: 동시에 여러 작업이 스피너를 요청해도
    // 모두 끝나야(0이 되어야) 사라진다. boolean 이면 먼저 끝난 쪽이 꺼버리는 문제가 있다.
    const spinnerCount = ref(0)
    const isSpinnerVisible = computed(() => spinnerCount.value > 0)

    function showSpinner() {
        spinnerCount.value++
    }

    function hideSpinner() {
        spinnerCount.value = Math.max(0, spinnerCount.value - 1)
    }

    /**
     * 비동기 작업을 감싸 스피너를 자동 표시/숨김.
     * 예) await notify.withSpinner(someService.fetch())
     */
    async function withSpinner(promise) {
        showSpinner()
        try {
            return await promise
        } finally {
            hideSpinner()
        }
    }

    // ───────────────────────── Progress Bar (진행률) ─────────────────────────
    // 스피너가 "얼마나 걸릴지 모름"이라면, 프로그레스 바는 "0~100% 진행률"을 보여준다.
    // 파일 다운로드처럼 진행률을 알 수 있는 작업에 쓴다(레거시 ProgressBar 정제 이관).
    const progressState = reactive({
        visible: false,
        percent: 0,
        title: '',
    })

    /** 진행률 표시 시작 */
    function showProgress(title) {
        progressState.title = title || ''
        progressState.percent = 0
        progressState.visible = true
    }

    /** 진행률 갱신(0~100) */
    function updateProgress(percent) {
        progressState.percent = Math.min(100, Math.max(0, percent))
    }

    /** 진행률 표시 종료 */
    function hideProgress() {
        progressState.visible = false
        progressState.percent = 0
        progressState.title = ''
    }

    /** 프로그레스 바 최소 표시 시간(ms) — 작업이 순식간에 끝나도 이만큼은 보여준다 */
    const DEFAULT_PROGRESS_MIN_MS = 700

    /**
     * 진행률 콜백을 받는 작업을 감싸 프로그레스 바를 자동 표시/갱신/숨김.
     * task 는 onProgress(percent) 함수를 인자로 받는다.
     *
     * minMs: 최소 표시 시간(기본 700ms). 다운로드가 너무 빨리 끝나 바가 깜빡이고 사라지는 것을
     *        막는다 — 실제 작업은 그대로 두고, 부족한 시간만큼만 더 표시 후 닫는다.
     *
     * 예) await notify.withProgress('다운로드 중', (onProgress) =>
     *         downloadGet(url, { onProgress }))
     *
     * @param {string} title 표시 제목
     * @param {(onProgress:(p:number)=>void)=>Promise} task 진행률 콜백을 받는 비동기 작업
     * @param {number} [minMs] 최소 표시 시간(ms)
     */
    async function withProgress(title, task, minMs = DEFAULT_PROGRESS_MIN_MS) {
        showProgress(title)
        const startedAt = performance.now()
        try {
            return await task((p) => updateProgress(p))
        } finally {
            // 작업이 minMs 보다 빨리 끝났으면, 남은 시간만큼 바를 더 보여준 뒤 닫는다.
            const elapsed = performance.now() - startedAt
            if (elapsed < minMs) {
                updateProgress(100) // 끝났으니 꽉 채워 보여줌
                await new Promise((resolve) => setTimeout(resolve, minMs - elapsed))
            }
            hideProgress()
        }
    }

    // ───────────────────────── Alert (모달) ─────────────────────────
    // reactive: 관련 필드(visible/text/resolve)를 한 덩어리로 관리.
    const alertState = reactive({
        visible: false,
        text: '',
        resolve: null, // Promise 의 resolve 함수를 보관해 두었다가 닫힐 때 호출
    })

    /**
     * 알럿 모달을 띄운다. 사용자가 닫으면 resolve 되는 Promise 반환.
     * 예) await notify.alert('저장되었습니다.')  // 닫힐 때까지 대기 가능(안 기다려도 됨)
     */
    function alert(text) {
        return new Promise((resolve) => {
            // 열려 있는 상태에서 또 호출되면(비동기 흐름 겹침) 이전 대기자를 먼저 해소 —
            // 안 하면 먼저 연 쪽의 await 이후 코드가 영원히 실행되지 않는다(Promise 유실)
            if (alertState.resolve) {
                alertState.resolve()
            }
            alertState.text = text
            alertState.visible = true
            alertState.resolve = resolve
        })
    }

    /** (NotifyHost 전용) 알럿 닫기 */
    function closeAlert() {
        alertState.visible = false
        if (alertState.resolve) {
            alertState.resolve()
            alertState.resolve = null
        }
    }

    // ───────────────────────── Confirm (모달) ─────────────────────────
    const confirmState = reactive({
        visible: false,
        text: '',
        resolve: null,
    })

    /**
     * 컨펌 모달을 띄운다. 확인=true / 취소=false 로 resolve 되는 Promise 반환.
     * 예) if (await notify.confirm('정말 삭제하시겠습니까?')) { ... }
     */
    function confirm(text) {
        return new Promise((resolve) => {
            // 중복 호출 시 이전 대기자는 "취소(false)"로 해소하고 덮어쓴다(Promise 유실 방지)
            if (confirmState.resolve) {
                confirmState.resolve(false)
            }
            confirmState.text = text
            confirmState.visible = true
            confirmState.resolve = resolve
        })
    }

    /** (NotifyHost 전용) 컨펌 응답 처리 */
    function answerConfirm(result) {
        confirmState.visible = false
        if (confirmState.resolve) {
            confirmState.resolve(result)
            confirmState.resolve = null
        }
    }

    // ───────────────────────── Alert Bar (상단 부착형 배너) ─────────────────────────
    // 화면 최상단에 딱 붙어 높이가 열리며 나타나는 바(레거시 alert_bar 이관).
    // 색상으로 성격 표현: red(에러·기본), green(성공), yellow(주의), blue(안내)
    const barState = reactive({
        visible: false,
        text: '',
        color: 'red',
    })
    const DEFAULT_BAR_TIME = 3000
    let barTimer = null // 자동 닫힘 타이머(반응형 불필요)

    /**
     * 상단 알림 바를 띄운다. 지정 시간 후 자동으로 닫힌다(X 버튼으로 즉시 닫기 가능).
     * @param {string} text 표시할 텍스트
     * @param {object} [options] { color: 'red'|'green'|'yellow'|'blue', time: ms }
     * 예) notify.bar('저장되었습니다.', { color: 'green' })
     */
    function bar(text, options = {}) {
        barState.text = text
        barState.color = options.color || 'red'
        barState.visible = true
        // 연속 호출 시 기존 타이머를 리셋(레거시는 interval+timeout 이중 관리로 꼬였던 부분)
        if (barTimer) {
            clearTimeout(barTimer)
        }
        barTimer = setTimeout(() => {
            barState.visible = false
            barTimer = null
        }, options.time || DEFAULT_BAR_TIME)
    }

    /** (NotifyHost 전용) 알림 바 즉시 닫기 */
    function closeBar() {
        if (barTimer) {
            clearTimeout(barTimer)
            barTimer = null
        }
        barState.visible = false
    }

    // ───────────────────────── Toast ─────────────────────────
    // 배열(큐) 방식: 연달아 띄워도 겹쳐 쌓였다가 각자 시간이 지나면 사라진다.
    // (구 스토어는 단일 상태 + interval/timeout 이중 관리라 연타 시 꼬였음)
    const toasts = ref([]) // [{ id, text, type }]
    const DEFAULT_TOAST_TIME = 2500
    let toastSeq = 0 // 반응형일 필요 없는 단순 일련번호는 ref 로 만들지 않는다

    /**
     * 토스트 메시지를 띄운다.
     * @param {string} text 표시할 텍스트
     * @param {object} [options] { type: 'info'|'success'|'error', time: ms }
     * 예) notify.toast('삭제되었습니다.', { type: 'success' })
     */
    function toast(text, options = {}) {
        const id = ++toastSeq
        toasts.value.push({
            id,
            text,
            type: options.type || 'info',
        })
        setTimeout(() => removeToast(id), options.time || DEFAULT_TOAST_TIME)
    }

    /** (NotifyHost/timeout 전용) 토스트 제거 */
    function removeToast(id) {
        toasts.value = toasts.value.filter((item) => item.id !== id)
    }

    // setup 스토어는 노출할 것만 return (return 안 한 것은 외부에서 접근 불가 = private)
    return {
        // spinner
        isSpinnerVisible,
        showSpinner,
        hideSpinner,
        withSpinner,
        // progress
        progressState,
        showProgress,
        updateProgress,
        hideProgress,
        withProgress,
        // alert
        alertState,
        alert,
        closeAlert,
        // confirm
        confirmState,
        confirm,
        answerConfirm,
        // alert bar
        barState,
        bar,
        closeBar,
        // toast
        toasts,
        toast,
        removeToast,
    }
})
