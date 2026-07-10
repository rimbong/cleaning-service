<script setup>
// 견적 목록 — vue-query 캐싱, 서비스내용/고객명 검색, 페이징, 등록/상세/수정/삭제 진입. 행 클릭 시 상세.
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/vue-query'

import { quoteService } from '@/services/admin/quote/quoteService'
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
// 페이지는 URL 쿼리(?page=N)와 동기화 — 상세 왕복/뒤로가기 시 보던 페이지 유지.
const { page } = usePageQuery()

// 검색어가 바뀌면 1페이지로 되돌린다.
watch(appliedKeyword, () => {
    page.value = 1
})

const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['quotes', appliedKeyword, page],
    queryFn: () => quoteService
        .list({ keyword: appliedKeyword.value, page: page.value })
        .then((res) => res.data.data),
    staleTime: 30_000,
    placeholderData: keepPreviousData,
})

const quotes = computed(() => data.value?.content ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 0)

// 마지막 페이지에서 항목을 모두 지워 페이지 수가 줄면 현재 페이지를 유효 범위로 당긴다(빈 페이지 방지).
watch(totalPages, (tp) => {
    if (tp > 0 && page.value > tp) {
        page.value = tp
    }
})

function doSearch() {
    appliedKeyword.value = searchInput.value.trim()
}

function resetSearch() {
    searchInput.value = ''
    appliedKeyword.value = ''
}

const removeMutation = useMutation({
    mutationFn: (id) => quoteService.remove(id),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['quotes'] })
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    },
})

async function onDelete(quote) {
    if (!(await notify.confirm(`'${quote.title}' 견적을 삭제하시겠습니까?`))) {
        return
    }
    removeMutation.mutate(quote.id)
}

function customerOf(q) {
    return q.clientName || q.customerName || '-'
}

function fmtDate(v) {
    return v ? String(v).slice(0, 10) : '-'
}

function fmtMoney(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') + '원' : '-'
}

function goDetail(id) {
    router.push({ name: 'admin-quote-detail', params: { id } })
}
</script>

<template>
    <section class="quotes">
        <div class="toolbar">
            <form class="search" @submit.prevent="doSearch">
                <input v-model="searchInput" placeholder="서비스 내용·고객명 검색" />
                <button class="btn" type="submit">검색</button>
                <button v-if="appliedKeyword" class="btn btn--ghost" type="button" @click="resetSearch">초기화</button>
            </form>
            <button class="btn btn--primary" type="button" @click="router.push({ name: 'admin-quote-new' })">
                + 새 견적
            </button>
        </div>

        <TableSkeleton v-if="isLoading" :rows="5" />
        <p v-else-if="isError" class="state state--err">목록을 불러오지 못했습니다.</p>

        <div v-else class="table-wrap">
            <div class="list-meta">
                <span>총 <strong>{{ totalElements }}</strong>건</span>
                <span v-if="isFetching" class="list-meta__sync">갱신 중…</span>
            </div>

            <table v-if="quotes.length" class="table">
                <thead>
                    <tr>
                        <th>서비스 내용</th>
                        <th>고객</th>
                        <th>견적 금액</th>
                        <th>견적일</th>
                        <th>상태</th>
                        <th class="col-actions">작업</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="q in quotes" :key="q.id" class="row-click" @click="goDetail(q.id)">
                        <td><span class="name-link">{{ q.title }}</span></td>
                        <td>{{ customerOf(q) }}</td>
                        <td>{{ fmtMoney(q.amount) }}</td>
                        <td>{{ fmtDate(q.quoteDate) }}</td>
                        <td>
                            <span v-if="q.status" class="tag" :class="'tag--' + q.status.toLowerCase()">
                                {{ q.statusLabel }}
                            </span>
                            <span v-else class="muted">-</span>
                        </td>
                        <td class="col-actions" @click.stop>
                            <RouterLink class="btn btn--sm" :to="{ name: 'admin-quote-edit', params: { id: q.id } }">
                                수정
                            </RouterLink>
                            <button class="btn btn--sm btn--danger" type="button" @click="onDelete(q)">삭제</button>
                        </td>
                    </tr>
                </tbody>
            </table>

            <EmptyState v-else icon="🧾" message="등록된 견적이 없습니다.">
                <button class="btn btn--primary" type="button" @click="router.push({ name: 'admin-quote-new' })">
                    첫 견적 등록하기
                </button>
            </EmptyState>

            <Pager v-model:page="page" :total-pages="totalPages" :total-elements="totalElements" />
        </div>
    </section>
</template>

<style scoped>
.toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    margin-bottom: 1.25rem;
    flex-wrap: wrap;
}

.search {
    display: flex;
    gap: 0.5rem;
}

.search input {
    padding: 0.5rem 0.7rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    font: inherit;
    min-width: 220px;
}

.search input:focus {
    outline: none;
    border-color: var(--primary);
    box-shadow: 0 0 0 3px var(--primary-soft);
}

/* 버튼은 전역 .btn 계열(style.css) 사용 */

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}

.state--err {
    color: var(--danger);
}

.table-wrap {
    background: #fff;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    overflow: hidden;
}

.list-meta {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem 1rem;
    border-bottom: 1px solid var(--border);
    font-size: 0.85rem;
    color: var(--text);
}

.list-meta__sync {
    color: var(--primary);
    font-size: 0.78rem;
}

.table {
    width: 100%;
    border-collapse: collapse;
}

.table th,
.table td {
    padding: 0.75rem 1rem;
    text-align: left;
    border-bottom: 1px solid var(--border);
    font-size: 0.9rem;
}

.table th {
    background: var(--muted);
    color: var(--text);
    font-weight: 600;
    font-size: 0.82rem;
}

.table tbody tr:last-child td {
    border-bottom: none;
}

.table tbody tr:hover {
    background: var(--muted);
}

.row-click {
    cursor: pointer;
}

.col-actions {
    text-align: right;
    white-space: nowrap;
}

.col-actions .btn {
    margin-left: 0.35rem;
}

.name-link {
    color: var(--text-h);
    font-weight: 600;
    text-decoration: none;
}

.name-link:hover {
    color: var(--primary);
}

.tag {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 999px;
    font-size: 0.78rem;
    font-weight: 600;
}

.tag--pending {
    background: #fef3c7;
    color: #92400e;
}

.tag--accepted {
    background: var(--primary-soft);
    color: var(--primary-hover);
}

.tag--rejected {
    background: #e5e7eb;
    color: #4b5563;
}

.muted {
    color: var(--text);
}

.empty {
    padding: 3rem 1rem;
    text-align: center;
    color: var(--text);
}

.empty p {
    margin-bottom: 1rem;
}
</style>
