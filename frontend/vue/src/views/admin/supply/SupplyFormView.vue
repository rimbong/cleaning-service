<script setup>
// 약품/소모품 품목 등록/수정 겸용 폼. props.id 없으면 등록, 있으면 수정.
// 여기서 다루는 것은 품목의 "정의"뿐이다. 재고 수량은 입출고 등록으로만 움직인다.
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { supplyService } from '@/services/admin/supply/supplyService'
import { useFormErrors } from '@/common/composables/useFormErrors'
import { useNotifyStore } from '@/stores/common/notify/notify'

const props = defineProps({ id: { type: [String, Number], default: null } })
const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

const isEdit = computed(() => props.id != null)
const loading = ref(false)
const saving = ref(false)

const form = reactive({ name: '', spec: '', unit: '', unitPrice: '', safetyQty: 0, memo: '' })

// 필드별 인라인 검증 에러
const { errors, setError, clearError, reset, hasErrors } = useFormErrors()

watch(() => props.id, async (id) => {
    if (id == null) {
        return
    }
    loading.value = true
    try {
        const item = (await supplyService.get(id)).data.data
        form.name = item.name ?? ''
        form.spec = item.spec ?? ''
        form.unit = item.unit ?? ''
        form.unitPrice = item.unitPrice ?? ''
        form.safetyQty = item.safetyQty ?? 0
        form.memo = item.memo ?? ''
    } catch (e) {
        notify.bar('품목 정보를 불러오지 못했습니다.', { color: 'red' })
    } finally {
        loading.value = false
    }
}, { immediate: true })

/** 필드별 검증 — 에러가 있으면 errors 에 담고 false 를 반환한다. */
function validate() {
    reset()
    if (!form.name.trim()) {
        setError('name', '품목명은 필수입니다.')
    }
    if (!form.unit.trim()) {
        setError('unit', '단위는 필수입니다.')
    }
    if (form.safetyQty === '' || Number(form.safetyQty) < 0) {
        setError('safetyQty', '안전재고를 0 이상으로 입력하세요.')
    }
    if (form.unitPrice !== '' && Number(form.unitPrice) < 0) {
        setError('unitPrice', '단가를 0 이상으로 입력하세요.')
    }
    return !hasErrors()
}

async function onSubmit() {
    if (!validate()) {
        return
    }
    saving.value = true
    try {
        const payload = {
            name: form.name.trim(),
            spec: form.spec.trim() || null,
            unit: form.unit.trim(),
            unitPrice: form.unitPrice === '' ? null : Number(form.unitPrice),
            safetyQty: Number(form.safetyQty),
            memo: form.memo.trim() || null,
        }
        if (isEdit.value) {
            await supplyService.update(props.id, payload)
            notify.toast('수정되었습니다.', { type: 'success' })
        } else {
            await supplyService.create(payload)
            notify.toast('등록되었습니다.', { type: 'success' })
        }
        // 목록('supplies')과 단건('supply')은 queryKey 가 달라 따로 지워야 한다.
        // 단건을 안 지우면 이력 화면 상단 요약(품목명·단위)이 수정 전 값으로 남는다.
        queryClient.invalidateQueries({ queryKey: ['supplies'] })
        queryClient.invalidateQueries({ queryKey: ['supply'] })
        router.replace({ name: 'admin-supplies' })
    } catch (e) {
        notify.bar(e.response?.data?.message ?? '저장 실패', { color: 'red' })
    } finally {
        saving.value = false
    }
}
</script>

<template>
    <section class="form-page">
        <p v-if="loading" class="state">불러오는 중…</p>
        <form v-else class="card" @submit.prevent="onSubmit">
            <div class="row">
                <div class="field" :class="{ 'has-error': errors.name }">
                    <label>품목명 <span class="req">*</span></label>
                    <input v-model="form.name" maxlength="100" placeholder="예: 락스" @input="clearError('name')" />
                    <p v-if="errors.name" class="err-msg">{{ errors.name }}</p>
                </div>
                <div class="field">
                    <label>규격</label>
                    <input v-model="form.spec" maxlength="50" placeholder="예: 20L 말통" />
                    <p class="hint">같은 약품이라도 용량이 다르면 규격으로 구분합니다.</p>
                </div>
            </div>
            <div class="row">
                <div class="field" :class="{ 'has-error': errors.unit }">
                    <label>단위 <span class="req">*</span></label>
                    <input v-model="form.unit" maxlength="20" placeholder="예: 통" @input="clearError('unit')" />
                    <p v-if="errors.unit" class="err-msg">{{ errors.unit }}</p>
                </div>
                <div class="field" :class="{ 'has-error': errors.unitPrice }">
                    <label>단가(원)</label>
                    <input v-model="form.unitPrice" type="number" min="0" step="1" placeholder="예: 12000" @input="clearError('unitPrice')" />
                    <p v-if="errors.unitPrice" class="err-msg">{{ errors.unitPrice }}</p>
                </div>
            </div>
            <div class="field" :class="{ 'has-error': errors.safetyQty }">
                <label>안전재고 <span class="req">*</span></label>
                <input v-model="form.safetyQty" type="number" min="0" step="1" @input="clearError('safetyQty')" />
                <p v-if="errors.safetyQty" class="err-msg">{{ errors.safetyQty }}</p>
                <p v-else class="hint">재고가 이 수량 아래로 떨어지면 목록에 '부족'으로 표시됩니다.</p>
            </div>
            <div class="field">
                <label>메모</label>
                <input v-model="form.memo" maxlength="255" placeholder="예: 00상회에서 구입" />
            </div>
            <p v-if="!isEdit" class="notice">
                재고 수량은 여기서 넣지 않습니다. 품목을 등록한 뒤 목록에서 '입고'로 넣으세요.
            </p>
            <div class="actions">
                <button class="btn btn--ghost" type="button" @click="router.back()">취소</button>
                <button class="btn btn--primary" type="submit" :disabled="saving">{{ saving ? '저장 중…' : (isEdit ? '수정' : '등록') }}</button>
            </div>
        </form>
    </section>
</template>

<style scoped>
.form-page { max-width: 680px; margin: 0 auto; }
.card { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); padding: 1.5rem; display: flex; flex-direction: column; gap: 1.1rem; }
.row { display: grid; grid-template-columns: 1fr 1fr; gap: 1.1rem; }
@media (max-width: 560px) { .row { grid-template-columns: 1fr; } }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field label { font-size: 0.85rem; color: var(--text); font-weight: 600; }
.req { color: var(--danger); }
.field input { padding: 0.55rem 0.7rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; color: var(--text-h); background: #fff; }
.field input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.field.has-error input { border-color: var(--danger); }
.field.has-error input:focus { border-color: var(--danger); box-shadow: 0 0 0 3px var(--danger-soft); }
.err-msg { margin: 0; color: var(--danger); font-size: 0.78rem; }
.hint { margin: 0; font-size: 0.78rem; color: var(--text); }
.notice { margin: 0; padding: 0.6rem 0.8rem; background: var(--muted); border-radius: var(--radius); font-size: 0.82rem; color: var(--text); }
.actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.5rem; }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
</style>
