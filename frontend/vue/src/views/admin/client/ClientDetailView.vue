<script setup>
// 거래처 상세 — 정보 표시 + 수정/삭제/목록 진입.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

import { clientService } from '@/services/admin/client/clientService'
import { contractService } from '@/services/admin/contract/contractService'
import { useNotifyStore } from '@/stores/common/notify/notify'

const props = defineProps({
    id: { type: [String, Number], required: true },
})

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

// 단건도 캐시(목록에서 넘어오면 즉시 표시 가능)
const { data, isLoading, isError } = useQuery({
    queryKey: ['client', computed(() => String(props.id))],
    queryFn: () => clientService.get(props.id).then((res) => res.data.data),
    staleTime: 30_000,
})

const client = computed(() => data.value)

// 이 거래처에 걸린 계약 목록 — queryKey 가 'contracts' 로 시작하므로
// 계약 등록/수정/삭제 시 invalidateQueries(['contracts'])로 함께 갱신된다.
// 한 거래처의 계약은 보통 소수라, 큰 size 로 한 번에 받아 전부 표시한다(목록 API 는 페이징 응답).
const { data: contractData } = useQuery({
    queryKey: ['contracts', 'byClient', computed(() => String(props.id))],
    queryFn: () => contractService.list({ clientId: props.id, size: 200 }).then((res) => res.data.data),
    staleTime: 30_000,
})
const contracts = computed(() => contractData.value?.content ?? [])

function goAddContract() {
    router.push({ name: 'admin-contract-new', query: { clientId: props.id } })
}

function goContract(id) {
    router.push({ name: 'admin-contract-detail', params: { id } })
}

function fmtMoney(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') + '원' : '-'
}

const removeMutation = useMutation({
    mutationFn: () => clientService.remove(props.id),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['clients'] })
        router.replace({ name: 'admin-clients' })
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    },
})

async function onDelete() {
    if (!(await notify.confirm(`'${client.value.name}' 거래처를 삭제하시겠습니까?`))) {
        return
    }
    removeMutation.mutate()
}

function fmt(v) {
    return v ? String(v) : '-'
}

function fmtDate(v) {
    return v ? String(v).slice(0, 10) : '-'
}

function fmtDateTime(v) {
    return v ? String(v).slice(0, 16).replace('T', ' ') : '-'
}

// 표시용 필드 행
const rows = computed(() => {
    const c = client.value
    if (!c) {
        return []
    }
    return [
        { label: '건물명', value: c.name },
        { label: '청소 종류', value: c.cleaningTypeLabel, tag: c.cleaningType },
        { label: '주소', value: fmt(c.address) },
        { label: '담당자', value: fmt(c.managerName) },
        { label: '연락처', value: fmt(c.managerPhone) },
        { label: '계약 시작일', value: fmtDate(c.contractStartDate) },
        { label: '사업자번호', value: fmt(c.businessNumber) },
        { label: '대표자', value: fmt(c.representativeName) },
        { label: '업태', value: fmt(c.businessType) },
        { label: '종목', value: fmt(c.businessItem) },
        { label: '세금계산서', value: fmt(c.taxInvoiceTypeLabel) },
        { label: '메모', value: fmt(c.memo), multiline: true },
        { label: '등록일', value: fmtDateTime(c.createdAt) },
        { label: '수정일', value: fmtDateTime(c.updatedAt) },
    ]
})
</script>

<template>
    <section class="detail-page">
        <p v-if="isLoading" class="state">불러오는 중…</p>
        <p v-else-if="isError" class="state state--err">거래처를 불러오지 못했습니다.</p>

        <template v-else-if="client">
            <div class="detail-head">
                <button class="btn btn--ghost" type="button" @click="router.push({ name: 'admin-clients' })">
                    ← 목록
                </button>
                <div class="detail-head__actions">
                    <RouterLink class="btn" :to="{ name: 'admin-client-edit', params: { id: client.id } }">수정</RouterLink>
                    <button class="btn btn--danger" type="button" @click="onDelete">삭제</button>
                </div>
            </div>

            <div class="card">
                <h2 class="card-title">{{ client.name }}</h2>
                <dl class="info">
                    <div v-for="row in rows" :key="row.label" class="info-row">
                        <dt>{{ row.label }}</dt>
                        <dd :class="{ 'info-multi': row.multiline }">
                            <span v-if="row.tag && row.value" class="tag" :class="'tag--' + row.tag.toLowerCase()">
                                {{ row.value }}
                            </span>
                            <template v-else>{{ row.value }}</template>
                        </dd>
                    </div>
                </dl>
            </div>

            <!-- 이 거래처의 계약 -->
            <div class="card contracts-card">
                <div class="contracts-head">
                    <h3 class="contracts-title">계약 <span class="count">{{ contracts.length }}</span></h3>
                    <button class="btn btn--primary btn--sm" type="button" @click="goAddContract">+ 계약 추가</button>
                </div>

                <ul v-if="contracts.length" class="contract-list">
                    <li v-for="ct in contracts" :key="ct.id" class="contract-item" @click="goContract(ct.id)">
                        <div class="contract-item__main">
                            <span class="contract-item__title">{{ ct.title }}</span>
                            <span v-if="ct.status" class="tag tag--sm" :class="'tag--' + ct.status.toLowerCase()">
                                {{ ct.statusLabel }}
                            </span>
                        </div>
                        <span class="contract-item__fee">{{ fmtMoney(ct.monthlyFee) }}/월</span>
                    </li>
                </ul>
                <p v-else class="contracts-empty">등록된 계약이 없습니다.</p>
            </div>
        </template>
    </section>
</template>

<style scoped>
.detail-page {
    max-width: 680px;
    margin: 0 auto;
}

.detail-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
}

.detail-head__actions {
    display: flex;
    gap: 0.5rem;
}

.btn {
    padding: 0.45rem 0.9rem;
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

.btn--ghost {
    background: transparent;
    border-color: transparent;
    color: var(--text);
}

.btn--danger {
    color: var(--danger);
}

.btn--danger:hover {
    border-color: var(--danger);
    color: var(--danger);
    background: var(--danger-soft);
}

.card {
    background: #fff;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    padding: 1.5rem;
}

.card-title {
    margin: 0 0 1.25rem;
    font-size: 1.25rem;
}

.info {
    margin: 0;
    display: flex;
    flex-direction: column;
}

.info-row {
    display: grid;
    grid-template-columns: 120px 1fr;
    gap: 1rem;
    padding: 0.7rem 0;
    border-bottom: 1px solid var(--border);
}

.info-row:last-child {
    border-bottom: none;
}

.info-row dt {
    color: var(--text);
    font-size: 0.88rem;
    font-weight: 600;
}

.info-row dd {
    margin: 0;
    color: var(--text-h);
}

.info-multi {
    white-space: pre-wrap;
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

/* 계약 상태 tag */
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

.tag--sm {
    padding: 0.1rem 0.45rem;
    font-size: 0.72rem;
}

/* 이 거래처의 계약 섹션 */
.contracts-card {
    margin-top: 1.25rem;
}

.contracts-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
}

.contracts-title {
    margin: 0;
    font-size: 1.05rem;
}

.contracts-title .count {
    color: var(--primary);
    font-weight: 700;
    margin-left: 0.25rem;
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

.btn--sm {
    padding: 0.3rem 0.7rem;
    font-size: 0.82rem;
}

.contract-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
}

.contract-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
    padding: 0.75rem 0.25rem;
    border-bottom: 1px solid var(--border);
    cursor: pointer;
}

.contract-item:last-child {
    border-bottom: none;
}

.contract-item:hover {
    background: var(--muted);
}

.contract-item__main {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    min-width: 0;
}

.contract-item__title {
    font-weight: 600;
    color: var(--text-h);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.contract-item__fee {
    color: var(--text);
    font-size: 0.88rem;
    white-space: nowrap;
}

.contracts-empty {
    color: var(--text);
    text-align: center;
    padding: 1.5rem 0;
    margin: 0;
}

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}

.state--err {
    color: var(--danger);
}
</style>
