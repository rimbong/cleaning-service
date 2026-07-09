<script setup>
// 정산(월 청구/수금 관리) — 월 선택 → 청구 생성 → 계약별 청구 목록에 입금 기록(분할 가능)·편집·삭제.
import { computed, ref } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

import { settlementService } from '@/services/settlement/settlementService'
import Modal from '@/common/components/common/Modal.vue'
import { useNotifyStore } from '@/common/stores/notify/notify'

const notify = useNotifyStore()
const queryClient = useQueryClient()

const now = new Date()
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const statusFilter = ref('') // '' | UNPAID | PARTIAL | PAID

const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['settlement', computed(() => year.value), computed(() => month.value)],
    queryFn: () => settlementService.getMonth(year.value, month.value).then((res) => res.data.data),
    staleTime: 15_000,
})

const monthData = computed(() => data.value)
const items = computed(() => {
    const list = monthData.value?.items ?? []
    return statusFilter.value ? list.filter((b) => b.status === statusFilter.value) : list
})

function shiftMonth(delta) {
    let m = month.value + delta
    let y = year.value
    if (m < 1) { m = 12; y -= 1 }
    if (m > 12) { m = 1; y += 1 }
    year.value = y
    month.value = m
}

function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['settlement'] })
}

const generateMut = useMutation({
    mutationFn: () => settlementService.generate(year.value, month.value),
    onSuccess: (res) => {
        notify.toast(`${res.data.data}건 생성`, { type: 'success' })
        invalidate()
    },
    onError: (e) => notify.bar(e.response?.data?.message ?? '생성 실패', { color: 'red' }),
})

const removeMut = useMutation({
    mutationFn: (id) => settlementService.deleteBilling(id),
    onSuccess: () => { notify.toast('삭제되었습니다.', { type: 'info' }); invalidate() },
    onError: (e) => notify.bar(e.response?.data?.message ?? '삭제 실패', { color: 'red' }),
})

async function onDeleteBilling(b) {
    if (!(await notify.confirm(`'${b.targetName ?? b.title}' 청구를 삭제하시겠습니까?`))) return
    removeMut.mutate(b.id)
}

// ── 입금 모달 ──
const payModal = ref(null)   // 선택된 billing
const payments = ref([])
const payForm = ref({ amount: '', paidDate: '', method: '', memo: '' })

async function openPayments(b) {
    payModal.value = b
    payForm.value = { amount: String(Math.max(0, (b.amount ?? 0) - (b.paidAmount ?? 0))), paidDate: '', method: '', memo: '' }
    await reloadPayments(b.id)
}
async function reloadPayments(billingId) {
    payments.value = (await settlementService.listPayments(billingId)).data.data
}
function closePayments() { payModal.value = null; payments.value = [] }

async function addPayment() {
    const f = payForm.value
    if (f.amount === '' || Number(f.amount) <= 0) { notify.bar('입금액을 입력하세요.', { color: 'yellow' }); return }
    if (!f.paidDate) { notify.bar('입금일을 입력하세요.', { color: 'yellow' }); return }
    try {
        await settlementService.addPayment(payModal.value.id, {
            amount: Number(f.amount), paidDate: f.paidDate, method: f.method.trim() || null, memo: f.memo.trim() || null,
        })
        notify.toast('입금 기록됨', { type: 'success' })
        await reloadPayments(payModal.value.id)
        invalidate()
        payForm.value = { amount: '', paidDate: '', method: f.method, memo: '' }
    } catch (e) { notify.bar(e.response?.data?.message ?? '입금 실패', { color: 'red' }) }
}
async function removePayment(p) {
    if (!(await notify.confirm('이 입금 기록을 삭제하시겠습니까?'))) return
    try {
        await settlementService.deletePayment(p.id)
        await reloadPayments(payModal.value.id)
        invalidate()
    } catch (e) { notify.bar('삭제 실패', { color: 'red' }) }
}

// ── 청구 편집 모달 ──
const editModal = ref(null)
const editForm = ref({ amount: '', memo: '' })
function openEdit(b) { editModal.value = b; editForm.value = { amount: String(b.amount ?? ''), memo: b.memo ?? '' } }
function closeEdit() { editModal.value = null }
async function saveEdit() {
    if (editForm.value.amount === '' || Number(editForm.value.amount) < 0) { notify.bar('청구액을 올바르게.', { color: 'yellow' }); return }
    try {
        await settlementService.editBilling(editModal.value.id, { amount: Number(editForm.value.amount), memo: editForm.value.memo.trim() || null })
        notify.toast('수정됨', { type: 'success' })
        closeEdit(); invalidate()
    } catch (e) { notify.bar(e.response?.data?.message ?? '수정 실패', { color: 'red' }) }
}

function money(v) { return v != null ? Number(v).toLocaleString('ko-KR') : '0' }
function fmtDate(v) { return v ? String(v).slice(0, 10) : '-' }
</script>

<template>
    <section class="settlement">
        <div class="toolbar">
            <div class="month-nav">
                <button class="btn" @click="shiftMonth(-1)">◀</button>
                <strong>{{ year }}년 {{ month }}월</strong>
                <button class="btn" @click="shiftMonth(1)">▶</button>
                <span v-if="isFetching" class="sync">갱신 중…</span>
            </div>
            <button class="btn btn--primary" :disabled="generateMut.isPending?.value" @click="generateMut.mutate()">
                이번 달 청구 생성
            </button>
        </div>

        <div v-if="monthData" class="summary">
            <span>청구 <strong>{{ money(monthData.totalBilled) }}</strong>원</span>
            <span>수금 <strong class="ok">{{ money(monthData.totalPaid) }}</strong>원</span>
            <span>미수 <strong class="bad">{{ money(monthData.totalUnpaid) }}</strong>원</span>
            <span class="count">{{ monthData.count }}건</span>
            <span class="filter">
                <select v-model="statusFilter">
                    <option value="">전체</option>
                    <option value="UNPAID">미수</option>
                    <option value="PARTIAL">부분수금</option>
                    <option value="PAID">완납</option>
                </select>
            </span>
        </div>

        <p v-if="isLoading" class="state">불러오는 중…</p>
        <p v-else-if="isError" class="state state--err">불러오지 못했습니다.</p>
        <div v-else class="table-wrap">
            <table v-if="items.length" class="table">
                <thead>
                    <tr>
                        <th>대상</th><th>서비스</th><th>청구액</th><th>수금액</th><th>상태</th><th class="col-actions">작업</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="b in items" :key="b.id">
                        <td>{{ b.targetName || '-' }}<span v-if="b.sourceType==='QUOTE'" class="badge">견적</span></td>
                        <td>{{ b.title || '-' }}</td>
                        <td>{{ money(b.amount) }}원</td>
                        <td>{{ money(b.paidAmount) }}원</td>
                        <td><span class="tag" :class="'tag--' + b.status.toLowerCase()">{{ b.statusLabel }}</span></td>
                        <td class="col-actions">
                            <button class="btn btn--sm btn--primary" @click="openPayments(b)">입금</button>
                            <button class="btn btn--sm" @click="openEdit(b)">편집</button>
                            <button class="btn btn--sm btn--danger" @click="onDeleteBilling(b)">삭제</button>
                        </td>
                    </tr>
                </tbody>
            </table>
            <div v-else class="empty">
                <p>이 달 청구가 없습니다.</p>
                <button class="btn btn--primary" @click="generateMut.mutate()">이번 달 청구 생성</button>
            </div>
        </div>

        <!-- 입금 모달 (배경 클릭으로 닫히지 않음) -->
        <Modal
            v-if="payModal"
            :title="`입금 관리 — ${payModal.targetName} (${money(payModal.amount)}원)`"
            :close-on-backdrop="false"
            @close="closePayments"
        >
            <ul class="pay-list">
                <li v-for="p in payments" :key="p.id">
                    <span>{{ money(p.amount) }}원 · {{ fmtDate(p.paidDate) }} · {{ p.method || '-' }}</span>
                    <button class="btn btn--sm btn--danger" @click="removePayment(p)">삭제</button>
                </li>
                <li v-if="!payments.length" class="muted">입금 기록 없음</li>
            </ul>
            <div class="pay-form">
                <input v-model="payForm.amount" type="number" min="0" placeholder="입금액" />
                <input v-model="payForm.paidDate" type="date" />
                <input v-model="payForm.method" placeholder="방법(현금·신한)" maxlength="30" />
                <button class="btn btn--primary" @click="addPayment">입금 추가</button>
            </div>
            <template #actions>
                <button class="btn" @click="closePayments">닫기</button>
            </template>
        </Modal>

        <!-- 청구 편집 모달 (배경 클릭으로 닫히지 않음) -->
        <Modal
            v-if="editModal"
            :title="`청구 편집 — ${editModal.targetName}`"
            :close-on-backdrop="false"
            @close="closeEdit"
        >
            <div class="field"><label>청구액</label><input v-model="editForm.amount" type="number" min="0" /></div>
            <div class="field"><label>메모</label><input v-model="editForm.memo" maxlength="255" /></div>
            <template #actions>
                <button class="btn" @click="closeEdit">취소</button>
                <button class="btn btn--primary" @click="saveEdit">저장</button>
            </template>
        </Modal>
    </section>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
.month-nav { display: flex; align-items: center; gap: 0.6rem; font-size: 1.05rem; }
.sync { color: var(--primary); font-size: 0.78rem; }
.summary { display: flex; align-items: center; gap: 1.25rem; padding: 0.75rem 1rem; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); margin-bottom: 1rem; flex-wrap: wrap; font-size: 0.92rem; }
.summary .ok { color: var(--primary-hover); }
.summary .bad { color: var(--danger); }
.summary .count { color: var(--text); }
.summary .filter { margin-left: auto; }
.summary select { padding: 0.35rem 0.5rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; }
.btn { padding: 0.45rem 0.8rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; }
.btn:hover { border-color: var(--primary); color: var(--primary); }
.btn--primary { background: var(--primary); border-color: var(--primary); color: var(--primary-fg); }
.btn--primary:hover { background: var(--primary-hover); color: var(--primary-fg); }
.btn--sm { padding: 0.3rem 0.6rem; font-size: 0.82rem; margin-left: 0.3rem; }
.btn--danger { color: var(--danger); }
.btn--danger:hover { border-color: var(--danger); background: var(--danger-soft); }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow: hidden; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 0.7rem 1rem; text-align: left; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
.table th { background: var(--muted); font-size: 0.82rem; color: var(--text); }
.table tbody tr:last-child td { border-bottom: none; }
.col-actions { text-align: right; white-space: nowrap; }
.badge { margin-left: 0.4rem; font-size: 0.7rem; background: #fef3c7; color: #92400e; padding: 0.05rem 0.35rem; border-radius: 999px; }
.tag { display: inline-block; padding: 0.15rem 0.55rem; border-radius: 999px; font-size: 0.78rem; font-weight: 600; }
.tag--unpaid { background: #fee2e2; color: #991b1b; }
.tag--partial { background: #fef3c7; color: #92400e; }
.tag--paid { background: var(--primary-soft); color: var(--primary-hover); }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
.state--err { color: var(--danger); }
.empty { padding: 3rem 1rem; text-align: center; color: var(--text); }
.empty p { margin-bottom: 1rem; }
/* 아래 pay-list/pay-form/field 는 Modal 슬롯 안에서 쓰인다(슬롯 내용은 부모 스코프라 적용됨) */
.pay-list { list-style: none; margin: 0 0 1rem; padding: 0; }
.pay-list li { display: flex; justify-content: space-between; align-items: center; padding: 0.4rem 0; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
.pay-list .muted { color: var(--text); justify-content: center; }
.pay-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem; margin-bottom: 1rem; }
.pay-form input { padding: 0.5rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; }
.pay-form .btn--primary { grid-column: 1 / -1; }
.field { display: flex; flex-direction: column; gap: 0.35rem; margin-bottom: 0.75rem; }
.field label { font-size: 0.85rem; font-weight: 600; color: var(--text); }
.field input { padding: 0.5rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; }
</style>
