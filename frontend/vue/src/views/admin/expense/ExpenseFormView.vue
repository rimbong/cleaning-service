<script setup>
// 지출 등록/수정 겸용 폼. props.id 없으면 등록, 있으면 수정.
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'

import { expenseService, EXPENSE_CATEGORIES } from '@/services/expense/expenseService'
import { useNotifyStore } from '@/stores/notify/notify'

const props = defineProps({ id: { type: [String, Number], default: null } })
const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

const isEdit = computed(() => props.id != null)
const loading = ref(false)
const saving = ref(false)

const form = reactive({ category: 'FUEL', vendorName: '', businessNumber: '', amount: '', expenseDate: '', memo: '' })

watch(() => props.id, async (id) => {
    if (id == null) return
    loading.value = true
    try {
        const e = (await expenseService.get(id)).data.data
        form.category = e.category ?? 'FUEL'
        form.vendorName = e.vendorName ?? ''
        form.businessNumber = e.businessNumber ?? ''
        form.amount = e.amount ?? ''
        form.expenseDate = e.expenseDate ?? ''
        form.memo = e.memo ?? ''
    } catch (e) { notify.bar('지출 정보를 불러오지 못했습니다.', { color: 'red' }) } finally { loading.value = false }
}, { immediate: true })

async function onSubmit() {
    if (form.amount === '' || Number(form.amount) < 0) { notify.bar('금액을 올바르게 입력하세요.', { color: 'yellow' }); return }
    if (!form.expenseDate) { notify.bar('지출일은 필수입니다.', { color: 'yellow' }); return }
    saving.value = true
    try {
        const payload = {
            category: form.category,
            vendorName: form.vendorName.trim() || null,
            businessNumber: form.businessNumber.trim() || null,
            amount: Number(form.amount),
            expenseDate: form.expenseDate,
            memo: form.memo.trim() || null,
        }
        if (isEdit.value) { await expenseService.update(props.id, payload); notify.toast('수정되었습니다.', { type: 'success' }) }
        else { await expenseService.create(payload); notify.toast('등록되었습니다.', { type: 'success' }) }
        queryClient.invalidateQueries({ queryKey: ['expenses'] })
        router.replace({ name: 'admin-expenses' })
    } catch (e) { notify.bar(e.response?.data?.message ?? '저장 실패', { color: 'red' }) } finally { saving.value = false }
}
</script>

<template>
    <section class="form-page">
        <p v-if="loading" class="state">불러오는 중…</p>
        <form v-else class="card" @submit.prevent="onSubmit">
            <div class="row">
                <div class="field">
                    <label>분류 <span class="req">*</span></label>
                    <select v-model="form.category">
                        <option v-for="c in EXPENSE_CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</option>
                    </select>
                </div>
                <div class="field">
                    <label>금액(원) <span class="req">*</span></label>
                    <input v-model="form.amount" type="number" min="0" step="1" placeholder="예: 50000" />
                </div>
            </div>
            <div class="row">
                <div class="field">
                    <label>거래처/주유소</label>
                    <input v-model="form.vendorName" maxlength="100" placeholder="예: 목동충전소" />
                </div>
                <div class="field">
                    <label>사업자번호</label>
                    <input v-model="form.businessNumber" maxlength="20" />
                </div>
            </div>
            <div class="field">
                <label>지출일 <span class="req">*</span></label>
                <input v-model="form.expenseDate" type="date" />
            </div>
            <div class="field">
                <label>메모</label>
                <input v-model="form.memo" maxlength="255" />
            </div>
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
.field input, .field select { padding: 0.55rem 0.7rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; color: var(--text-h); background: #fff; }
.field input:focus, .field select:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.5rem; }
.btn { padding: 0.55rem 1.2rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; }
.btn--ghost { background: transparent; border-color: var(--border); color: var(--text); }
.btn--primary { background: var(--primary); border-color: var(--primary); color: var(--primary-fg); }
.btn--primary:hover:not(:disabled) { background: var(--primary-hover); }
.btn--primary:disabled { opacity: 0.6; cursor: default; }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
</style>
