<script setup>
// 입고/사용/조정 등록 모달 — 재고 현황·입출고 이력 두 화면이 같이 쓴다.
// 등록 후 캐시 무효화까지 여기서 책임진다(부르는 쪽마다 빠뜨리면 화면이 옛 값으로 남는다).
import { computed, reactive } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'

import Modal from '@/common/components/common/Modal.vue'
import { supplyService, SUPPLY_TX_TYPES } from '@/services/admin/supply/supplyService'
import { invalidateSupplyCaches } from '@/services/admin/supply/supplyCache'
import { useNotifyStore } from '@/stores/common/notify/notify'

const props = defineProps({
    /** 대상 품목 — id·name·unit·currentQuantity 를 쓴다 */
    item: { type: Object, required: true },
    /** 모달을 열 때의 기본 구분(입고/사용/조정) */
    initialType: { type: String, default: 'IN' },
})

const emit = defineEmits(['close'])

const notify = useNotifyStore()
const queryClient = useQueryClient()

/** 오늘 날짜(YYYY-MM-DD). toISOString 은 UTC 라 한국 시간 오전에 하루 밀리므로 로컬 값으로 만든다. */
function todayLocal() {
    const d = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const form = reactive({
    txType: props.initialType,
    quantity: '',
    txDate: todayLocal(),
    memo: '',
    error: '',
})

const titleText = computed(() => {
    const label = SUPPLY_TX_TYPES.find((t) => t.value === form.txType)?.label ?? ''
    return `${props.item.name} — ${label}`
})

/** 조정은 "세어본 실제 수량"이라 0 도 허용, 입고/사용은 1 이상이어야 한다. */
const minQuantity = computed(() => (form.txType === 'ADJUST' ? 0 : 1))

const hint = computed(() => {
    const unit = props.item.unit
    if (form.txType === 'ADJUST') {
        return `창고를 세어본 실제 수량을 적으세요. 현재 장부상 ${props.item.currentQuantity}${unit} 입니다.`
    }
    return form.txType === 'IN' ? `들여온 수량(${unit})` : `현장에서 쓴 수량(${unit})`
})

const saveMut = useMutation({
    mutationFn: (payload) => supplyService.addTransaction(props.item.id, payload),
    onSuccess: () => {
        notify.toast('등록되었습니다.', { type: 'success' })
        // 재고가 바뀌면 목록·단건·이력·위험경고가 한꺼번에 옛 값이 된다(supplyCache 참고).
        invalidateSupplyCaches(queryClient)
        emit('close')
    },
    onError: (e) => {
        form.error = e.response?.data?.message ?? '등록에 실패했습니다.'
    },
})

function submit() {
    form.error = ''
    // Number.isFinite 로 먼저 거른다. 빈 값·숫자가 아닌 값은 Number(...) 가 NaN 이 되는데,
    // NaN < min 은 false 라 그것만으로는 검증을 통과해 버린다.
    const qty = Number(form.quantity)
    if (!Number.isFinite(qty) || qty < minQuantity.value) {
        form.error = `수량을 ${minQuantity.value} 이상으로 입력하세요.`
        return
    }
    if (!form.txDate) {
        form.error = '일자를 입력하세요.'
        return
    }
    saveMut.mutate({
        txType: form.txType,
        quantity: Number(form.quantity),
        txDate: form.txDate,
        memo: form.memo.trim() || null,
    })
}
</script>

<template>
    <Modal :title="titleText" @close="emit('close')">
        <form class="tx-form" @submit.prevent="submit">
            <div class="field">
                <label>구분</label>
                <select v-model="form.txType">
                    <option v-for="t in SUPPLY_TX_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
                </select>
            </div>
            <div class="field">
                <label>수량 <span class="req">*</span></label>
                <input v-model="form.quantity" type="number" :min="minQuantity" step="1" placeholder="예: 2" />
                <p class="hint">{{ hint }}</p>
            </div>
            <div class="field">
                <label>일자 <span class="req">*</span></label>
                <input v-model="form.txDate" type="date" />
            </div>
            <div class="field">
                <label>메모</label>
                <input v-model="form.memo" maxlength="255" placeholder="예: 대성빌딩 계단청소" />
            </div>
            <p v-if="form.error" class="err-msg">{{ form.error }}</p>
        </form>
        <template #actions>
            <button class="btn btn--ghost" type="button" @click="emit('close')">취소</button>
            <button class="btn btn--primary" type="button" :disabled="saveMut.isPending.value" @click="submit">
                {{ saveMut.isPending.value ? '저장 중…' : '등록' }}
            </button>
        </template>
    </Modal>
</template>

<style scoped>
.tx-form { display: flex; flex-direction: column; gap: 0.9rem; }
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field label { font-size: 0.85rem; color: var(--text); font-weight: 600; }
.req { color: var(--danger); }
.field input, .field select { padding: 0.55rem 0.7rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; color: var(--text-h); background: #fff; }
.field input:focus, .field select:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
.hint { margin: 0; font-size: 0.78rem; color: var(--text); }
.err-msg { margin: 0; color: var(--danger); font-size: 0.82rem; }
.btn { padding: 0.5rem 0.9rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; }
.btn:hover { border-color: var(--primary); color: var(--primary); }
.btn--primary { background: var(--primary); border-color: var(--primary); color: var(--primary-fg); }
.btn--primary:hover { background: var(--primary-hover); color: var(--primary-fg); }
.btn--ghost { background: transparent; border-color: transparent; color: var(--text); }
</style>
