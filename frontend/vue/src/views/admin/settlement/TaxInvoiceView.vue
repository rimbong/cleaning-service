<script setup>
// 세금계산서 — 기간·기준(청구/수금) 선택 → 거래처별 집계 미리보기 + 엑셀 다운로드 + 발행 기록.
import { computed, ref } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

import { taxInvoiceService, TAX_BASIS } from '@/services/admin/settlement/taxInvoiceService'
import { useNotifyStore } from '@/stores/common/notify/notify'

const notify = useNotifyStore()
const queryClient = useQueryClient()

const now = new Date()
const params = ref({
    year: now.getFullYear(),
    fromMonth: now.getMonth() + 1 <= 6 ? 1 : 7,
    toMonth: now.getMonth() + 1 <= 6 ? 6 : 12,
    basis: 'BILLED',
})
const issueDate = ref('')

const aggKey = computed(() => ['tax-agg', params.value.year, params.value.fromMonth, params.value.toMonth, params.value.basis])
const { data: aggData, isFetching } = useQuery({
    queryKey: aggKey,
    queryFn: () => taxInvoiceService.aggregate(params.value.year, params.value.fromMonth, params.value.toMonth, params.value.basis).then((r) => r.data.data),
    staleTime: 15_000,
})
const agg = computed(() => aggData.value)

const { data: recordsData } = useQuery({
    queryKey: ['tax-records'],
    queryFn: () => taxInvoiceService.list().then((r) => r.data.data),
    staleTime: 15_000,
})
const records = computed(() => recordsData.value ?? [])

function money(v) { return v != null ? Number(v).toLocaleString('ko-KR') : '0' }

async function downloadExcel() {
    try {
        await taxInvoiceService.downloadExcel(params.value.year, params.value.fromMonth, params.value.toMonth, params.value.basis,
            `세금계산서집계_${params.value.year}_${params.value.fromMonth}-${params.value.toMonth}.xlsx`)
    } catch (e) { notify.bar(e.response?.data?.message ?? '엑셀 다운로드 실패', { color: 'red' }) }
}

const issueMut = useMutation({
    mutationFn: (row) => taxInvoiceService.issue({
        clientId: row.clientId,
        year: params.value.year, fromMonth: params.value.fromMonth, toMonth: params.value.toMonth,
        basis: params.value.basis,
        issueDate: issueDate.value || new Date().toISOString().slice(0, 10),
    }),
    onSuccess: () => { notify.toast('발행 기록 저장', { type: 'success' }); queryClient.invalidateQueries({ queryKey: ['tax-records'] }) },
    onError: (e) => notify.bar(e.response?.data?.message ?? '발행 실패', { color: 'red' }),
})

const removeMut = useMutation({
    mutationFn: (id) => taxInvoiceService.remove(id),
    onSuccess: () => { notify.toast('삭제됨', { type: 'info' }); queryClient.invalidateQueries({ queryKey: ['tax-records'] }) },
    onError: (e) => notify.bar(e.response?.data?.message ?? '삭제 실패', { color: 'red' }),
})

async function onDeleteRecord(t) {
    const label = t.clientName ?? '발행 기록'
    if (!(await notify.confirm(`'${label}' 발행 기록을 삭제하시겠습니까?`))) {
        return
    }
    removeMut.mutate(t.id)
}

async function downloadForm(t) {
    try {
        await taxInvoiceService.downloadForm(t.id, `세금계산서_${t.clientName ?? t.id}.xlsx`)
    } catch (e) { notify.bar(e.response?.data?.message ?? '세금계산서 양식 다운로드 실패', { color: 'red' }) }
}
</script>

<template>
    <section class="tax">
        <div class="toolbar">
            <div class="period">
                <input v-model.number="params.year" type="number" class="yr" /> 년
                <input v-model.number="params.fromMonth" type="number" min="1" max="12" class="mo" /> ~
                <input v-model.number="params.toMonth" type="number" min="1" max="12" class="mo" /> 월
                <select v-model="params.basis">
                    <option v-for="b in TAX_BASIS" :key="b.value" :value="b.value">{{ b.label }}</option>
                </select>
                <span v-if="isFetching" class="sync">집계 중…</span>
            </div>
            <button class="btn btn--primary" @click="downloadExcel">엑셀 다운로드</button>
        </div>

        <div v-if="agg" class="summary">
            <span>공급가액 합 <strong>{{ money(agg.totalSupply) }}</strong>원</span>
            <span>세액 합 <strong class="ok">{{ money(agg.totalTax) }}</strong>원</span>
            <span class="issue-date">발행일 <input v-model="issueDate" type="date" /></span>
        </div>

        <div class="table-wrap">
            <table v-if="agg && agg.rows.length" class="table">
                <thead>
                    <tr><th>사업자번호</th><th>상호</th><th>공급가액</th><th>세액</th><th>건수</th><th class="col-actions">발행</th></tr>
                </thead>
                <tbody>
                    <tr v-for="row in agg.rows" :key="row.clientId ?? 'none'">
                        <td>{{ row.businessNumber || '-' }}</td>
                        <td>{{ row.clientName }}</td>
                        <td>{{ money(row.supplyAmount) }}원</td>
                        <td>{{ money(row.taxAmount) }}원</td>
                        <td>{{ row.count }}</td>
                        <td class="col-actions">
                            <button v-if="row.clientId" class="btn btn--sm" @click="issueMut.mutate(row)">발행 기록</button>
                            <span v-else class="muted">미연결</span>
                        </td>
                    </tr>
                    <tr class="total-row">
                        <td colspan="2">합계</td>
                        <td>{{ money(agg.totalSupply) }}원</td>
                        <td>{{ money(agg.totalTax) }}원</td>
                        <td colspan="2"></td>
                    </tr>
                </tbody>
            </table>
            <p v-else class="state">이 기간 청구가 없습니다.</p>
        </div>

        <h3 class="rec-title">발행 기록</h3>
        <div class="table-wrap">
            <table v-if="records.length" class="table">
                <thead>
                    <tr><th>거래처</th><th>기간</th><th>공급가액</th><th>세액</th><th>기준</th><th>발행일</th><th class="col-actions">작업</th></tr>
                </thead>
                <tbody>
                    <tr v-for="t in records" :key="t.id">
                        <td>{{ t.clientName }}</td>
                        <td>{{ t.periodYear }}.{{ t.fromMonth }}~{{ t.toMonth }}</td>
                        <td>{{ money(t.supplyAmount) }}원</td>
                        <td>{{ money(t.taxAmount) }}원</td>
                        <td>{{ t.basis === 'PAID' ? '수금' : '청구' }}</td>
                        <td>{{ t.issueDate }}</td>
                        <td class="col-actions">
                            <button class="btn btn--sm" @click="downloadForm(t)">양식</button>
                            <button class="btn btn--sm btn--danger" @click="onDeleteRecord(t)">삭제</button>
                        </td>
                    </tr>
                </tbody>
            </table>
            <p v-else class="state">발행 기록이 없습니다.</p>
        </div>
    </section>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
.period { display: flex; align-items: center; gap: 0.4rem; }
.period input, .period select { padding: 0.4rem 0.5rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; }
.yr { width: 5rem; }
.mo { width: 3.5rem; }
.sync { color: var(--primary); font-size: 0.78rem; }
.summary { display: flex; align-items: center; gap: 1.5rem; padding: 0.75rem 1rem; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); margin-bottom: 1rem; flex-wrap: wrap; }
.summary .ok { color: var(--primary-hover); }
.summary .issue-date { margin-left: auto; display: flex; align-items: center; gap: 0.4rem; }
.summary input { padding: 0.35rem 0.5rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; }
.btn { padding: 0.45rem 0.8rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; }
.btn:hover { border-color: var(--primary); color: var(--primary); }
.btn--primary { background: var(--primary); border-color: var(--primary); color: var(--primary-fg); }
.btn--primary:hover { background: var(--primary-hover); color: var(--primary-fg); }
.btn--sm { padding: 0.3rem 0.6rem; font-size: 0.82rem; }
.btn--danger { color: var(--danger); }
.btn--danger:hover { border-color: var(--danger); background: var(--danger-soft); }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow: hidden; margin-bottom: 1.5rem; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 0.65rem 1rem; text-align: left; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
.table th { background: var(--muted); font-size: 0.82rem; color: var(--text); }
.total-row td { font-weight: 700; background: var(--muted); }
.col-actions { text-align: right; }
.muted { color: var(--text); font-size: 0.82rem; }
.rec-title { font-size: 1rem; margin: 0 0 0.75rem; }
.state { text-align: center; padding: 2rem 0; color: var(--text); margin: 0; }
</style>
