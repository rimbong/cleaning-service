<script setup>
// 견적 상세 — 정보 표시 + 수정/삭제/목록 진입.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

import { quoteService } from '@/services/admin/quote/quoteService'
import { useNotifyStore } from '@/stores/common/notify/notify'

const props = defineProps({
    id: { type: [String, Number], required: true },
})

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

const { data, isLoading, isError } = useQuery({
    queryKey: ['quote', computed(() => String(props.id))],
    queryFn: () => quoteService.get(props.id).then((res) => res.data.data),
    staleTime: 30_000,
})

const quote = computed(() => data.value)

/**
 * 목록으로 이동 — 뒤로가기 히스토리가 있으면 그대로 돌아가(목록에서 보던 페이지 유지),
 * 딥링크로 바로 상세에 들어온 경우엔 목록 route 로 이동한다.
 */
function goList() {
    if (window.history.state?.back) {
        router.back()
    } else {
        router.push({ name: 'admin-quotes' })
    }
}

const removeMutation = useMutation({
    mutationFn: () => quoteService.remove(props.id),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['quotes'] })
        router.replace({ name: 'admin-quotes' })
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    },
})

async function onDelete() {
    if (!(await notify.confirm(`'${quote.value.title}' 견적을 삭제하시겠습니까?`))) {
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
        <p v-else-if="isError" class="state state--err">견적을 불러오지 못했습니다.</p>

        <template v-else-if="quote">
            <div class="detail-head">
                <button class="btn btn--ghost" type="button" @click="goList">
                    ← 목록
                </button>
                <div class="detail-head__actions">
                    <RouterLink class="btn" :to="{ name: 'admin-quote-edit', params: { id: quote.id } }">수정</RouterLink>
                    <button class="btn btn--danger" type="button" @click="onDelete">삭제</button>
                </div>
            </div>

            <div class="card">
                <div class="card-head">
                    <h2 class="card-title">{{ quote.title }}</h2>
                    <span v-if="quote.status" class="tag" :class="'tag--' + quote.status.toLowerCase()">
                        {{ quote.statusLabel }}
                    </span>
                </div>
                <dl class="info">
                    <div class="info-row">
                        <dt>거래처</dt>
                        <dd>
                            <RouterLink
                                v-if="quote.clientId"
                                class="link"
                                :to="{ name: 'admin-client-detail', params: { id: quote.clientId } }"
                            >
                                {{ quote.clientName }}
                            </RouterLink>
                            <template v-else>미연결(신규 고객)</template>
                        </dd>
                    </div>
                    <div class="info-row">
                        <dt>고객명</dt>
                        <dd>{{ fmt(quote.customerName) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>연락처</dt>
                        <dd>{{ fmt(quote.customerPhone) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>현장 주소</dt>
                        <dd>{{ fmt(quote.address) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>견적 금액</dt>
                        <dd>{{ fmtMoney(quote.amount) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>견적일</dt>
                        <dd>{{ fmtDate(quote.quoteDate) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>유효기간</dt>
                        <dd>{{ quote.validUntil ? fmtDate(quote.validUntil) : '-' }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>메모</dt>
                        <dd class="info-multi">{{ fmt(quote.memo) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>등록일</dt>
                        <dd>{{ fmtDateTime(quote.createdAt) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>수정일</dt>
                        <dd>{{ fmtDateTime(quote.updatedAt) }}</dd>
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

/* 버튼은 전역 .btn 계열(style.css) 사용 */

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

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}

.state--err {
    color: var(--danger);
}
</style>
