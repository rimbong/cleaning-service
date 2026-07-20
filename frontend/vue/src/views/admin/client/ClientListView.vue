<script setup>
// 거래처 목록 — vue-query 로 캐싱, 건물명 검색, 페이징, 등록/상세/수정/삭제 진입.
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/vue-query'

import { clientService } from '@/services/admin/client/clientService'
import Pager from '@/common/components/common/Pager.vue'
import TableSkeleton from '@/common/components/common/TableSkeleton.vue'
import EmptyState from '@/common/components/common/EmptyState.vue'
import { usePageQuery } from '@/common/composables/usePageQuery'
import { useNotifyStore } from '@/stores/common/notify/notify'
import { invalidatePricingReview } from '@/services/admin/pricing/pricingCache'

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

// 입력 중인 검색어(input)와 실제 조회에 쓰는 검색어(applied)를 분리 —
// 타이핑마다 요청하지 않고 "검색" 실행 시점에만 반영한다.
const searchInput = ref('')
const appliedKeyword = ref('')
// 페이지는 URL 쿼리(?page=N)와 동기화 — 상세 왕복/뒤로가기 시 보던 페이지 유지.
const { page } = usePageQuery()

// 검색어가 바뀌면 1페이지로 되돌린다.
watch(appliedKeyword, () => {
    page.value = 1
})

// queryKey 에 검색어·페이지를 넣어, 조건별로 캐시가 분리·재사용된다.
// placeholderData: keepPreviousData — 페이지 이동 시 이전 데이터를 유지해 깜빡임을 줄인다.
const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['clients', appliedKeyword, page],
    queryFn: () => clientService
        .list({ keyword: appliedKeyword.value, page: page.value })
        .then((res) => res.data.data),
    staleTime: 30_000, // 30초 이내 재방문은 캐시 사용(목록 왕복 시 즉시 표시)
    placeholderData: keepPreviousData,
})

const clients = computed(() => data.value?.content ?? [])
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

// 삭제 — 성공 시 목록 캐시 무효화(자동 갱신)
const removeMutation = useMutation({
    mutationFn: (id) => clientService.remove(id),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['clients'] })
        // 거래처가 사라지면 그 계약도 재산정에서 빠진다.
        invalidatePricingReview(queryClient)
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    },
})

async function onDelete(client) {
    if (!(await notify.confirm(`'${client.name}' 거래처를 삭제하시겠습니까?`))) {
        return
    }
    removeMutation.mutate(client.id)
}

function fmtDate(v) {
    return v ? String(v).slice(0, 10) : '-'
}

// 목록 행 클릭 → 상세로 이동(수정/삭제 버튼은 @click.stop 으로 제외)
function goDetail(id) {
    router.push({ name: 'admin-client-detail', params: { id } })
}
</script>

<template>
    <section class="clients">
        <!-- 툴바: 검색 + 등록 -->
        <div class="toolbar">
            <form class="search" @submit.prevent="doSearch">
                <input v-model="searchInput" placeholder="건물명으로 검색" />
                <button class="btn" type="submit">검색</button>
                <button v-if="appliedKeyword" class="btn btn--ghost" type="button" @click="resetSearch">초기화</button>
            </form>
            <button class="btn btn--primary" type="button" @click="router.push({ name: 'admin-client-new' })">
                + 새 거래처
            </button>
        </div>

        <!-- 상태 -->
        <TableSkeleton v-if="isLoading" :rows="5" />
        <p v-else-if="isError" class="state state--err">목록을 불러오지 못했습니다.</p>

        <!-- 목록 -->
        <div v-else class="table-wrap">
            <div class="list-meta">
                <span>총 <strong>{{ totalElements }}</strong>건</span>
                <span v-if="isFetching" class="list-meta__sync">갱신 중…</span>
            </div>

            <table v-if="clients.length" class="table">
                <thead>
                    <tr>
                        <th>건물명</th>
                        <th>청소 종류</th>
                        <th>담당자</th>
                        <th>연락처</th>
                        <th>계약 시작일</th>
                        <th class="col-actions">작업</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="c in clients" :key="c.id" class="row-click" @click="goDetail(c.id)">
                        <td>
                            <span class="name-link">{{ c.name }}</span>
                        </td>
                        <td>
                            <span v-if="c.cleaningType" class="tag" :class="'tag--' + c.cleaningType.toLowerCase()">
                                {{ c.cleaningTypeLabel }}
                            </span>
                            <span v-else class="muted">-</span>
                        </td>
                        <td>{{ c.managerName || '-' }}</td>
                        <td>{{ c.managerPhone || '-' }}</td>
                        <td>{{ fmtDate(c.contractStartDate) }}</td>
                        <td class="col-actions" @click.stop>
                            <RouterLink class="btn btn--sm" :to="{ name: 'admin-client-edit', params: { id: c.id } }">
                                수정
                            </RouterLink>
                            <button class="btn btn--sm btn--danger" type="button" @click="onDelete(c)">삭제</button>
                        </td>
                    </tr>
                </tbody>
            </table>

            <EmptyState v-else icon="🏢" message="등록된 거래처가 없습니다.">
                <button class="btn btn--primary" type="button" @click="router.push({ name: 'admin-client-new' })">
                    첫 거래처 등록하기
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

.tag--regular {
    background: var(--primary-soft);
    color: var(--primary-hover);
}

.tag--special {
    background: #fef3c7;
    color: #92400e;
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
