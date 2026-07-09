<script setup>
// 견적 등록/수정 겸용 폼.
//  - props.id 없음 → 등록, 있음 → 수정(기존 값 로드).
//  - 거래처는 선택(비우면 고객 정보를 직접 입력). 수정 로드는 watch 로 props.id 를 명시 추적.
import { computed, reactive, ref, watch, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuery, useQueryClient } from '@tanstack/vue-query'

import { quoteService, QUOTE_STATUSES } from '@/services/quote/quoteService'
import { clientService } from '@/services/client/clientService'
import { useNotifyStore } from '@/stores/notify/notify'

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
    customerName: '',
    customerPhone: '',
    address: '',
    title: '',
    amount: '',
    quoteDate: '',
    validUntil: '',
    status: 'PENDING',
    memo: '',
})

// 거래처 셀렉트 옵션(선택 항목) — 목록 API 는 페이징 응답이라, 큰 size 로 받아 .content 를 쓴다.
const { data: clientData } = useQuery({
    queryKey: ['clients', 'options'],
    queryFn: () => clientService.list({ size: 200 }).then((res) => res.data.data),
    staleTime: 30_000,
})
const clientOptions = computed(() => clientData.value?.content ?? [])

// 등록 화면에 ?clientId=... 로 진입하면 해당 거래처를 미리 선택.
watchEffect(() => {
    if (!isEdit.value && route.query.clientId && !form.clientId) {
        form.clientId = Number(route.query.clientId)
    }
})

// 수정 모드면 기존 값 로드 — props.id 를 명시적으로 추적.
watch(() => props.id, async (id) => {
    if (id == null) {
        return
    }
    loading.value = true
    try {
        const res = await quoteService.get(id)
        const q = res.data.data
        form.clientId = q.clientId ?? ''
        form.customerName = q.customerName ?? ''
        form.customerPhone = q.customerPhone ?? ''
        form.address = q.address ?? ''
        form.title = q.title ?? ''
        form.amount = q.amount ?? ''
        form.quoteDate = q.quoteDate ?? ''
        form.validUntil = q.validUntil ?? ''
        form.status = q.status ?? 'PENDING'
        form.memo = q.memo ?? ''
    } catch (e) {
        notify.bar('견적 정보를 불러오지 못했습니다.', { color: 'red' })
    } finally {
        loading.value = false
    }
}, { immediate: true })

function buildPayload() {
    return {
        clientId: form.clientId ? Number(form.clientId) : null,
        customerName: form.customerName.trim() || null,
        customerPhone: form.customerPhone.trim() || null,
        address: form.address.trim() || null,
        title: form.title.trim(),
        amount: form.amount !== '' ? Number(form.amount) : null,
        quoteDate: form.quoteDate || null,
        validUntil: form.validUntil || null,
        status: form.status || 'PENDING',
        memo: form.memo.trim() || null,
    }
}

async function onSubmit() {
    if (!form.title.trim()) {
        notify.bar('서비스 내용은 필수입니다.', { color: 'yellow' })
        return
    }
    if (!form.clientId && !form.customerName.trim()) {
        notify.bar('거래처를 선택하거나 고객명을 입력하세요.', { color: 'yellow' })
        return
    }
    if (form.amount === '' || Number(form.amount) < 0) {
        notify.bar('견적 금액을 올바르게 입력하세요.', { color: 'yellow' })
        return
    }
    if (!form.quoteDate) {
        notify.bar('견적일은 필수입니다.', { color: 'yellow' })
        return
    }
    // type="date" 값은 YYYY-MM-DD 문자열이라 사전식 비교로 대소 비교 가능
    if (form.validUntil && form.quoteDate && form.validUntil < form.quoteDate) {
        notify.bar('유효기간은 견적일 이후여야 합니다.', { color: 'yellow' })
        return
    }
    saving.value = true
    try {
        const payload = buildPayload()
        let saved
        if (isEdit.value) {
            saved = await quoteService.update(props.id, payload)
            notify.toast('수정되었습니다.', { type: 'success' })
        } else {
            saved = await quoteService.create(payload)
            notify.toast('등록되었습니다.', { type: 'success' })
        }
        queryClient.invalidateQueries({ queryKey: ['quotes'] }) // 목록 캐시
        queryClient.invalidateQueries({ queryKey: ['quote'] })  // 상세 캐시(단건)
        const id = saved.data.data.id
        router.replace({ name: 'admin-quote-detail', params: { id } })
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
                <label>거래처 (선택)</label>
                <select v-model="form.clientId">
                    <option value="">거래처 미연결(신규 고객)</option>
                    <option v-for="cl in clientOptions" :key="cl.id" :value="cl.id">{{ cl.name }}</option>
                </select>
                <small class="hint">기존 거래처면 선택, 아니면 아래 고객 정보를 입력하세요.</small>
            </div>

            <div class="row">
                <div class="field">
                    <label>고객명</label>
                    <input v-model="form.customerName" placeholder="고객 이름" maxlength="50" />
                </div>
                <div class="field">
                    <label>연락처</label>
                    <input v-model="form.customerPhone" placeholder="010-0000-0000" maxlength="30" />
                </div>
            </div>

            <div class="field">
                <label>현장 주소</label>
                <input v-model="form.address" placeholder="작업 현장 주소" maxlength="255" />
            </div>

            <div class="field">
                <label>서비스 내용 <span class="req">*</span></label>
                <input v-model="form.title" placeholder="예: 입주청소, 물탱크청소" maxlength="100" />
            </div>

            <div class="row">
                <div class="field">
                    <label>견적 금액(원) <span class="req">*</span></label>
                    <input v-model="form.amount" type="number" min="0" step="1" placeholder="예: 300000" />
                </div>
                <div class="field">
                    <label>상태</label>
                    <select v-model="form.status">
                        <option v-for="s in QUOTE_STATUSES" :key="s.value" :value="s.value">{{ s.label }}</option>
                    </select>
                </div>
            </div>

            <div class="row">
                <div class="field">
                    <label>견적일 <span class="req">*</span></label>
                    <input v-model="form.quoteDate" type="date" />
                </div>
                <div class="field">
                    <label>유효기간</label>
                    <input v-model="form.validUntil" type="date" />
                </div>
            </div>

            <div class="field">
                <label>메모</label>
                <textarea v-model="form.memo" rows="4" placeholder="현장 특이사항, 요청 내용 등"></textarea>
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
