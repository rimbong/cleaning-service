<script setup>
// 계약 상세 — 정보 표시 + 수정/삭제/목록 진입.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

import { contractService } from '@/services/contract/contractService'
import { useNotifyStore } from '@/stores/notify/notify'

const props = defineProps({
    id: { type: [String, Number], required: true },
})

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

// 단건도 캐시(목록에서 넘어오면 즉시 표시 가능)
const { data, isLoading, isError } = useQuery({
    queryKey: ['contract', computed(() => String(props.id))],
    queryFn: () => contractService.get(props.id).then((res) => res.data.data),
    staleTime: 30_000,
})

const contract = computed(() => data.value)

const removeMutation = useMutation({
    mutationFn: () => contractService.remove(props.id),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['contracts'] })
        router.replace({ name: 'admin-contracts' })
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    },
})

async function onDelete() {
    if (!(await notify.confirm(`'${contract.value.title}' 계약을 삭제하시겠습니까?`))) {
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

function fmtMoney(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') + '원' : '-'
}
</script>

<template>
    <section class="detail-page">
        <p v-if="isLoading" class="state">불러오는 중…</p>
        <p v-else-if="isError" class="state state--err">계약을 불러오지 못했습니다.</p>

        <template v-else-if="contract">
            <div class="detail-head">
                <button class="btn btn--ghost" type="button" @click="router.push({ name: 'admin-contracts' })">
                    ← 목록
                </button>
                <div class="detail-head__actions">
                    <RouterLink class="btn" :to="{ name: 'admin-contract-edit', params: { id: contract.id } }">수정</RouterLink>
                    <button class="btn btn--danger" type="button" @click="onDelete">삭제</button>
                </div>
            </div>

            <div class="card">
                <div class="card-head">
                    <h2 class="card-title">{{ contract.title }}</h2>
                    <span v-if="contract.status" class="tag" :class="'tag--' + contract.status.toLowerCase()">
                        {{ contract.statusLabel }}
                    </span>
                </div>
                <dl class="info">
                    <div class="info-row">
                        <dt>거래처</dt>
                        <dd>
                            <RouterLink
                                v-if="contract.clientId"
                                class="link"
                                :to="{ name: 'admin-client-detail', params: { id: contract.clientId } }"
                            >
                                {{ contract.clientName }}
                            </RouterLink>
                            <template v-else>-</template>
                        </dd>
                    </div>
                    <div class="info-row">
                        <dt>월 청구금액</dt>
                        <dd>{{ fmtMoney(contract.monthlyFee) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>청구일</dt>
                        <dd>{{ contract.billingDay ? '매월 ' + contract.billingDay + '일' : '-' }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>계약 시작일</dt>
                        <dd>{{ fmtDate(contract.startDate) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>계약 종료일</dt>
                        <dd>{{ contract.endDate ? fmtDate(contract.endDate) : '무기한' }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>메모</dt>
                        <dd class="info-multi">{{ fmt(contract.memo) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>등록일</dt>
                        <dd>{{ fmtDateTime(contract.createdAt) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>수정일</dt>
                        <dd>{{ fmtDateTime(contract.updatedAt) }}</dd>
                    </div>
                </dl>
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

.card-head {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin: 0 0 1.25rem;
}

.card-title {
    margin: 0;
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

.link {
    color: var(--primary);
    font-weight: 600;
    text-decoration: none;
}

.link:hover {
    text-decoration: underline;
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

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}

.state--err {
    color: var(--danger);
}
</style>
