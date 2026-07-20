<script setup>
// 계약 등록/수정 겸용 폼.
//  - props.id 없음 → 등록,  있음 → 수정(기존 값 로드).
//  - 거래처(건물)는 검색 가능한 모달(ClientPickerField)로 선택한다.
import { computed, reactive, ref, watch, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { contractService, CONTRACT_STATUSES, WEEKDAYS, CLEANING_CYCLES, VAT_TYPES } from '@/services/admin/contract/contractService'
import ClientPickerField from '@/views/admin/client/ClientPickerField.vue'
import { useFormErrors } from '@/common/composables/useFormErrors'
import { useNotifyStore } from '@/stores/common/notify/notify'

const props = defineProps({
    id: { type: [String, Number], default: null },
})

const route = useRoute()
const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

const isEdit = computed(() => props.id != null)
const loading = ref(false)
const saving = ref(false)

// 필드별 인라인 검증 에러
const { errors, setError, clearError, reset, hasErrors } = useFormErrors()

const form = reactive({
    clientId: '',
    title: '',
    monthlyFee: '',
    billingDay: '',
    startDate: '',
    endDate: '',
    status: 'ACTIVE',
    documentLocation: '',
    paymentMethod: '',
    doorCode: '',
    cleaningWeekdays: [],
    cleaningCycle: 'WEEKLY',
    visitsPerMonth: '',
    vatType: 'EXCLUSIVE',
    initialFee: '',
    cleaningScope: '',
    serviceItems: '',
    extraServices: '',
    extraNotes: '',
    memo: '',
})

/** 청소 요일 체크박스 토글 */
function toggleWeekday(code) {
    const i = form.cleaningWeekdays.indexOf(code)
    if (i >= 0) {
        form.cleaningWeekdays.splice(i, 1)
    } else {
        form.cleaningWeekdays.push(code)
    }
}

// ===== 월 방문 횟수 =====
// 요일로 떨어지는 계약(매주·격주)은 자동으로 계산된다.
// 요일로 떨어지지 않는 계약은 곧 매월(비정기)이므로, 그때만 숫자를 직접 받는다.
// 별도의 "직접 지정" 스위치는 두지 않는다 — 매주 월요일인데 월 3회 같은 조합은
// 애초에 성립하지 않아서, 스위치가 있으면 서로 모순되는 값이 저장될 수 있다.

/** 한 달을 몇 주로 보는지 — 서버(VisitFrequency.WEEKS_PER_MONTH)와 같아야 한다. */
const WEEKS_PER_MONTH = 4

/**
 * 매월 계약은 요일로 적을 수 없다.
 * "매월 첫째주 수요일, 넷째주 금요일" 같은 패턴이라 요일 하나로는 표현이 안 되고,
 * 요일을 골라두면 매주 가는 것으로 오해된다. 그래서 요일 선택을 막고
 * 방문 횟수를 직접 넣게 한 뒤 상세 일정은 메모에 적는다.
 */
const isMonthly = computed(() => form.cleaningCycle === 'MONTHLY')

/**
 * 요일·주기로 계산한 월 방문 횟수.
 * 요일은 "무슨 요일에 가는가", 주기는 "그 요일들을 얼마나 자주 반복하는가"이므로
 * 요일 개수를 곱하고 배수만 다르다(매주 4 / 격주 2).
 * 매월은 요일로 못 적으므로 자동 계산 대상이 아니고 기본 1회로 둔다.
 */
const suggestedVisits = computed(() => {
    if (isMonthly.value) {
        return 1
    }
    const days = form.cleaningWeekdays.length || 1
    return days * (form.cleaningCycle === 'BIWEEKLY' ? WEEKS_PER_MONTH / 2 : WEEKS_PER_MONTH)
})

/** 지금 고른 요일·주기가 무슨 뜻인지 한 문장으로 — "매주 월·목이면 월 8회" 처럼 바로 확인되게 */
const scheduleSummary = computed(() => {
    const cycleLabel = CLEANING_CYCLES.find((c) => c.value === form.cleaningCycle)?.label ?? ''
    if (isMonthly.value) {
        const n = form.visitsPerMonth === '' ? suggestedVisits.value : form.visitsPerMonth
        return `${cycleLabel} → 월 ${n}회 (상세 일정은 메모에)`
    }
    const picked = WEEKDAYS.filter((d) => form.cleaningWeekdays.includes(d.value)).map((d) => d.label)
    if (picked.length === 0) {
        return `${cycleLabel} (요일 미선택) → 월 ${suggestedVisits.value}회`
    }
    return `${cycleLabel} ${picked.join('·')} → 월 ${suggestedVisits.value}회`
})

// 주기가 바뀌면 그에 맞게 정리한다.
//   매월  : 요일은 의미가 없으므로 비우고, 횟수 입력을 1회부터 시작
//   매주/격주 : 요일로 자동 계산되므로 직접 넣었던 횟수는 버린다
watch(isMonthly, (monthly) => {
    if (monthly) {
        form.cleaningWeekdays = []
        if (form.visitsPerMonth === '') {
            form.visitsPerMonth = 1
        }
    } else {
        form.visitsPerMonth = ''
    }
})

// 등록 화면에 ?clientId=... 로 진입하면(거래처 상세의 "계약 추가") 해당 거래처를 미리 선택.
watchEffect(() => {
    if (!isEdit.value && route.query.clientId && !form.clientId) {
        form.clientId = Number(route.query.clientId)
    }
})

// 수정 모드면 기존 값 로드 — props.id 를 명시적으로 추적(watchEffect 는 await 이후 접근한
// props.id 를 의존으로 못 잡아, 라우트 파라미터만 바뀌어 컴포넌트가 재사용되면 갱신이 안 됨)
watch(() => props.id, async (id) => {
    if (id == null) {
        return
    }
    loading.value = true
    try {
        const res = await contractService.get(id)
        const c = res.data.data
        form.clientId = c.clientId ?? ''
        form.title = c.title ?? ''
        form.monthlyFee = c.monthlyFee ?? ''
        form.billingDay = c.billingDay ?? ''
        form.startDate = c.startDate ?? ''
        form.endDate = c.endDate ?? ''
        form.status = c.status ?? 'ACTIVE'
        form.documentLocation = c.documentLocation ?? ''
        form.paymentMethod = c.paymentMethod ?? ''
        form.doorCode = c.doorCode ?? ''
        form.cleaningWeekdays = Array.isArray(c.cleaningWeekdays) ? [...c.cleaningWeekdays] : []
        form.cleaningCycle = c.cleaningCycle ?? 'WEEKLY'
        // 횟수는 매월 계약에서만 쓴다. 매주·격주는 요일로 계산되므로 화면에 담아두지 않는다.
        form.visitsPerMonth = c.cleaningCycle === 'MONTHLY' ? (c.visitsPerMonth ?? 1) : ''
        form.vatType = c.vatType ?? 'EXCLUSIVE'
        form.initialFee = c.initialFee ?? ''
        form.cleaningScope = c.cleaningScope ?? ''
        form.serviceItems = c.serviceItems ?? ''
        form.extraServices = c.extraServices ?? ''
        form.extraNotes = c.extraNotes ?? ''
        form.memo = c.memo ?? ''
    } catch (e) {
        notify.bar('계약 정보를 불러오지 못했습니다.', { color: 'red' })
    } finally {
        loading.value = false
    }
}, { immediate: true })

function buildPayload() {
    // 빈 문자열은 null 로 보내 서버에서 NULL 로 저장되게 한다(선택 항목).
    return {
        clientId: form.clientId ? Number(form.clientId) : null,
        title: form.title.trim(),
        monthlyFee: form.monthlyFee !== '' ? Number(form.monthlyFee) : null,
        billingDay: form.billingDay !== '' ? Number(form.billingDay) : null,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        status: form.status || 'ACTIVE',
        documentLocation: form.documentLocation.trim() || null,
        paymentMethod: form.paymentMethod.trim() || null,
        doorCode: form.doorCode.trim() || null,
        cleaningWeekdays: form.cleaningWeekdays,
        cleaningCycle: form.cleaningCycle || 'WEEKLY',
        // 매월 계약만 횟수를 보낸다. 매주·격주는 null 을 보내 서버가 요일로 환산하게 한다.
        // 같은 값을 두 곳에 저장해 두면 나중에 한쪽만 고쳐져 어긋난다.
        visitsPerMonth: isMonthly.value && form.visitsPerMonth !== ''
            ? Number(form.visitsPerMonth)
            : null,
        vatType: form.vatType || 'EXCLUSIVE',
        initialFee: form.initialFee !== '' ? Number(form.initialFee) : null,
        cleaningScope: form.cleaningScope.trim() || null,
        serviceItems: form.serviceItems.trim() || null,
        extraServices: form.extraServices.trim() || null,
        extraNotes: form.extraNotes.trim() || null,
        memo: form.memo.trim() || null,
    }
}

/** 필드별 검증 — 에러가 있으면 errors 에 담고 false 를 반환한다. */
function validate() {
    reset()
    if (!form.clientId) {
        setError('clientId', '거래처를 선택하세요.')
    }
    if (!form.title.trim()) {
        setError('title', '계약명은 필수입니다.')
    }
    if (form.monthlyFee === '' || Number(form.monthlyFee) < 0) {
        setError('monthlyFee', '월 청구금액을 0 이상으로 입력하세요.')
    }
    if (!form.startDate) {
        setError('startDate', '계약 시작일은 필수입니다.')
    }
    // type="date" 값은 YYYY-MM-DD 문자열이라 사전식 비교로 대소 비교 가능
    if (form.endDate && form.startDate && form.endDate < form.startDate) {
        setError('endDate', '계약 종료일은 시작일과 같거나 이후여야 합니다.')
    }
    // 매월 계약은 요일로 계산할 수 없으므로 횟수가 없으면 권장가를 낼 수 없다.
    if (isMonthly.value && (form.visitsPerMonth === '' || Number(form.visitsPerMonth) < 1)) {
        setError('visitsPerMonth', '매월 계약은 월 방문 횟수를 1 이상 입력하세요.')
    }
    return !hasErrors()
}

async function onSubmit() {
    if (!validate()) {
        return
    }
    saving.value = true
    try {
        const payload = buildPayload()
        let saved
        if (isEdit.value) {
            saved = await contractService.update(props.id, payload)
            notify.toast('수정되었습니다.', { type: 'success' })
        } else {
            saved = await contractService.create(payload)
            notify.toast('등록되었습니다.', { type: 'success' })
        }
        queryClient.invalidateQueries({ queryKey: ['contracts'] }) // 목록 캐시
        queryClient.invalidateQueries({ queryKey: ['contract'] })  // 상세 캐시(단건) — 수정분 즉시 반영
        const id = saved.data.data.id
        router.replace({ name: 'admin-contract-detail', params: { id } })
    } catch (e) {
        notify.bar(e.response?.data?.message ?? '저장에 실패했습니다.', { color: 'red' })
    } finally {
        saving.value = false
    }
}

function onCancel() {
    router.back()
}
</script>

<template>
    <section class="form-page">
        <p v-if="loading" class="state">불러오는 중…</p>

        <form v-else class="card" @submit.prevent="onSubmit">
            <div class="field" :class="{ 'has-error': errors.clientId }">
                <label>거래처 <span class="req">*</span></label>
                <ClientPickerField
                    v-model="form.clientId"
                    :invalid="!!errors.clientId"
                    @update:model-value="clearError('clientId')"
                />
                <p v-if="errors.clientId" class="err-msg">{{ errors.clientId }}</p>
            </div>

            <div class="field" :class="{ 'has-error': errors.title }">
                <label>계약명 <span class="req">*</span></label>
                <input v-model="form.title" placeholder="예: 2026년 정기 계단청소" maxlength="100" @input="clearError('title')" />
                <p v-if="errors.title" class="err-msg">{{ errors.title }}</p>
            </div>

            <div class="row">
                <div class="field" :class="{ 'has-error': errors.monthlyFee }">
                    <label>월 청구금액(원) <span class="req">*</span></label>
                    <input v-model="form.monthlyFee" type="number" min="0" step="1" placeholder="예: 150000" @input="clearError('monthlyFee')" />
                    <p v-if="errors.monthlyFee" class="err-msg">{{ errors.monthlyFee }}</p>
                </div>
                <div class="field">
                    <label>청구일(매월)</label>
                    <input v-model="form.billingDay" type="number" min="1" max="31" placeholder="1~31" />
                </div>
            </div>

            <div class="row">
                <div class="field" :class="{ 'has-error': errors.startDate }">
                    <label>계약 시작일 <span class="req">*</span></label>
                    <input v-model="form.startDate" type="date" @input="clearError('startDate')" />
                    <p v-if="errors.startDate" class="err-msg">{{ errors.startDate }}</p>
                </div>
                <div class="field" :class="{ 'has-error': errors.endDate }">
                    <label>계약 종료일</label>
                    <input v-model="form.endDate" type="date" @input="clearError('endDate')" />
                    <p v-if="errors.endDate" class="err-msg">{{ errors.endDate }}</p>
                </div>
            </div>

            <div class="field">
                <label>상태</label>
                <select v-model="form.status">
                    <option v-for="s in CONTRACT_STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
                </select>
            </div>

            <div class="field">
                <label>계약서 보관 위치</label>
                <input v-model="form.documentLocation" placeholder="예: 캐비닛 A-3, 공유드라이브 링크" maxlength="255" />
                <small class="hint">종이 원본 등 실물 보관 위치. 스캔 파일은 등록 후 상세에서 첨부하세요.</small>
            </div>

            <div class="row">
                <div class="field">
                    <label>수금 방법</label>
                    <input v-model="form.paymentMethod" placeholder="예: 현금, 신한, 국민" maxlength="30" />
                </div>
                <div class="field">
                    <label>출입문 비번</label>
                    <input v-model="form.doorCode" placeholder="운영 메모" maxlength="50" />
                </div>
            </div>

            <div class="row">
                <div class="field">
                    <label>청소 요일 <small class="hint">(정기 청소 실행 요일, 복수 선택)</small></label>
                    <div class="weekdays">
                        <button
                            v-for="d in WEEKDAYS"
                            :key="d.value"
                            type="button"
                            class="wd"
                            :class="{ 'is-on': form.cleaningWeekdays.includes(d.value) }"
                            :disabled="isMonthly"
                            @click="toggleWeekday(d.value)"
                        >
                            {{ d.label }}
                        </button>
                    </div>
                    <p v-if="isMonthly" class="hint">
                        매월 계약은 요일로 적을 수 없습니다.
                        아래에 월 방문 횟수를 넣고, 상세 일정은 메모에 적으세요.
                    </p>
                </div>
                <div class="field">
                    <label>청소 주기</label>
                    <select v-model="form.cleaningCycle">
                        <option v-for="c in CLEANING_CYCLES" :key="c.value" :value="c.value">{{ c.label }}</option>
                    </select>
                </div>
            </div>

            <!-- 매주·격주는 요일로 계산되므로 결과만 보여준다.
                 매월은 요일로 적을 수 없으므로 그때만 횟수를 직접 받는다. -->
            <div class="field">
                <label>월 방문 횟수 <small class="hint">(권장가 산정에 사용)</small></label>

                <div v-if="isMonthly" class="visits" :class="{ 'has-error': errors.visitsPerMonth }">
                    <input
                        v-model="form.visitsPerMonth"
                        type="number"
                        min="1"
                        max="31"
                        step="1"
                        @input="clearError('visitsPerMonth')"
                    />
                    <span class="visits__unit">회 / 월</span>
                </div>
                <p v-else class="summary">{{ scheduleSummary }}</p>

                <p v-if="errors.visitsPerMonth" class="err-msg">{{ errors.visitsPerMonth }}</p>
                <p v-else-if="isMonthly" class="warn-msg">
                    매월 계약은 요일로 적을 수 없어 횟수를 직접 넣습니다.
                    메모에 <b>매월 첫째주 수요일, 넷째주 금요일</b> 처럼 실제 일정을 적어두세요.
                    청소 스케줄 화면에서는 요일 칸이 아니라 별도 목록에 표시됩니다.
                </p>
                <p v-else class="hint">
                    요일과 주기로 자동 계산됩니다. 요일을 여러 개 고르면 그만큼 늘어납니다(매주 월·목 = 월 8회).
                    요일로 떨어지지 않는 계약은 주기를 <b>매월</b>로 바꾸고 횟수를 직접 넣으세요.
                </p>
            </div>

            <div class="field">
                <label>부가세 기준</label>
                <select v-model="form.vatType">
                    <option v-for="v in VAT_TYPES" :key="v.value" :value="v.value">{{ v.label }}</option>
                </select>
                <small class="hint">세금계산서 발행 시 공급가액·세액 계산 기준. 별도=청구액이 공급가액, 포함=청구액에 부가세 포함, 면세=세액 없음.</small>
            </div>

            <div class="row">
                <div class="field">
                    <label>초도청소비(원)</label>
                    <input v-model="form.initialFee" type="number" min="0" step="1" placeholder="최초 1회 청소비(선택)" />
                </div>
                <div class="field">
                    <label>청소 범위</label>
                    <input v-model="form.cleaningScope" placeholder="예: 지하1층~지상4층 건물내부" maxlength="255" />
                </div>
            </div>

            <div class="field">
                <label>기본 서비스 항목</label>
                <input v-model="form.serviceItems" placeholder="예: 현관, 계단, 창틀, 우편함, 화장실" maxlength="255" />
            </div>

            <div class="field">
                <label>추가 서비스 항목</label>
                <input v-model="form.extraServices" placeholder="기본 서비스 외에 따로 합의한 작업(선택)" maxlength="255" />
            </div>

            <div class="field">
                <label>계약서 추가사항</label>
                <input v-model="form.extraNotes" placeholder="특약 등, 계약서에 인쇄될 내용(선택)" maxlength="255" />
                <small class="hint">계약서(HWP)의 "추가사항" 칸에 그대로 인쇄됩니다. 내부용 내용은 아래 메모에 적으세요.</small>
            </div>

            <div class="field">
                <label>메모</label>
                <textarea v-model="form.memo" rows="4" placeholder="내부용 메모(계약서에는 인쇄되지 않습니다)"></textarea>
            </div>

            <div class="actions">
                <button class="btn btn--ghost" type="button" @click="onCancel">취소</button>
                <button class="btn btn--primary" type="submit" :disabled="saving">
                    {{ saving ? '저장 중…' : (isEdit ? '수정' : '등록') }}
                </button>
            </div>
        </form>
    </section>
</template>

<style scoped>
.form-page {
    max-width: 680px;
    margin: 0 auto;
}

.card {
    background: #fff;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.1rem;
}

.row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.1rem;
}

@media (max-width: 560px) {
    .row {
        grid-template-columns: 1fr;
    }
}

.field {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.field label {
    font-size: 0.85rem;
    color: var(--text);
    font-weight: 600;
}

.req {
    color: var(--danger);
}

.field input,
.field select,
.field textarea {
    padding: 0.55rem 0.7rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    font: inherit;
    color: var(--text-h);
    background: #fff;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
    outline: none;
    border-color: var(--primary);
    box-shadow: 0 0 0 3px var(--primary-soft);
}

/* 검증 실패 필드 — 테두리 강조 + 인라인 에러 메시지 */
.field.has-error input,
.field.has-error select,
.field.has-error textarea {
    border-color: var(--danger);
}

.field.has-error input:focus,
.field.has-error select:focus,
.field.has-error textarea:focus {
    border-color: var(--danger);
    box-shadow: 0 0 0 3px var(--danger-soft);
}

.err-msg {
    margin: 0;
    color: var(--danger);
    font-size: 0.78rem;
}

.field textarea {
    resize: vertical;
}

.hint {
    color: var(--text);
    font-size: 0.78rem;
    font-weight: 400;
}

/* 월 방문 횟수 — 입력칸과 단위·되돌리기 버튼을 한 줄에 둔다 */
.visits {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.visits input {
    max-width: 6.5rem;
}

.visits__unit {
    font-size: 0.85rem;
    color: var(--text);
    white-space: nowrap;
}

/* 검증 실패 표시 — .field.has-error 는 .field 를 겨냥하므로 여기서 따로 잡아준다 */
.visits.has-error input {
    border-color: var(--danger);
}

.visits.has-error input:focus {
    border-color: var(--danger);
    box-shadow: 0 0 0 3px var(--danger-soft);
}

/* 매월 계약은 요일이 의미 없으므로 버튼을 잠근다 */
.wd:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.visits .btn {
    padding: 0.32rem 0.6rem;
    font-size: 0.78rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-h);
    cursor: pointer;
    font-family: inherit;
    white-space: nowrap;
}

.visits .btn:hover {
    border-color: var(--primary);
    color: var(--primary);
}

/* 고른 요일·주기가 무슨 뜻인지 한 문장으로 */
.summary {
    margin: 0.35rem 0 0;
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--primary);
}

/* 자동값과 다를 때의 안내 — 실수로 남은 값을 알아채게 한다 */
.warn-msg {
    margin: 0.3rem 0 0;
    color: #b45309;
    font-size: 0.78rem;
}

.weekdays {
    display: flex;
    gap: 0.3rem;
    flex-wrap: wrap;
}

.wd {
    width: 2.2rem;
    height: 2.2rem;
    border: 1px solid var(--border);
    border-radius: 50%;
    background: #fff;
    color: var(--text);
    cursor: pointer;
    font: inherit;
    font-size: 0.85rem;
}

.wd:hover {
    border-color: var(--primary);
}

.wd.is-on {
    background: var(--primary);
    border-color: var(--primary);
    color: var(--primary-fg);
    font-weight: 700;
}

.actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    margin-top: 0.5rem;
}

/* 버튼은 전역 .btn 계열(style.css) 사용 */

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}
</style>
