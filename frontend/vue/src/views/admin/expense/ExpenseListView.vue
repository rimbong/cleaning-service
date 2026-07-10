<script setup>
// 지출 목록 — 검색·페이징·등록/수정/삭제. 행 클릭 시 수정.
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/vue-query'

import { expenseService } from '@/services/admin/expense/expenseService'
import Pager from '@/common/components/common/Pager.vue'
import { useNotifyStore } from '@/stores/common/notify/notify'

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

const searchInput = ref('')
const appliedKeyword = ref('')
const page = ref(1)

// 검색어가 바뀌면 1페이지로 되돌린다.
watch(appliedKeyword, () => {
    page.value = 1
})

const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['expenses', appliedKeyword, page],
    queryFn: () => expenseService
        .list({ keyword: appliedKeyword.value, page: page.value })
        .then((r) => r.data.data),
    staleTime: 30_000,
    placeholderData: keepPreviousData,
})
const expenses = computed(() => data.value?.content ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 0)
// 합계는 현재 페이지 기준(전체 합계는 별도 집계가 필요하므로 페이지 합계만 표시)
const pageSum = computed(() => expenses.value.reduce((s, e) => s + (e.amount ?? 0), 0))

function doSearch() { appliedKeyword.value = searchInput.value.trim() }
function resetSearch() { searchInput.value = ''; appliedKeyword.value = '' }

const removeMut = useMutation({
    mutationFn: (id) => expenseService.remove(id),
    onSuccess: () => { notify.toast('삭제되었습니다.', { type: 'info' }); queryClient.invalidateQueries({ queryKey: ['expenses'] }) },
    onError: (e) => notify.bar(e.response?.data?.message ?? '삭제 실패', { color: 'red' }),
})
async function onDelete(e) {
    if (!(await notify.confirm(`'${e.vendorName || e.categoryLabel}' 지출을 삭제하시겠습니까?`))) return
    removeMut.mutate(e.id)
}

function money(v) { return v != null ? Number(v).toLocaleString('ko-KR') : '0' }
function fmtDate(v) { return v ? String(v).slice(0, 10) : '-' }
function goEdit(id) { router.push({ name: 'admin-expense-edit', params: { id } }) }

const downloading = ref(false)
async function onDownload() {
    downloading.value = true
    try {
        await expenseService.downloadExcel(appliedKeyword.value, '지출내역.xlsx')
    } catch (e) {
        notify.bar(e.response?.data?.message ?? '엑셀 다운로드에 실패했습니다.', { color: 'red' })
    } finally {
        downloading.value = false
    }
}
</script>

<template>
    <section class="expenses">
        <div class="toolbar">
            <form class="search" @submit.prevent="doSearch">
                <input v-model="searchInput" placeholder="거래처·주유소명 검색" />
                <button class="btn" type="submit">검색</button>
                <button v-if="appliedKeyword" class="btn btn--ghost" type="button" @click="resetSearch">초기화</button>
            </form>
            <div class="tools">
                <button class="btn" :disabled="downloading" @click="onDownload">
                    {{ downloading ? '내보내는 중…' : '엑셀 다운로드' }}
                </button>
                <button class="btn btn--primary" @click="router.push({ name: 'admin-expense-new' })">+ 새 지출</button>
            </div>
        </div>

        <p v-if="isLoading" class="state">불러오는 중…</p>
        <p v-else-if="isError" class="state state--err">불러오지 못했습니다.</p>
        <div v-else class="table-wrap">
            <div class="list-meta">
                <span>총 <strong>{{ totalElements }}</strong>건 · 이 페이지 합계 <strong>{{ money(pageSum) }}</strong>원</span>
                <span v-if="isFetching" class="sync">갱신 중…</span>
            </div>
            <table v-if="expenses.length" class="table">
                <thead>
                    <tr><th>분류</th><th>거래처/주유소</th><th>금액</th><th>지출일</th><th class="col-actions">작업</th></tr>
                </thead>
                <tbody>
                    <tr v-for="e in expenses" :key="e.id" class="row-click" @click="goEdit(e.id)">
                        <td><span class="tag" :class="'tag--' + e.category.toLowerCase()">{{ e.categoryLabel }}</span></td>
                        <td>{{ e.vendorName || '-' }}</td>
                        <td>{{ money(e.amount) }}원</td>
                        <td>{{ fmtDate(e.expenseDate) }}</td>
                        <td class="col-actions" @click.stop>
                            <RouterLink class="btn btn--sm" :to="{ name: 'admin-expense-edit', params: { id: e.id } }">수정</RouterLink>
                            <button class="btn btn--sm btn--danger" @click="onDelete(e)">삭제</button>
                        </td>
                    </tr>
                </tbody>
            </table>
            <div v-else class="empty">
                <p>등록된 지출이 없습니다.</p>
                <button class="btn btn--primary" @click="router.push({ name: 'admin-expense-new' })">첫 지출 등록</button>
            </div>

            <Pager v-model:page="page" :total-pages="totalPages" :total-elements="totalElements" />
        </div>
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
.row-click { cursor: pointer; }
.col-actions { text-align: right; white-space: nowrap; }
.tag { display: inline-block; padding: 0.15rem 0.55rem; border-radius: 999px; font-size: 0.78rem; font-weight: 600; }
.tag--fuel { background: var(--primary-soft); color: var(--primary-hover); }
.tag--etc { background: #e5e7eb; color: #4b5563; }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
.state--err { color: var(--danger); }
.empty { padding: 3rem 1rem; text-align: center; color: var(--text); }
.empty p { margin-bottom: 1rem; }
</style>
