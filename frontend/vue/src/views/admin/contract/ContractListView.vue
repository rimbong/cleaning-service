<script setup>
// 계약 목록 — vue-query 로 캐싱, 계약명 검색, 등록/상세/수정/삭제 진입.
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

import { contractService } from '@/services/contract/contractService'
import { useNotifyStore } from '@/stores/notify/notify'

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

// 입력 중인 검색어(input)와 실제 조회에 쓰는 검색어(applied)를 분리 —
// 타이핑마다 요청하지 않고 "검색" 실행 시점에만 반영한다.
const searchInput = ref('')
const appliedKeyword = ref('')

// queryKey 에 검색어를 넣어, 검색어별로 캐시가 분리·재사용된다.
const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['contracts', appliedKeyword],
    queryFn: () => contractService.list({ keyword: appliedKeyword.value }).then((res) => res.data.data),
    staleTime: 30_000, // 30초 이내 재방문은 캐시 사용
})

const contracts = computed(() => data.value ?? [])

function doSearch() {
    appliedKeyword.value = searchInput.value.trim()
}

function resetSearch() {
    searchInput.value = ''
    appliedKeyword.value = ''
}

// 삭제 — 성공 시 목록 캐시 무효화(자동 갱신)
const removeMutation = useMutation({
    mutationFn: (id) => contractService.remove(id),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['contracts'] })
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    },
})

async function onDelete(contract) {
    if (!(await notify.confirm(`'${contract.title}' 계약을 삭제하시겠습니까?`))) {
        return
    }
    removeMutation.mutate(contract.id)
}

function fmtDate(v) {
    return v ? String(v).slice(0, 10) : '-'
}

function fmtMoney(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') + '원' : '-'
}

// 목록 행 클릭 → 상세로 이동(수정/삭제 버튼은 @click.stop 으로 제외)
function goDetail(id) {
    router.push({ name: 'admin-contract-detail', params: { id } })
}
</script>

<template>
    <section class="contracts">
        <!-- 툴바: 검색 + 등록 -->
        <div class="toolbar">
            <form class="search" @submit.prevent="doSearch">
                <input v-model="searchInput" placeholder="계약명으로 검색" />
                <button class="btn" type="submit">검색</button>
                <button v-if="appliedKeyword" class="btn btn--ghost" type="button" @click="resetSearch">초기화</button>
            </form>
            <button class="btn btn--primary" type="button" @click="router.push({ name: 'admin-contract-new' })">
                + 새 계약
            </button>
        </div>

        <!-- 상태 -->
        <p v-if="isLoading" class="state">불러오는 중…</p>
        <p v-else-if="isError" class="state state--err">목록을 불러오지 못했습니다.</p>

        <!-- 목록 -->
        <div v-else class="table-wrap">
            <div class="list-meta">
                <span>총 <strong>{{ contracts.length }}</strong>건</span>
                <span v-if="isFetching" class="list-meta__sync">갱신 중…</span>
            </div>

            <table v-if="contracts.length" class="table">
                <thead>
                    <tr>
                        <th>계약명</th>
                        <th>거래처</th>
                        <th>월 청구금액</th>
                        <th>시작일</th>
                        <th>상태</th>
                        <th class="col-actions">작업</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="c in contracts" :key="c.id" class="row-click" @click="goDetail(c.id)">
                        <td>
                            <span class="name-link">{{ c.title }}</span>
                        </td>
                        <td>{{ c.clientName || '-' }}</td>
                        <td>{{ fmtMoney(c.monthlyFee) }}</td>
                        <td>{{ fmtDate(c.startDate) }}</td>
                        <td>
                            <span v-if="c.status" class="tag" :class="'tag--' + c.status.toLowerCase()">
                                {{ c.statusLabel }}
                            </span>
                            <span v-else class="muted">-</span>
                        </td>
                        <td class="col-actions" @click.stop>
                            <RouterLink class="btn btn--sm" :to="{ name: 'admin-contract-edit', params: { id: c.id } }">
                                수정
                            </RouterLink>
                            <button class="btn btn--sm btn--danger" type="button" @click="onDelete(c)">삭제</button>
                        </td>
                    </tr>
                </tbody>
            </table>

            <div v-else class="empty">
                <p>등록된 계약이 없습니다.</p>
                <button class="btn btn--primary" type="button" @click="router.push({ name: 'admin-contract-new' })">
                    첫 계약 등록하기
                </button>
            </div>
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

/* 버튼 공통 */
.btn {
    padding: 0.5rem 0.9rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-h);
    cursor: pointer;
    font: inherit;
    text-decoration: none;
    display: inline-flex;
    align-items: center;
}

.btn:hover {
    border-color: var(--primary);
    color: var(--primary);
}

.btn--primary {
    background: var(--primary);
    border-color: var(--primary);
    color: var(--primary-fg);
}

.btn--primary:hover {
    background: var(--primary-hover);
    color: var(--primary-fg);
}

.btn--ghost {
    background: transparent;
    border-color: transparent;
    color: var(--text);
}

.btn--sm {
    padding: 0.3rem 0.6rem;
    font-size: 0.82rem;
}

.btn--danger {
    color: var(--danger);
}

.btn--danger:hover {
    border-color: var(--danger);
    color: var(--danger);
    background: var(--danger-soft);
}

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

.tag--active {
    background: var(--primary-soft);
    color: var(--primary-hover);
}

.tag--ended {
    background: #e5e7eb;
    color: #4b5563;
}

.tag--suspended {
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
