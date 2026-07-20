<script setup>
// 단가 정책 설정 — 회사 정보처럼 단일 설정 화면(조회 후 수정).
//
// 이 화면이 있는 이유: 단가를 코드에 박아두면 최저임금이 오를 때마다 소스 수정·재빌드·
// 재배포가 필요해서 이미 설치된 PC 에서는 손을 댈 수가 없다. 여기서 숫자만 바꾸면 된다.
import { reactive, ref, onMounted } from 'vue'

import { pricingService } from '@/services/admin/pricing/pricingService'
import { useNotifyStore } from '@/stores/common/notify/notify'

const notify = useNotifyStore()
const loading = ref(true)
const saving = ref(false)
const updatedAt = ref(null)

const form = reactive({
    baseFee: 0,
    perFloor: 0,
    perHousehold: 0,
    perToilet: 0,
    elevatorFee: 0,
    coefMonthly1: '0.60',
    coefMonthly2: '1.00',
    coefMonthly3: '1.40',
    coefWeekly1: '1.70',
    coefWeekly2: '2.60',
    coefWeekly3: '3.40',
    roundingUnit: 1000,
    memo: '',
})

const AMOUNT_FIELDS = [
    { key: 'baseFee', label: '기본 출동료', unit: '원/월', hint: '건물당 이동·준비·소모품·관리비·최소 마진' },
    { key: 'perFloor', label: '층당 단가', unit: '원/층', hint: '계단참·난간·창틀 — 작업량의 핵심' },
    { key: 'perHousehold', label: '세대당 단가', unit: '원/세대', hint: '세대가 많을수록 늘어나는 오염·쓰레기 몫' },
    { key: 'perToilet', label: '공용 화장실', unit: '원/개', hint: '변기·세면대·바닥·거울 청소가 있을 때' },
    { key: 'elevatorFee', label: '엘리베이터 가산', unit: '원', hint: '내부 바닥·거울·버튼판 청소 포함 시' },
]

const COEF_FIELDS = [
    { key: 'coefMonthly1', label: '월 1회' },
    { key: 'coefMonthly2', label: '월 2회 (격주) — 기준' },
    { key: 'coefMonthly3', label: '월 3회' },
    { key: 'coefWeekly1', label: '주 1회 (월 4회)' },
    { key: 'coefWeekly2', label: '주 2회' },
    { key: 'coefWeekly3', label: '주 3회' },
]

onMounted(load)

async function load() {
    loading.value = true
    try {
        const p = (await pricingService.getPolicy()).data.data
        Object.keys(form).forEach((key) => {
            if (p[key] !== undefined && p[key] !== null) {
                form[key] = p[key]
            }
        })
        form.memo = p.memo ?? ''
        updatedAt.value = p.updatedAt
    } catch (e) {
        notify.bar('단가 정책을 불러오지 못했습니다.', { color: 'red' })
    } finally {
        loading.value = false
    }
}

async function onSubmit() {
    saving.value = true
    try {
        const payload = {
            baseFee: Number(form.baseFee),
            perFloor: Number(form.perFloor),
            perHousehold: Number(form.perHousehold),
            perToilet: Number(form.perToilet),
            elevatorFee: Number(form.elevatorFee),
            coefMonthly1: Number(form.coefMonthly1),
            coefMonthly2: Number(form.coefMonthly2),
            coefMonthly3: Number(form.coefMonthly3),
            coefWeekly1: Number(form.coefWeekly1),
            coefWeekly2: Number(form.coefWeekly2),
            coefWeekly3: Number(form.coefWeekly3),
            roundingUnit: Number(form.roundingUnit),
            memo: form.memo.trim() || null,
        }
        const p = (await pricingService.updatePolicy(payload)).data.data
        updatedAt.value = p.updatedAt
        notify.toast('저장되었습니다.', { type: 'success' })
    } catch (e) {
        notify.bar(e.response?.data?.message ?? '저장 실패', { color: 'red' })
    } finally {
        saving.value = false
    }
}

function fmtDateTime(v) {
    return v ? String(v).slice(0, 16).replace('T', ' ') : '-'
}
</script>

<template>
    <section class="policy-page">
        <p v-if="loading" class="state">불러오는 중…</p>
        <form v-else class="card" @submit.prevent="onSubmit">
            <div class="notice">
                여기서 바꾼 단가는 <b>앞으로 계산하는 권장가</b>에만 적용됩니다.
                이미 맺은 계약 금액은 바뀌지 않습니다. 기존 거래처 인상은 '적정가 재산정' 화면에서 검토하세요.
            </div>

            <div class="section-label">항목별 단가</div>
            <div class="grid">
                <div v-for="f in AMOUNT_FIELDS" :key="f.key" class="field">
                    <label>{{ f.label }}</label>
                    <div class="input-unit">
                        <input v-model="form[f.key]" type="number" min="0" step="100" />
                        <span class="unit">{{ f.unit }}</span>
                    </div>
                    <p class="hint">{{ f.hint }}</p>
                </div>
            </div>

            <div class="section-label">청소 주기 계수</div>
            <p class="section-hint">
                횟수를 늘려도 단순 배수가 아닙니다. 한 번 갈 때 몰아 하는 동선·준비 시간의 효율을 반영한 값입니다.
                위 단가는 <b>월 2회(격주) = 1.00</b> 을 기준으로 맞춰져 있으니, 기준을 바꾸려면 단가도 함께 조정하세요.
            </p>
            <div class="grid">
                <div v-for="f in COEF_FIELDS" :key="f.key" class="field">
                    <label>{{ f.label }}</label>
                    <input v-model="form[f.key]" type="number" min="0.1" max="99.99" step="0.05" />
                </div>
            </div>

            <div class="section-label">기타</div>
            <div class="grid">
                <div class="field">
                    <label>반올림 단위</label>
                    <div class="input-unit">
                        <input v-model="form.roundingUnit" type="number" min="1" step="100" />
                        <span class="unit">원</span>
                    </div>
                    <p class="hint">1000 이면 천원 단위로 맞춥니다.</p>
                </div>
            </div>
            <div class="field">
                <label>메모</label>
                <input v-model="form.memo" maxlength="255" placeholder="예: 2026년 최저임금(시급 10,320원) 기준" />
                <p class="hint">단가를 이렇게 정한 근거를 적어두면 나중에 조정할 때 기준이 됩니다.</p>
            </div>

            <div class="actions">
                <span class="updated">최종 수정 {{ fmtDateTime(updatedAt) }}</span>
                <button class="btn btn--primary" type="submit" :disabled="saving">
                    {{ saving ? '저장 중…' : '저장' }}
                </button>
            </div>
        </form>
    </section>
</template>

<style scoped>
.policy-page { max-width: 860px; margin: 0 auto; }
.card { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
.notice { background: #fff8e6; border: 1px solid #f0e0a8; border-radius: var(--radius); padding: 0.8rem 1rem; font-size: 0.83rem; color: #6b5900; line-height: 1.6; }
.section-label { font-size: 0.85rem; font-weight: 700; color: var(--text-h); border-top: 1px solid var(--border); padding-top: 1rem; margin-top: 0.25rem; }
.section-hint { margin: -0.6rem 0 0; font-size: 0.78rem; color: var(--text); line-height: 1.5; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field label { font-size: 0.85rem; color: var(--text); font-weight: 600; }
.field input { padding: 0.55rem 0.7rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; color: var(--text-h); background: #fff; width: 100%; }
.field input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.input-unit { display: flex; align-items: center; gap: 0.4rem; }
.unit { font-size: 0.8rem; color: var(--text); white-space: nowrap; }
.hint { margin: 0; font-size: 0.76rem; color: var(--text); line-height: 1.45; }
.actions { display: flex; justify-content: flex-end; align-items: center; gap: 0.8rem; margin-top: 0.5rem; }
.updated { font-size: 0.78rem; color: var(--text); }
.btn { padding: 0.5rem 0.9rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; }
.btn--primary { background: var(--primary); border-color: var(--primary); color: var(--primary-fg); }
.btn--primary:hover { background: var(--primary-hover); }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
</style>
