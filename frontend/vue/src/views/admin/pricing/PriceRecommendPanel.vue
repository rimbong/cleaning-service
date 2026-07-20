<script setup>
// 계단청소 권장가 추천 패널 — 견적/계약 화면에 끼워 쓴다.
//
// 여기서 나오는 금액은 참고용 권장가다. 실제 금액을 자동으로 덮어쓰지 않고
// '이 금액 적용'을 눌렀을 때만 부모에게 넘긴다(가격 흥정 여지를 남겨야 하므로).
import { onBeforeUnmount, reactive, ref, watch } from 'vue'

import { pricingService, PRICING_CYCLES } from '@/services/admin/pricing/pricingService'

const props = defineProps({
    /** 거래처에 저장된 건물 규모(있으면 자동으로 채운다) */
    spec: { type: Object, default: null },
    /** 비교용 현재 금액(있으면 권장가와의 차이를 보여준다) */
    currentAmount: { type: [String, Number], default: null },
})

const emit = defineEmits(['apply'])

/** 입력값이 바뀐 뒤 계산을 요청하기까지 기다리는 시간(ms) — 타이핑 중 매번 호출하지 않기 위함 */
const CALC_DEBOUNCE_MS = 400

const form = reactive({
    floors: '',
    householdCount: '',
    sharedToilets: 0,
    extraFloors: 0,
    hasElevator: false,
    cycle: 'MONTHLY_2',
})

const result = ref(null)
const calculating = ref(false)
const error = ref('')

let timer = null

/** 거래처에서 넘어온 건물 규모로 입력값을 채운다(거래처를 바꿔 고르면 다시 채워진다). */
watch(() => props.spec, (spec) => {
    if (!spec) {
        return
    }
    form.floors = spec.floors ?? ''
    form.householdCount = spec.householdCount ?? ''
    form.sharedToilets = spec.sharedToilets ?? 0
    form.extraFloors = spec.extraFloors ?? 0
    form.hasElevator = spec.hasElevator ?? false
}, { immediate: true, deep: true })

/** 층수·세대수가 채워졌을 때만 계산한다(둘이 단가의 뼈대라 없으면 의미 없는 금액이 나온다). */
function canCalculate() {
    return form.floors !== '' && form.householdCount !== ''
}

async function calculate() {
    if (!canCalculate()) {
        result.value = null
        return
    }
    calculating.value = true
    error.value = ''
    try {
        const res = await pricingService.estimate({
            floors: Number(form.floors),
            householdCount: Number(form.householdCount),
            sharedToilets: Number(form.sharedToilets) || 0,
            extraFloors: Number(form.extraFloors) || 0,
            hasElevator: form.hasElevator,
            cycle: form.cycle,
        })
        result.value = res.data.data
    } catch (e) {
        result.value = null
        error.value = e.response?.data?.message ?? '권장가를 계산하지 못했습니다.'
    } finally {
        calculating.value = false
    }
}

// 입력이 바뀌면 조금 기다렸다가 계산한다.
watch(form, () => {
    if (timer) {
        clearTimeout(timer)
    }
    timer = setTimeout(calculate, CALC_DEBOUNCE_MS)
}, { deep: true, immediate: true })

// 타이머가 남은 채로 화면이 사라지면 없는 컴포넌트를 건드리게 된다.
onBeforeUnmount(() => {
    if (timer) {
        clearTimeout(timer)
    }
})

function money(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') : '-'
}

/** 현재 금액 대비 차이(권장가가 더 높으면 양수) */
function gap() {
    const current = Number(props.currentAmount)
    if (!result.value || !props.currentAmount || Number.isNaN(current) || current <= 0) {
        return null
    }
    const diff = result.value.recommendedAmount - current
    const rate = Math.round((diff / current) * 1000) / 10
    return { diff, rate }
}
</script>

<template>
    <div class="panel">
        <div class="panel__head">
            <h3>권장가 계산</h3>
            <span class="panel__note">참고용입니다. 실제 금액은 직접 정하세요.</span>
        </div>

        <div class="grid">
            <div class="fld">
                <label>지상 층수</label>
                <input v-model="form.floors" type="number" min="0" max="100" step="1" placeholder="예: 5" />
            </div>
            <div class="fld">
                <label>세대수</label>
                <input v-model="form.householdCount" type="number" min="0" max="1000" step="1" placeholder="예: 10" />
            </div>
            <div class="fld">
                <label>공용 화장실</label>
                <input v-model="form.sharedToilets" type="number" min="0" max="100" step="1" />
            </div>
            <div class="fld">
                <label>지하·옥상 추가층</label>
                <input v-model="form.extraFloors" type="number" min="0" max="20" step="1" />
            </div>
            <div class="fld">
                <label>청소 주기</label>
                <select v-model="form.cycle">
                    <option v-for="c in PRICING_CYCLES" :key="c.value" :value="c.value">{{ c.label }}</option>
                </select>
            </div>
            <div class="fld fld--check">
                <label class="check">
                    <input v-model="form.hasElevator" type="checkbox" />
                    <span>엘리베이터</span>
                </label>
            </div>
        </div>

        <p v-if="!canCalculate()" class="hint">
            층수와 세대수를 넣으면 권장가가 계산됩니다.
            거래처에 건물 규모가 저장되어 있으면 자동으로 채워집니다.
        </p>
        <p v-else-if="error" class="err">{{ error }}</p>
        <div v-else-if="result" class="result">
            <div class="result__main">
                <div>
                    <div class="result__big">{{ money(result.recommendedAmount) }}원</div>
                    <div class="result__sub">
                        월 청소비 (부가세 별도) · 1회 방문 환산 {{ money(result.perVisitAmount) }}원
                        <span v-if="calculating"> · 계산 중…</span>
                    </div>
                </div>
                <button class="btn btn--primary" type="button" @click="emit('apply', result.recommendedAmount)">
                    이 금액 적용
                </button>
            </div>

            <div v-if="gap()" class="gap" :class="gap().diff > 0 ? 'gap--up' : 'gap--down'">
                현재 입력 금액과 차이 {{ gap().diff > 0 ? '+' : '' }}{{ money(gap().diff) }}원
                ({{ gap().rate > 0 ? '+' : '' }}{{ gap().rate }}%)
            </div>

            <ul class="breakdown">
                <li v-for="line in result.breakdown" :key="line.label">
                    <span class="bd-label">{{ line.label }}</span>
                    <span class="bd-detail">{{ line.detail }}</span>
                    <span class="bd-amount">{{ money(line.amount) }}원</span>
                </li>
                <li class="bd-coef">
                    <span class="bd-label">주기 계수</span>
                    <span class="bd-detail">x {{ result.coefficient }} ({{ result.cycleLabel }})</span>
                    <span class="bd-amount">{{ money(result.recommendedAmount) }}원</span>
                </li>
            </ul>
        </div>
    </div>
</template>

<style scoped>
.panel { background: linear-gradient(135deg, #f0f6ff, #eef2fb); border: 1px solid #cfe0fb; border-radius: var(--radius); padding: 1.1rem 1.2rem; }
.panel__head { display: flex; align-items: baseline; gap: 0.6rem; flex-wrap: wrap; margin-bottom: 0.8rem; }
.panel__head h3 { margin: 0; font-size: 1rem; color: #1e40af; }
.panel__note { font-size: 0.76rem; color: var(--text); }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 0.7rem; }
.fld { display: flex; flex-direction: column; gap: 0.3rem; }
.fld label { font-size: 0.76rem; font-weight: 700; color: #334; }
.fld input, .fld select { padding: 0.45rem 0.6rem; border: 1px solid #c3cfe0; border-radius: 8px; font: inherit; font-size: 0.9rem; background: #fff; color: var(--text-h); }
.fld input:focus, .fld select:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.fld--check { justify-content: flex-end; }
.check { display: flex; align-items: center; gap: 0.4rem; font-size: 0.85rem; font-weight: 500; cursor: pointer; }
.check input { width: auto; }
.hint { margin: 0.8rem 0 0; font-size: 0.8rem; color: var(--text); line-height: 1.5; }
.err { margin: 0.8rem 0 0; font-size: 0.82rem; color: var(--danger); }
.result { margin-top: 0.9rem; background: #fff; border: 2px solid var(--primary); border-radius: 12px; padding: 0.9rem 1rem; }
.result__main { display: flex; align-items: center; justify-content: space-between; gap: 0.8rem; flex-wrap: wrap; }
.result__big { font-size: 1.6rem; font-weight: 800; color: #1e40af; letter-spacing: -0.5px; }
.result__sub { font-size: 0.78rem; color: var(--text); margin-top: 0.15rem; }
.btn { padding: 0.5rem 0.9rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; }
.btn--primary { background: var(--primary); border-color: var(--primary); color: var(--primary-fg); }
.btn--primary:hover { background: var(--primary-hover); }
.gap { margin-top: 0.6rem; font-size: 0.82rem; font-weight: 600; padding: 0.35rem 0.6rem; border-radius: 8px; }
.gap--up { background: #fef3e2; color: #b45309; }
.gap--down { background: #e7f6ee; color: #0f9d58; }
.breakdown { list-style: none; margin: 0.8rem 0 0; padding: 0.7rem 0 0; border-top: 1px dashed var(--border); font-size: 0.8rem; }
.breakdown li { display: flex; align-items: baseline; gap: 0.5rem; padding: 0.18rem 0; }
.bd-label { min-width: 5.6rem; font-weight: 600; color: var(--text-h); }
.bd-detail { flex: 1; color: var(--text); }
.bd-amount { font-variant-numeric: tabular-nums; color: var(--text-h); }
.bd-coef { border-top: 1px solid var(--border); margin-top: 0.3rem; padding-top: 0.4rem; font-weight: 700; }
</style>
