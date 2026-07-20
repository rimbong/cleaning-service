<script setup>
// 품목별 입출고 이력 — 재고가 이상할 때 어디서 틀어졌는지 훑어보는 화면.
// 수량은 부호 있는 증감이라 그대로 보여준다(+2 들어옴 / -1 씀).
import { computed, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/vue-query'

import { supplyService } from '@/services/admin/supply/supplyService'
import SupplyTxModal from './SupplyTxModal.vue'
import Pager from '@/common/components/common/Pager.vue'
import TableSkeleton from '@/common/components/common/TableSkeleton.vue'
import EmptyState from '@/common/components/common/EmptyState.vue'
import { usePageQuery } from '@/common/composables/usePageQuery'
import { useNotifyStore } from '@/stores/common/notify/notify'

const props = defineProps({ id: { type: [String, Number], required: true } })
const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

const { page } = usePageQuery()

// 품목 정보(이름·단위·현재 재고) — 이력 표 위에 요약으로 보여준다.
const { data: itemData } = useQuery({
    queryKey: ['supply', computed(() => String(props.id))],
    queryFn: () => supplyService.get(props.id).then((r) => r.data.data),
})
const item = computed(() => itemData.value ?? null)

const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['supply-history', computed(() => String(props.id)), page],
    queryFn: () => supplyService.history(props.id, { page: page.value }).then((r) => r.data.data),
    placeholderData: keepPreviousData,
})
const rows = computed(() => data.value?.content ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 0)

// 마지막 페이지를 비우면 유효 범위로 당긴다(빈 페이지 방지).
watch(totalPages, (tp) => {
    if (tp > 0 && page.value > tp) {
        page.value = tp
    }
})

function fmtDate(v) {
    return v ? String(v).slice(0, 10) : '-'
}

/** 부호를 눈에 보이게 붙인다(+2 / -1). 조정으로 차이가 0 이면 그대로 0. */
function signed(q) {
    return q > 0 ? `+${q}` : String(q)
}

// ===== 입출고 모달 =====
// 목록으로 돌아가지 않고 이 화면에서 바로 등록한다. 등록되면 SupplyTxModal 이 캐시를
// 무효화하므로 아래 이력 표와 상단 재고가 알아서 다시 불려온다.
const txModal = reactive({ open: false, txType: 'IN' })

function openTx(txType) {
    txModal.open = true
    txModal.txType = txType
}

const removeMut = useMutation({
    mutationFn: (transactionId) => supplyService.removeTransaction(props.id, transactionId),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['supply-history'] })
        queryClient.invalidateQueries({ queryKey: ['supply'] })
        queryClient.invalidateQueries({ queryKey: ['supplies'] })
    },
    onError: (e) => notify.bar(e.response?.data?.message ?? '삭제 실패', { color: 'red' }),
})

async function onDelete(row) {
    if (!(await notify.confirm(`${fmtDate(row.txDate)} ${row.txTypeLabel} ${signed(row.quantity)} 내역을 삭제하시겠습니까? 재고가 그만큼 되돌아갑니다.`))) {
        return
    }
    removeMut.mutate(row.id)
}
</script>

<template>
    <section class="history">
        <div class="toolbar">
            <div v-if="item" class="summary">
                <strong>{{ item.name }}</strong>
                <span v-if="item.spec" class="spec">{{ item.spec }}</span>
                <span class="qty" :class="{ 'qty--low': item.belowSafety }">
                    현재 재고 {{ item.currentQuantity }}{{ item.unit }}
                </span>
            </div>
            <div class="tools">
                <button class="btn btn--in" :disabled="!item" @click="openTx('IN')">입고</button>
                <button class="btn btn--out" :disabled="!item" @click="openTx('OUT')">사용</button>
                <button class="btn" @click="router.push({ name: 'admin-supplies' })">목록으로</button>
            </div>
        </div>

        <TableSkeleton v-if="isLoading" :rows="5" />
        <p v-else-if="isError" class="state state--err">불러오지 못했습니다.</p>
        <div v-else class="table-wrap">
            <div class="list-meta">
                <span>총 <strong>{{ totalElements }}</strong>건</span>
                <span v-if="isFetching" class="sync">갱신 중…</span>
            </div>
            <table v-if="rows.length" class="table">
                <thead>
                    <tr>
                        <th>일자</th>
                        <th>구분</th>
                        <th>증감</th>
                        <th>메모</th>
                        <th class="col-actions">작업</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="r in rows" :key="r.id">
                        <td>{{ fmtDate(r.txDate) }}</td>
                        <td><span class="tag" :class="'tag--' + r.txType.toLowerCase()">{{ r.txTypeLabel }}</span></td>
                        <td :class="r.quantity < 0 ? 'minus' : 'plus'">{{ signed(r.quantity) }}{{ item ? item.unit : '' }}</td>
                        <td>{{ r.memo || '-' }}</td>
                        <td class="col-actions">
                            <button class="btn btn--sm btn--danger" @click="onDelete(r)">삭제</button>
                        </td>
                    </tr>
                </tbody>
            </table>
            <EmptyState v-else icon="📋" message="입출고 이력이 없습니다." />

            <Pager v-model:page="page" :total-pages="totalPages" :total-elements="totalElements" />
        </div>

        <SupplyTxModal
            v-if="txModal.open && item"
            :item="item"
            :initial-type="txModal.txType"
            @close="txModal.open = false"
        />
    </section>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 1.25rem; flex-wrap: wrap; }
.summary { display: flex; align-items: baseline; gap: 0.6rem; }
.summary strong { font-size: 1.05rem; color: var(--text-h); }
.spec { font-size: 0.85rem; color: var(--text); }
.qty { font-size: 0.9rem; font-weight: 600; color: var(--primary); }
.qty--low { color: var(--danger); }
.tools { display: flex; gap: 0.5rem; }
.btn { padding: 0.5rem 0.9rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; text-decoration: none; display: inline-flex; align-items: center; }
.btn:hover { border-color: var(--primary); color: var(--primary); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--in { color: var(--primary); border-color: var(--primary); }
.btn--out { color: #b45309; border-color: #fcd34d; }
.btn--sm { padding: 0.3rem 0.6rem; font-size: 0.82rem; }
.btn--danger { color: var(--danger); }
.btn--danger:hover { border-color: var(--danger); background: var(--danger-soft); }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow: hidden; }
.list-meta { display: flex; align-items: center; gap: 0.75rem; padding: 0.75rem 1rem; border-bottom: 1px solid var(--border); font-size: 0.85rem; color: var(--text); }
.sync { color: var(--primary); font-size: 0.78rem; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 0.7rem 1rem; text-align: left; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
.table th { background: var(--muted); font-size: 0.82rem; color: var(--text); }
.table tbody tr:last-child td { border-bottom: none; }
.table tbody tr:hover { background: var(--muted); }
.col-actions { text-align: right; white-space: nowrap; }
.tag { display: inline-block; padding: 0.15rem 0.55rem; border-radius: 999px; font-size: 0.78rem; font-weight: 600; }
.tag--in { background: var(--primary-soft); color: var(--primary-hover); }
.tag--out { background: #fef3c7; color: #b45309; }
.tag--adjust { background: #e5e7eb; color: #4b5563; }
.plus { color: var(--primary); font-weight: 600; }
.minus { color: #b45309; font-weight: 600; }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
.state--err { color: var(--danger); }
</style>
