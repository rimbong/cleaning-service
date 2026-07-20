<script setup>
// 약품/소모품 재고 현황 — 품목별 현재 재고 + 그 자리에서 입고/사용 등록.
// 재고 숫자는 서버가 입출고 이력 합계로 계산해 준 값이라 화면에서 직접 고치지 않는다.
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/vue-query'

import { supplyService, PH_TYPES, DANGEROUS_PAIRS } from '@/services/admin/supply/supplyService'
import SupplyTxModal from './SupplyTxModal.vue'
import Pager from '@/common/components/common/Pager.vue'
import TableSkeleton from '@/common/components/common/TableSkeleton.vue'
import EmptyState from '@/common/components/common/EmptyState.vue'
import { usePageQuery } from '@/common/composables/usePageQuery'
import { useNotifyStore } from '@/stores/common/notify/notify'

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

const searchInput = ref('')
const appliedKeyword = ref('')
// 페이지는 URL 쿼리(?page=N)와 동기화 — 목록 왕복/뒤로가기 시 보던 페이지 유지.
const { page } = usePageQuery()

// 검색어가 바뀌면 1페이지로 되돌린다.
watch(appliedKeyword, () => {
    page.value = 1
})

const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['supplies', appliedKeyword, page],
    queryFn: () => supplyService
        .list({ keyword: appliedKeyword.value, page: page.value })
        .then((r) => r.data.data),
    placeholderData: keepPreviousData,
})
const items = computed(() => data.value?.content ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 0)

// 마지막 페이지에서 항목을 모두 지워 페이지 수가 줄면 현재 페이지를 유효 범위로 당긴다(빈 페이지 방지).
watch(totalPages, (tp) => {
    if (tp > 0 && page.value > tp) {
        page.value = tp
    }
})

// 안전재고 미만(또는 음수) 품목 수 — 상단에 요약해 "지금 사야 할 게 있는지"를 한눈에 보여준다.
const shortageCount = computed(() => items.value.filter((i) => i.belowSafety).length)

/**
 * 보유 약품 중 섞으면 위험한 조합이 있는지 찾는다.
 * 청소 사고는 대부분 "더 강하게" 하려고 섞다가 나므로, 창고에 둘 다 있으면 미리 알린다.
 * 재고가 남아 있는 품목만 본다(0 이면 지금 창고에 없다는 뜻).
 */
const dangerWarnings = computed(() => {
    const present = new Set(
        items.value.filter((i) => i.phType && i.currentQuantity > 0).map((i) => i.phType),
    )
    return DANGEROUS_PAIRS
        .filter((pair) => present.has(pair.a) && present.has(pair.b))
        .map((pair) => ({
            ...pair,
            aLabel: labelOfPh(pair.a),
            bLabel: labelOfPh(pair.b),
        }))
})

function labelOfPh(value) {
    return PH_TYPES.find((p) => p.value === value)?.label ?? value
}

function doSearch() {
    appliedKeyword.value = searchInput.value.trim()
}

function resetSearch() {
    searchInput.value = ''
    appliedKeyword.value = ''
}

function money(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') : '-'
}

// ===== 입출고 모달 =====
// 폼·검증·캐시 무효화는 SupplyTxModal 이 전부 맡는다. 여기서는 열고 닫기만 관리한다.
const txModal = reactive({
    item: null,
    txType: 'IN',
})

function openTx(item, txType) {
    txModal.item = item
    txModal.txType = txType
}

function closeTx() {
    txModal.item = null
}

// ===== 품목 삭제 =====
const removeMut = useMutation({
    mutationFn: (id) => supplyService.remove(id),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        // 품목이 사라지면 목록뿐 아니라 그 품목의 단건·이력 캐시도 의미가 없어진다.
        queryClient.invalidateQueries({ queryKey: ['supplies'] })
        queryClient.invalidateQueries({ queryKey: ['supply'] })
        queryClient.invalidateQueries({ queryKey: ['supply-history'] })
    },
    onError: (e) => notify.bar(e.response?.data?.message ?? '삭제 실패', { color: 'red' }),
})

async function onDelete(item) {
    if (!(await notify.confirm(`'${item.name}' 품목을 삭제하시겠습니까?`))) {
        return
    }
    removeMut.mutate(item.id)
}

const downloading = ref(false)
async function onDownload() {
    downloading.value = true
    try {
        await supplyService.downloadExcel(appliedKeyword.value, '약품재고현황.xlsx')
    } catch (e) {
        notify.bar(e.response?.data?.message ?? '엑셀 다운로드에 실패했습니다.', { color: 'red' })
    } finally {
        downloading.value = false
    }
}
</script>

<template>
    <section class="supplies">
        <div class="toolbar">
            <form class="search" @submit.prevent="doSearch">
                <input v-model="searchInput" placeholder="품목명 검색" />
                <button class="btn" type="submit">검색</button>
                <button v-if="appliedKeyword" class="btn btn--ghost" type="button" @click="resetSearch">초기화</button>
            </form>
            <div class="tools">
                <RouterLink class="btn" :to="{ name: 'admin-supply-guide' }">약품 가이드</RouterLink>
                <button class="btn" :disabled="downloading" @click="onDownload">
                    {{ downloading ? '내보내는 중…' : '엑셀 다운로드' }}
                </button>
                <button class="btn btn--primary" @click="router.push({ name: 'admin-supply-new' })">+ 새 품목</button>
            </div>
        </div>

        <!-- 섞으면 유독가스가 나오는 조합을 둘 다 보유 중일 때. 안전 문제라 목록 맨 위에 둔다. -->
        <div v-for="w in dangerWarnings" :key="w.gas" class="danger">
            <strong>같이 두면 위험: {{ w.aLabel }} + {{ w.bLabel }} → {{ w.gas }}</strong>
            <p>{{ w.detail }}</p>
            <p class="danger__rule">세제는 한 번에 한 종류만. 다른 세제로 바꿀 땐 물로 완전히 헹군 뒤 사용하세요.</p>
        </div>

        <TableSkeleton v-if="isLoading" :rows="5" />
        <p v-else-if="isError" class="state state--err">불러오지 못했습니다.</p>
        <div v-else class="table-wrap">
            <div class="list-meta">
                <span>총 <strong>{{ totalElements }}</strong>개 품목</span>
                <span v-if="shortageCount" class="warn">이 페이지에 부족한 품목 <strong>{{ shortageCount }}</strong>개</span>
                <span v-if="isFetching" class="sync">갱신 중…</span>
            </div>
            <table v-if="items.length" class="table">
                <thead>
                    <tr>
                        <th>품목</th>
                        <th>규격</th>
                        <th>구분</th>
                        <th>현재 재고</th>
                        <th>안전재고</th>
                        <th>단가</th>
                        <th class="col-actions">작업</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="i in items" :key="i.id">
                        <td>{{ i.name }}</td>
                        <td>{{ i.spec || '-' }}</td>
                        <td>
                            <span
                                v-if="i.phType"
                                class="ph"
                                :class="'ph--' + i.phType.toLowerCase()"
                                :title="i.phMixWarning"
                            >{{ i.phTypeLabel }}</span>
                            <span v-else class="ph ph--none">미분류</span>
                        </td>
                        <td>
                            <span class="qty" :class="{ 'qty--low': i.belowSafety }">
                                {{ i.currentQuantity }}{{ i.unit }}
                            </span>
                            <span v-if="i.belowSafety" class="badge">부족</span>
                        </td>
                        <td>{{ i.safetyQty }}{{ i.unit }}</td>
                        <td>{{ money(i.unitPrice) }}{{ i.unitPrice != null ? '원' : '' }}</td>
                        <td class="col-actions">
                            <button class="btn btn--sm btn--in" @click="openTx(i, 'IN')">입고</button>
                            <button class="btn btn--sm btn--out" @click="openTx(i, 'OUT')">사용</button>
                            <RouterLink class="btn btn--sm" :to="{ name: 'admin-supply-history', params: { id: i.id } }">이력</RouterLink>
                            <RouterLink class="btn btn--sm" :to="{ name: 'admin-supply-edit', params: { id: i.id } }">수정</RouterLink>
                            <button class="btn btn--sm btn--danger" @click="onDelete(i)">삭제</button>
                        </td>
                    </tr>
                </tbody>
            </table>
            <EmptyState v-else icon="🧴" message="등록된 품목이 없습니다.">
                <button class="btn btn--primary" @click="router.push({ name: 'admin-supply-new' })">첫 품목 등록</button>
            </EmptyState>

            <Pager v-model:page="page" :total-pages="totalPages" :total-elements="totalElements" />
        </div>

        <SupplyTxModal
            v-if="txModal.item"
            :item="txModal.item"
            :initial-type="txModal.txType"
            @close="closeTx"
        />
    </section>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 1.25rem; flex-wrap: wrap; }
.tools { display: flex; gap: 0.5rem; }
.search { display: flex; gap: 0.5rem; }
.search input { padding: 0.5rem 0.7rem; border: 1px solid var(--border); border-radius: var(--radius); font: inherit; min-width: 220px; }
.btn { padding: 0.5rem 0.9rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; text-decoration: none; display: inline-flex; align-items: center; }
.btn:hover { border-color: var(--primary); color: var(--primary); }
.btn--primary { background: var(--primary); border-color: var(--primary); color: var(--primary-fg); }
.btn--primary:hover { background: var(--primary-hover); color: var(--primary-fg); }
.btn--ghost { background: transparent; border-color: transparent; color: var(--text); }
.btn--sm { padding: 0.3rem 0.6rem; font-size: 0.82rem; margin-left: 0.3rem; }
.btn--in { color: var(--primary); border-color: var(--primary); }
.btn--out { color: #b45309; border-color: #fcd34d; }
.btn--danger { color: var(--danger); }
.btn--danger:hover { border-color: var(--danger); background: var(--danger-soft); }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow: hidden; }
.list-meta { display: flex; align-items: center; gap: 0.75rem; padding: 0.75rem 1rem; border-bottom: 1px solid var(--border); font-size: 0.85rem; color: var(--text); }
.warn { color: var(--danger); }
.sync { color: var(--primary); font-size: 0.78rem; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 0.7rem 1rem; text-align: left; border-bottom: 1px solid var(--border); font-size: 0.9rem; }
.table th { background: var(--muted); font-size: 0.82rem; color: var(--text); }
.table tbody tr:last-child td { border-bottom: none; }
.table tbody tr:hover { background: var(--muted); }
.col-actions { text-align: right; white-space: nowrap; }
.qty { font-weight: 600; }
.qty--low { color: var(--danger); }
.badge { display: inline-block; margin-left: 0.4rem; padding: 0.1rem 0.45rem; border-radius: 999px; background: var(--danger-soft); color: var(--danger); font-size: 0.72rem; font-weight: 600; }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
.state--err { color: var(--danger); }
.danger { background: #fdeaea; border: 1px solid #f3c4c4; border-radius: var(--radius); padding: 0.85rem 1rem; margin-bottom: 1rem; }
.danger strong { display: block; color: #8f1d1d; font-size: 0.9rem; margin-bottom: 0.3rem; }
.danger p { margin: 0; font-size: 0.82rem; color: #7a2323; line-height: 1.55; }
.danger__rule { margin-top: 0.35rem !important; font-weight: 600; }
.ph { display: inline-block; padding: 0.13rem 0.5rem; border-radius: 999px; font-size: 0.75rem; font-weight: 600; white-space: nowrap; }
.ph--acid { background: #fdf1ea; color: #d15a26; }
.ph--neutral { background: #eef7f1; color: #2f7d51; }
.ph--alkali { background: #eaf1fd; color: #2b62c4; }
.ph--oxidizer { background: #f2ecfd; color: #6d43d1; }
.ph--enzyme { background: #e7f6f4; color: #0b6f68; }
.ph--etc, .ph--none { background: #e5e7eb; color: #6b7280; }
/* 입출고 모달의 폼 스타일은 SupplyTxModal 안에 있다. */
</style>
