<script setup>
// 적정가 재산정 — 진행 중 계약의 현재 월정액과 지금 기준 권장가를 나란히 본다.
//
// 거래처 단가가 몇 년 전에 정해진 채 그대로인 경우가 많아 "지금 얼마가 적정인가"를
// 한 번에 보려는 화면이다. 금액을 자동으로 바꾸지 않는다 — 인상은 협상이기 때문이다.
import { computed, ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'

import { pricingService } from '@/services/admin/pricing/pricingService'
import TableSkeleton from '@/common/components/common/TableSkeleton.vue'
import EmptyState from '@/common/components/common/EmptyState.vue'

const { data, isLoading, isError } = useQuery({
    queryKey: ['pricing-review'],
    queryFn: () => pricingService.review().then((r) => r.data.data),
})

const rows = computed(() => data.value?.rows ?? [])
const summary = computed(() => data.value ?? null)
// 빠진 계약은 기본으로 접어 둔다 — 목록이 길면 본 표가 밀린다.
const skipped = computed(() => data.value?.skipped ?? [])
const showSkipped = ref(false)

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

// 산출 근거를 펼쳐 놓은 계약 id 들.
// 거래처가 많으면 전부 펼쳐진 표는 읽기 어려우므로 접어 두고 필요한 것만 연다.
const expanded = ref(new Set())

function toggle(contractId) {
    // Set 을 직접 수정하면 Vue 가 변경을 감지하지 못한다. 새 Set 으로 교체한다.
    const next = new Set(expanded.value)
    if (next.has(contractId)) {
        next.delete(contractId)
    } else {
        next.add(contractId)
    }
    expanded.value = next
}

function isOpen(contractId) {
    return expanded.value.has(contractId)
}

function toggleAll() {
    expanded.value = expanded.value.size > 0
        ? new Set()
        : new Set(rows.value.map((r) => r.contractId))
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

            <!-- 제외된 계약을 밝힌다. 조용히 빼면 전체를 검토한 것으로 오해하고,
                 건수만 알려주면 어디를 고쳐야 할지 찾을 수 없어 손을 못 댄다. -->
            <div v-if="skipped.length" class="skipped">
                <div class="skipped__head">
                    <b>검토에서 빠진 계약 {{ skipped.length }}건</b>
                    <button class="btn btn--sm" type="button" @click="showSkipped = !showSkipped">
                        {{ showSkipped ? '접기' : '어떤 계약인지 보기' }}
                    </button>
                </div>
                <p class="skipped__sum">
                    <span v-if="summary.skippedNoBuilding">건물 규모 미입력 {{ summary.skippedNoBuilding }}건</span>
                    <span v-if="summary.skippedNoBuilding && summary.skippedNoCycle"> · </span>
                    <span v-if="summary.skippedNoCycle">방문 횟수 미확인 {{ summary.skippedNoCycle }}건</span>
                </p>

                <table v-if="showSkipped" class="table table--skipped">
                    <thead>
                        <tr><th>거래처</th><th>빠진 이유</th><th>고치는 법</th></tr>
                    </thead>
                    <tbody>
                        <tr v-for="s in skipped" :key="s.contractId">
                            <td>
                                <RouterLink :to="{ name: 'admin-contract-detail', params: { id: s.contractId } }">
                                    {{ s.clientName || '(거래처 없음)' }}
                                </RouterLink>
                                <div class="sub">{{ s.contractTitle }}</div>
                            </td>
                            <td>
                                <span class="tag" :class="'tag--' + s.reason.toLowerCase().replace('_', '-')">
                                    {{ s.reasonLabel }}
                                </span>
                            </td>
                            <td class="muted">
                                {{ s.howToFix }}
                                <RouterLink
                                    v-if="s.reason === 'NO_BUILDING' && s.clientId"
                                    class="fix-link"
                                    :to="{ name: 'admin-client-edit', params: { id: s.clientId } }"
                                >거래처 수정</RouterLink>
                                <RouterLink
                                    v-else-if="s.reason === 'NO_VISITS'"
                                    class="fix-link"
                                    :to="{ name: 'admin-contract-edit', params: { id: s.contractId } }"
                                >계약 수정</RouterLink>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="rows.length" class="table-wrap">
                <div class="table-tools">
                    <button class="btn btn--sm" type="button" @click="toggleAll">
                        {{ expanded.size > 0 ? '산출 근거 모두 접기' : '산출 근거 모두 펼치기' }}
                    </button>
                </div>
                <table class="table">
                    <thead>
                        <tr>
                            <th class="col-toggle"></th>
                            <th>거래처</th>
                            <th>건물 규모</th>
                            <th>주기</th>
                            <th class="r">현재</th>
                            <th class="r">권장가</th>
                            <th class="r">차이</th>
                        </tr>
                    </thead>
                    <tbody>
                        <template v-for="row in rows" :key="row.contractId">
                            <tr class="row-main" @click="toggle(row.contractId)">
                                <td class="col-toggle">
                                    <span class="caret" :class="{ 'caret--open': isOpen(row.contractId) }">▶</span>
                                </td>
                                <td @click.stop>
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
                            <!-- 산출 근거 — 거래처에 인상을 요청할 때 그대로 읽어줄 수 있는 내역 -->
                            <tr v-if="isOpen(row.contractId)" class="row-detail">
                                <td></td>
                                <td colspan="6">
                                    <div class="bd">
                                        <div class="bd__title">권장가 {{ money(row.recommendedAmount) }}원 산출 근거</div>
                                        <ul class="bd__list">
                                            <li v-for="line in row.breakdown" :key="line.label">
                                                <span class="bd__label">{{ line.label }}</span>
                                                <span class="bd__detail">{{ line.detail }}</span>
                                                <span class="bd__amount">{{ money(line.amount) }}원</span>
                                            </li>
                                            <li class="bd__coef">
                                                <span class="bd__label">주기 계수</span>
                                                <span class="bd__detail">x {{ row.coefficient }} ({{ row.cycleLabel }})</span>
                                                <span class="bd__amount">{{ money(row.recommendedAmount) }}원</span>
                                            </li>
                                        </ul>
                                    </div>
                                </td>
                            </tr>
                        </template>
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
.skipped__head { display: flex; align-items: center; justify-content: space-between; gap: 0.8rem; flex-wrap: wrap; }
.skipped__sum { margin: 0.15rem 0 0; font-size: 0.78rem; }
.table--skipped { margin-top: 0.7rem; background: #fff; border-radius: 10px; overflow: hidden; }
.table--skipped th { background: #f7efd4; color: #6b5900; }
.table--skipped td { color: var(--text-h); }
.tag { display: inline-block; padding: 0.13rem 0.5rem; border-radius: 999px; font-size: 0.75rem; font-weight: 600; white-space: nowrap; }
.tag--no-building { background: #fde8e8; color: #a33; }
.tag--no-visits { background: #e8eefd; color: #33a; }
.fix-link { margin-left: 0.4rem; font-weight: 600; color: var(--primary); white-space: nowrap; }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow-x: auto; }
.table-tools { padding: 0.6rem 0.9rem; border-bottom: 1px solid var(--border); }
.btn { padding: 0.5rem 0.9rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; }
.btn:hover { border-color: var(--primary); color: var(--primary); }
.btn--sm { padding: 0.3rem 0.6rem; font-size: 0.8rem; }
.col-toggle { width: 1.8rem; text-align: center; padding-right: 0 !important; }
.caret { display: inline-block; font-size: 0.62rem; color: var(--text); transition: transform 0.15s; }
.caret--open { transform: rotate(90deg); }
.row-main { cursor: pointer; }
.row-detail td { background: var(--muted); padding-top: 0; padding-bottom: 0.8rem; }
.bd { background: #fff; border: 1px solid var(--border); border-radius: 10px; padding: 0.7rem 0.9rem; }
.bd__title { font-size: 0.8rem; font-weight: 700; color: var(--text-h); margin-bottom: 0.45rem; }
.bd__list { list-style: none; margin: 0; padding: 0; font-size: 0.8rem; }
.bd__list li { display: flex; align-items: baseline; gap: 0.5rem; padding: 0.16rem 0; }
.bd__label { min-width: 5.6rem; font-weight: 600; color: var(--text-h); }
.bd__detail { flex: 1; color: var(--text); }
.bd__amount { font-variant-numeric: tabular-nums; color: var(--text-h); white-space: nowrap; }
.bd__coef { border-top: 1px solid var(--border); margin-top: 0.3rem; padding-top: 0.4rem !important; font-weight: 700; }
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
