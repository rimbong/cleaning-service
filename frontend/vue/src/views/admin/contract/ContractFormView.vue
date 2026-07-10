<script setup>
// 계약 등록/수정 겸용 폼.
//  - props.id 없음 → 등록,  있음 → 수정(기존 값 로드).
//  - 거래처(건물)는 셀렉트로 선택(거래처 목록을 불러와 채운다).
import { computed, reactive, ref, watch, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuery, useQueryClient } from '@tanstack/vue-query'

import { contractService, CONTRACT_STATUSES, WEEKDAYS, CLEANING_CYCLES, VAT_TYPES } from '@/services/admin/contract/contractService'
import { clientService } from '@/services/admin/client/clientService'
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
    vatType: 'EXCLUSIVE',
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

// 거래처 셀렉트 옵션 — 목록 API 는 페이징 응답이라, 큰 size 로 한 번에 받아 .content 를 쓴다.
const { data: clientData } = useQuery({
    queryKey: ['clients', 'options'],
    queryFn: () => clientService.list({ size: 200 }).then((res) => res.data.data),
    staleTime: 30_000,
})
const clientOptions = computed(() => clientData.value?.content ?? [])

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
        form.vatType = c.vatType ?? 'EXCLUSIVE'
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
        vatType: form.vatType || 'EXCLUSIVE',
        memo: form.memo.trim() || null,
    }
}

async function onSubmit() {
    if (!form.clientId) {
        notify.bar('거래처를 선택하세요.', { color: 'yellow' })
        return
    }
    if (!form.title.trim()) {
        notify.bar('계약명은 필수입니다.', { color: 'yellow' })
        return
    }
    if (form.monthlyFee === '' || Number(form.monthlyFee) < 0) {
        notify.bar('월 청구금액을 올바르게 입력하세요.', { color: 'yellow' })
        return
    }
    if (!form.startDate) {
        notify.bar('계약 시작일은 필수입니다.', { color: 'yellow' })
        return
    }
    // type="date" 값은 YYYY-MM-DD 문자열이라 사전식 비교로 대소 비교 가능
    if (form.endDate && form.startDate && form.endDate < form.startDate) {
        notify.bar('계약 종료일은 시작일 이후여야 합니다.', { color: 'yellow' })
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
            <div class="field">
                <label>거래처 <span class="req">*</span></label>
                <select v-model="form.clientId">
                    <option value="">거래처 선택</option>
                    <option v-for="cl in clientOptions" :key="cl.id" :value="cl.id">{{ cl.name }}</option>
                </select>
            </div>

            <div class="field">
                <label>계약명 <span class="req">*</span></label>
                <input v-model="form.title" placeholder="예: 2026년 정기 계단청소" maxlength="100" />
            </div>

            <div class="row">
                <div class="field">
                    <label>월 청구금액(원) <span class="req">*</span></label>
                    <input v-model="form.monthlyFee" type="number" min="0" step="1" placeholder="예: 150000" />
                </div>
                <div class="field">
                    <label>청구일(매월)</label>
                    <input v-model="form.billingDay" type="number" min="1" max="31" placeholder="1~31" />
                </div>
            </div>

            <div class="row">
                <div class="field">
                    <label>계약 시작일 <span class="req">*</span></label>
                    <input v-model="form.startDate" type="date" />
                </div>
                <div class="field">
                    <label>계약 종료일</label>
                    <input v-model="form.endDate" type="date" />
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
                            @click="toggleWeekday(d.value)"
                        >
                            {{ d.label }}
                        </button>
                    </div>
                </div>
                <div class="field">
                    <label>청소 주기</label>
                    <select v-model="form.cleaningCycle">
                        <option v-for="c in CLEANING_CYCLES" :key="c.value" :value="c.value">{{ c.label }}</option>
                    </select>
                </div>
            </div>

            <div class="field">
                <label>부가세 기준</label>
                <select v-model="form.vatType">
                    <option v-for="v in VAT_TYPES" :key="v.value" :value="v.value">{{ v.label }}</option>
                </select>
                <small class="hint">세금계산서 발행 시 공급가액·세액 계산 기준. 별도=청구액이 공급가액, 포함=청구액에 부가세 포함, 면세=세액 없음.</small>
            </div>

            <div class="field">
                <label>메모</label>
                <textarea v-model="form.memo" rows="4" placeholder="특이사항, 요청 내용 등"></textarea>
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

.field textarea {
    resize: vertical;
}

.hint {
    color: var(--text);
    font-size: 0.78rem;
    font-weight: 400;
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

.btn {
    padding: 0.55rem 1.2rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-h);
    cursor: pointer;
    font: inherit;
}

.btn--ghost {
    background: transparent;
    border-color: var(--border);
    color: var(--text);
}

.btn--primary {
    background: var(--primary);
    border-color: var(--primary);
    color: var(--primary-fg);
}

.btn--primary:hover:not(:disabled) {
    background: var(--primary-hover);
}

.btn--primary:disabled {
    opacity: 0.6;
    cursor: default;
}

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}
</style>
