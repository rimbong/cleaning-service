<script setup>
// 거래처 상세 — 정보 표시 + 수정/삭제/목록 진입.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

import { clientService } from '@/services/client/clientService'
import { useNotifyStore } from '@/stores/notify/notify'

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

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}

.state--err {
    color: var(--danger);
}
</style>
