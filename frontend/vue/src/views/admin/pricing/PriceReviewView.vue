<script setup>
// 적정가 재산정 — 진행 중 계약의 현재 월정액과 지금 기준 권장가를 나란히 본다.
//
// 거래처 단가가 몇 년 전에 정해진 채 그대로인 경우가 많아 "지금 얼마가 적정인가"를
// 한 번에 보려는 화면이다. 금액을 자동으로 바꾸지 않는다 — 인상은 협상이기 때문이다.
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'

import { pricingService } from '@/services/admin/pricing/pricingService'
import TableSkeleton from '@/common/components/common/TableSkeleton.vue'
import EmptyState from '@/common/components/common/EmptyState.vue'

const { data, isLoading, isError } = useQuery({
    queryKey: ['pricing-review'],
    queryFn: () => pricingService.review().then((r) => r.data.data),
    staleTime: 30_000,
})

const rows = computed(() => data.value?.rows ?? [])
const summary = computed(() => data.value ?? null)

function money(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') : '-'
}

/** 인상폭에 따라 색을 나눈다 — 크게 벌어진 거래처가 먼저 눈에 띄어야 한다. */
function gapClass(row) {
    if (row.difference > 0) {
        return row.differenceRate >= 20 ? 'up up--big' : 'up'
    }
    return row.difference < 0 ? 'down' : ''
}
</script>

<template>
    <section class="review">
        <p class="lead">
            진행 중 계약의 <b>현재 월정액</b>과 <b>지금 단가표 기준 권장가</b>를 비교합니다.
            여기서 금액이 바뀌지는 않습니다 — 인상은 거래처와 협의할 사항이라 검토 자료로만 씁니다.
        </p>

        <TableSkeleton v-if="isLoading" :rows="6" />
        <p v-else-if="isError" class="state state--err">불러오지 못했습니다.</p>
        <template v-else>
            <div v-if="summary" class="summary">
                <div class="sum-card">
                    <div class="sum-big">{{ summary.reviewedCount }}건</div>
                    <div class="sum-lbl">검토 대상 계약</div>
                </div>
                <div class="sum-card">
                    <div class="sum-big">{{ money(summary.totalCurrent) }}원</div>
                    <div class="sum-lbl">현재 월 합계</div>
                </div>
                <div class="sum-card sum-card--accent">
                    <div class="sum-big">{{ money(summary.totalRecommended) }}원</div>
                    <div class="sum-lbl">권장가 월 합계</div>
                </div>
                <div class="sum-card" :class="summary.totalRecommended >= summary.totalCurrent ? 'sum-card--up' : ''">
                    <div class="sum-big">
                        {{ summary.totalRecommended - summary.totalCurrent > 0 ? '+' : '' }}{{ money(summary.totalRecommended - summary.totalCurrent) }}원
                    </div>
                    <div class="sum-lbl">차이 (월)</div>
                </div>
            </div>

            <!-- 제외된 계약을 밝힌다. 조용히 빼면 전체를 검토한 것으로 오해한다. -->
            <div v-if="summary && (summary.skippedNoBuilding || summary.skippedNoCycle)" class="skipped">
                <b>검토에서 빠진 계약이 있습니다.</b>
                <ul>
                    <li v-if="summary.skippedNoBuilding">
                        건물 규모 미입력 <b>{{ summary.skippedNoBuilding }}건</b> —
                        거래처 수정에서 층수·세대수를 넣으면 대상이 됩니다.
                    </li>
                    <li v-if="summary.skippedNoCycle">
                        청소 주기 미입력 <b>{{ summary.skippedNoCycle }}건</b> —
                        계약 수정에서 청소 주기를 지정하세요.
                    </li>
                </ul>
            </div>

            <div v-if="rows.length" class="table-wrap">
                <table class="table">
                    <thead>
                        <tr>
                            <th>거래처</th>
                            <th>건물 규모</th>
                            <th>주기</th>
                            <th class="r">현재</th>
                            <th class="r">권장가</th>
                            <th class="r">차이</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in rows" :key="row.contractId">
                            <td>
                                <RouterLink :to="{ name: 'admin-contract-detail', params: { id: row.contractId } }">
                                    {{ row.clientName }}
                                </RouterLink>
                                <div class="sub">{{ row.contractTitle }}</div>
                            </td>
                            <td class="muted">{{ row.buildingSummary }}</td>
                            <td class="muted">{{ row.cycleLabel }}</td>
                            <td class="r">{{ money(row.currentAmount) }}</td>
                            <td class="r strong">{{ money(row.recommendedAmount) }}</td>
                            <td class="r" :class="gapClass(row)">
                                {{ row.difference > 0 ? '+' : '' }}{{ money(row.difference) }}
                                <div class="rate">
                                    {{ row.differenceRate > 0 ? '+' : '' }}{{ row.differenceRate }}%
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <EmptyState
                v-else
                icon="📐"
                message="검토할 계약이 없습니다. 거래처에 건물 규모(층수·세대수)를 넣으면 여기에 나타납니다."
            />

            <div class="tip">
                <b>인상 실무 팁:</b>
                한 번에 크게 올리기보다 재계약·연초에 10~20%씩 단계적으로 올리면 저항이 적습니다.
                계약서에 "매년 최저임금 인상률만큼 자동 조정" 조항을 넣어두면 매번 협상하지 않아도 됩니다.
                인상 안내 시에는 최저임금·자재비 상승이라는 근거를 함께 제시하세요.
            </div>
        </template>
    </section>
</template>

<style scoped>
.review { max-width: 1000px; margin: 0 auto; }
.lead { margin: 0 0 1.2rem; font-size: 0.88rem; color: var(--text); line-height: 1.6; }
.summary { display: flex; gap: 0.75rem; flex-wrap: wrap; margin-bottom: 1rem; }
.sum-card { flex: 1; min-width: 150px; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); padding: 0.85rem 1rem; }
.sum-card--accent { border-color: var(--primary); }
.sum-card--up .sum-big { color: #b45309; }
.sum-big { font-size: 1.15rem; font-weight: 800; color: var(--text-h); letter-spacing: -0.4px; }
.sum-card--accent .sum-big { color: var(--primary); }
.sum-lbl { font-size: 0.76rem; color: var(--text); margin-top: 0.15rem; }
.skipped { background: #fff8e6; border: 1px solid #f0e0a8; border-radius: var(--radius); padding: 0.8rem 1rem; font-size: 0.82rem; color: #6b5900; margin-bottom: 1rem; line-height: 1.6; }
.skipped ul { margin: 0.35rem 0 0; padding-left: 1.1rem; }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow-x: auto; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 0.65rem 0.9rem; text-align: left; border-bottom: 1px solid var(--border); font-size: 0.86rem; vertical-align: top; }
.table th { background: var(--muted); font-size: 0.79rem; color: var(--text); white-space: nowrap; }
.table tbody tr:last-child td { border-bottom: none; }
.table tbody tr:hover { background: var(--muted); }
.table a { color: var(--text-h); font-weight: 600; text-decoration: none; }
.table a:hover { color: var(--primary); }
.r { text-align: right; white-space: nowrap; font-variant-numeric: tabular-nums; }
.strong { font-weight: 700; color: var(--primary); }
.sub { font-size: 0.76rem; color: var(--text); margin-top: 0.1rem; }
.muted { color: var(--text); font-size: 0.82rem; }
.rate { font-size: 0.74rem; font-weight: 600; }
.up { color: #b45309; font-weight: 600; }
.up--big { color: var(--danger); }
.down { color: #0f9d58; font-weight: 600; }
.tip { background: #e7f6ee; border: 1px solid #b7e0c8; border-radius: var(--radius); padding: 0.85rem 1rem; font-size: 0.83rem; color: #155e37; line-height: 1.65; margin-top: 1.2rem; }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
.state--err { color: var(--danger); }
</style>
