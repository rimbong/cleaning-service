<script setup>
// 거래처 등록/수정 겸용 폼.
//  - props.id 없음 → 등록,  있음 → 수정(기존 값 로드).
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { clientService, CLEANING_TYPES, TAX_INVOICE_TYPES } from '@/services/admin/client/clientService'
import { invalidatePricingReview } from '@/services/admin/pricing/pricingCache'
import { useFormErrors } from '@/common/composables/useFormErrors'
import { useNotifyStore } from '@/stores/common/notify/notify'

const props = defineProps({
    id: { type: [String, Number], default: null },
})

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

const isEdit = computed(() => props.id != null)
const loading = ref(false)
const saving = ref(false)

// 필드별 인라인 검증 에러
const { errors, setError, clearError, reset, hasErrors } = useFormErrors()

const form = reactive({
    name: '',
    address: '',
    managerName: '',
    managerPhone: '',
    cleaningType: '',
    contractStartDate: '',
    memo: '',
    floors: '',
    householdCount: '',
    sharedToilets: '',
    extraFloors: '',
    hasElevator: false,
    businessNumber: '',
    representativeName: '',
    businessType: '',
    businessItem: '',
    taxInvoiceType: '',
})

// 수정 모드면 기존 값 로드 — props.id 를 명시적으로 추적(watchEffect 는 await 이후 접근한
// props.id 를 의존으로 못 잡아, 라우트 파라미터만 바뀌어 컴포넌트가 재사용되면 갱신이 안 됨)
watch(() => props.id, async (id) => {
    if (id == null) {
        return
    }
    loading.value = true
    try {
        const res = await clientService.get(id)
        const c = res.data.data
        form.name = c.name ?? ''
        form.address = c.address ?? ''
        form.managerName = c.managerName ?? ''
        form.managerPhone = c.managerPhone ?? ''
        form.cleaningType = c.cleaningType ?? ''
        form.contractStartDate = c.contractStartDate ?? ''
        form.memo = c.memo ?? ''
        form.floors = c.floors ?? ''
        form.householdCount = c.householdCount ?? ''
        form.sharedToilets = c.sharedToilets ?? ''
        form.extraFloors = c.extraFloors ?? ''
        form.hasElevator = c.hasElevator ?? false
        form.businessNumber = c.businessNumber ?? ''
        form.representativeName = c.representativeName ?? ''
        form.businessType = c.businessType ?? ''
        form.businessItem = c.businessItem ?? ''
        form.taxInvoiceType = c.taxInvoiceType ?? ''
    } catch (e) {
        notify.bar('거래처 정보를 불러오지 못했습니다.', { color: 'red' })
    } finally {
        loading.value = false
    }
}, { immediate: true })

/**
 * 숫자 입력을 서버로 보낼 값으로 바꾼다.
 * 빈칸(미실측)과 0 을 반드시 구분해야 한다 — 빈칸을 0 으로 보내면
 * "0층 0세대 건물"로 저장되어 권장가가 실제보다 훨씬 낮게 나온다.
 *
 * @param {string|number} value 입력값
 * @returns {number|null} 비었으면 null
 */
function numberOrNull(value) {
    if (value === '' || value === null || value === undefined) {
        return null
    }
    return Number(value)
}

function buildPayload() {
    // 빈 문자열은 null 로 보내 서버에서 NULL 로 저장되게 한다(선택 항목).
    return {
        name: form.name.trim(),
        address: form.address.trim() || null,
        managerName: form.managerName.trim() || null,
        managerPhone: form.managerPhone.trim() || null,
        cleaningType: form.cleaningType || null,
        contractStartDate: form.contractStartDate || null,
        memo: form.memo.trim() || null,
        // 건물 규모 — 실측 전이면 빈칸이고, 0 과 구분해야 하므로 빈칸은 null 로 보낸다.
        //   (0 층으로 저장되면 권장가가 실제보다 낮게 나온다)
        floors: numberOrNull(form.floors),
        householdCount: numberOrNull(form.householdCount),
        sharedToilets: numberOrNull(form.sharedToilets),
        extraFloors: numberOrNull(form.extraFloors),
        hasElevator: form.hasElevator,
        businessNumber: form.businessNumber.trim() || null,
        representativeName: form.representativeName.trim() || null,
        businessType: form.businessType.trim() || null,
        businessItem: form.businessItem.trim() || null,
        taxInvoiceType: form.taxInvoiceType || null,
    }
}

/** 필드별 검증 — 에러가 있으면 errors 에 담고 false 를 반환한다. */
function validate() {
    reset()
    if (!form.name.trim()) {
        setError('name', '건물명은 필수입니다.')
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
            saved = await clientService.update(props.id, payload)
            notify.toast('수정되었습니다.', { type: 'success' })
        } else {
            saved = await clientService.create(payload)
            notify.toast('등록되었습니다.', { type: 'success' })
        }
        queryClient.invalidateQueries({ queryKey: ['clients'] }) // 목록 캐시
        queryClient.invalidateQueries({ queryKey: ['client'] })  // 상세 캐시(단건) — 수정분 즉시 반영
        // 건물 규모가 바뀌면 권장가뿐 아니라 재산정 검토 대상 여부까지 달라진다.
        invalidatePricingReview(queryClient)
        const id = saved.data.data.id
        router.replace({ name: 'admin-client-detail', params: { id } })
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
            <div class="field" :class="{ 'has-error': errors.name }">
                <label>건물명 <span class="req">*</span></label>
                <input v-model="form.name" placeholder="예: 행복빌라" maxlength="100" @input="clearError('name')" />
                <p v-if="errors.name" class="err-msg">{{ errors.name }}</p>
            </div>

            <div class="row">
                <div class="field">
                    <label>청소 종류</label>
                    <select v-model="form.cleaningType">
                        <option value="">선택 안 함</option>
                        <option v-for="t in CLEANING_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
                    </select>
                </div>
                <div class="field">
                    <label>계약 시작일</label>
                    <input v-model="form.contractStartDate" type="date" />
                </div>
            </div>

            <div class="field">
                <label>주소</label>
                <input v-model="form.address" placeholder="건물 주소" maxlength="255" />
            </div>

            <div class="row">
                <div class="field">
                    <label>담당자명</label>
                    <input v-model="form.managerName" placeholder="담당자 이름" maxlength="50" />
                </div>
                <div class="field">
                    <label>연락처</label>
                    <input v-model="form.managerPhone" placeholder="010-0000-0000" maxlength="30" />
                </div>
            </div>

            <div class="section-label">건물 규모 (선택) — 계단청소 권장가 산정에 사용</div>
            <p class="section-hint">
                층수와 세대수를 넣어두면 견적 화면에서 권장가가 자동으로 계산되고,
                나중에 적정가 재산정(인상 검토) 대상이 됩니다. 아직 실측 전이면 비워 두세요.
            </p>
            <div class="row">
                <div class="field">
                    <label>지상 층수</label>
                    <input v-model="form.floors" type="number" min="0" max="100" step="1" placeholder="예: 5" />
                </div>
                <div class="field">
                    <label>세대수 (호실 수)</label>
                    <input v-model="form.householdCount" type="number" min="0" max="1000" step="1" placeholder="예: 10" />
                </div>
            </div>
            <div class="row">
                <div class="field">
                    <label>공용 화장실 (개)</label>
                    <input v-model="form.sharedToilets" type="number" min="0" max="100" step="1" placeholder="없으면 0" />
                </div>
                <div class="field">
                    <label>지하·옥상 추가층</label>
                    <input v-model="form.extraFloors" type="number" min="0" max="20" step="1" placeholder="없으면 0" />
                </div>
            </div>
            <div class="field field--check">
                <label class="check">
                    <input v-model="form.hasElevator" type="checkbox" />
                    <span>엘리베이터 있음 (내부 바닥·거울·버튼판 청소 포함)</span>
                </label>
            </div>

            <div class="section-label">세금계산서/사업자 정보 (선택)</div>
            <div class="row">
                <div class="field">
                    <label>사업자번호</label>
                    <input v-model="form.businessNumber" placeholder="000-00-00000" maxlength="20" />
                </div>
                <div class="field">
                    <label>대표자명</label>
                    <input v-model="form.representativeName" maxlength="50" />
                </div>
            </div>
            <div class="row">
                <div class="field">
                    <label>업태</label>
                    <input v-model="form.businessType" placeholder="예: 임대업" maxlength="50" />
                </div>
                <div class="field">
                    <label>종목</label>
                    <input v-model="form.businessItem" maxlength="50" />
                </div>
            </div>
            <div class="field">
                <label>세금계산서 발행 방식</label>
                <select v-model="form.taxInvoiceType">
                    <option value="">선택 안 함</option>
                    <option v-for="t in TAX_INVOICE_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
                </select>
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

.section-label {
    font-size: 0.85rem;
    font-weight: 700;
    color: var(--text-h);
    border-top: 1px solid var(--border);
    padding-top: 1rem;
    margin-top: 0.25rem;
}

.section-hint {
    margin: -0.6rem 0 0;
    font-size: 0.78rem;
    color: var(--text);
    line-height: 1.5;
}

/* 체크박스는 라벨과 가로로 붙여야 자연스럽다(다른 field 는 세로 배치) */
.field--check {
    margin-top: -0.3rem;
}

.check {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.85rem;
    font-weight: 500;
    color: var(--text-h);
    cursor: pointer;
}

.check input {
    width: auto;
    margin: 0;
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
