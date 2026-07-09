<script setup>
// 연간 거래처 수금 현황 — 거래처 x 12개월 매트릭스(칸=그 달 최종 수금일). 정산 입금에서 파생.
// 수정환경.xls "거래처 현황(청소)" 장부의 디지털판. 엑셀 다운로드 지원.
import { computed, ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'

import { settlementService } from '@/services/admin/settlement/settlementService'
import { useNotifyStore } from '@/stores/common/notify/notify'

const notify = useNotifyStore()

const now = new Date()
const year = ref(now.getFullYear())
const MONTHS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]

const { data, isLoading, isError, isFetching } = useQuery({
    queryKey: ['settlement-yearly', computed(() => year.value)],
    queryFn: () => settlementService.getYearly(year.value).then((res) => res.data.data),
    staleTime: 15_000,
})

const rows = computed(() => data.value?.rows ?? [])
const count = computed(() => data.value?.count ?? 0)

function shiftYear(delta) {
    year.value += delta
}

function money(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') : '-'
}

const downloading = ref(false)
async function onDownload() {
    downloading.value = true
    try {
        await settlementService.downloadYearlyExcel(year.value, `거래처수금현황_${year.value}.xlsx`)
    } catch (e) {
        notify.bar('엑셀 다운로드에 실패했습니다.', { color: 'red' })
    } finally {
        downloading.value = false
    }
}
</script>

<template>
    <section class="yearly">
        <div class="toolbar">
            <div class="year-nav">
                <button class="btn" @click="shiftYear(-1)">◀</button>
                <strong>{{ year }}년</strong>
                <button class="btn" @click="shiftYear(1)">▶</button>
                <span v-if="isFetching" class="sync">갱신 중…</span>
            </div>
            <button class="btn btn--primary" :disabled="downloading" @click="onDownload">
                {{ downloading ? '내보내는 중…' : '엑셀 다운로드' }}
            </button>
        </div>

        <p class="hint">각 월 칸은 그 달 <strong>최종 수금일</strong>입니다(정산 입금 기록에서 자동 반영). 빈 칸은 미수금.</p>

        <p v-if="isLoading" class="state">불러오는 중…</p>
        <p v-else-if="isError" class="state state--err">불러오지 못했습니다.</p>
        <div v-else class="table-wrap">
            <div class="list-meta">거래처 <strong>{{ count }}</strong>곳</div>
            <div class="scroll">
                <table v-if="rows.length" class="table">
                    <thead>
                        <tr>
                            <th class="sticky-l col-name">거래처</th>
                            <th>담당자</th>
                            <th>수금</th>
                            <th class="col-fee">월정액</th>
                            <th v-for="m in MONTHS" :key="m" class="col-month">{{ m }}월</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="row in rows" :key="row.contractId">
                            <td class="sticky-l col-name">
                                <span class="name">{{ row.clientName || '-' }}</span>
                                <span v-if="row.title" class="sub">{{ row.title }}</span>
                            </td>
                            <td>{{ row.managerName || '-' }}</td>
                            <td>{{ row.paymentMethod || '-' }}</td>
                            <td class="col-fee">{{ money(row.monthlyFee) }}</td>
                            <td
                                v-for="(mv, idx) in row.months"
                                :key="idx"
                                class="col-month"
                                :class="{ paid: mv }"
                            >
                                {{ mv || '' }}
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div v-else class="empty">
                    <p>{{ year }}년에 해당하는 거래처(계약)가 없습니다.</p>
                </div>
            </div>
        </div>
    </section>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.year-nav { display: flex; align-items: center; gap: 0.6rem; font-size: 1.1rem; }
.sync { color: var(--primary); font-size: 0.78rem; }
.hint { font-size: 0.82rem; color: var(--text); margin: 0 0 1rem; }
.btn { padding: 0.45rem 0.8rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; }
.btn:hover { border-color: var(--primary); color: var(--primary); }
.btn--primary { background: var(--primary); border-color: var(--primary); color: var(--primary-fg); }
.btn--primary:hover:not(:disabled) { background: var(--primary-hover); }
.btn--primary:disabled { opacity: 0.6; cursor: default; }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow: hidden; }
.list-meta { padding: 0.6rem 1rem; border-bottom: 1px solid var(--border); font-size: 0.85rem; color: var(--text); }
.scroll { overflow-x: auto; }
.table { border-collapse: collapse; white-space: nowrap; }
.table th, .table td { padding: 0.5rem 0.7rem; border-bottom: 1px solid var(--border); border-right: 1px solid var(--border); font-size: 0.85rem; text-align: center; }
.table th { background: var(--muted); color: var(--text); font-weight: 600; font-size: 0.8rem; position: sticky; top: 0; }
.col-name { text-align: left; min-width: 160px; }
.col-name .name { font-weight: 600; color: var(--text-h); display: block; }
.col-name .sub { font-size: 0.72rem; color: var(--text); }
.col-fee { text-align: right; }
.col-month { min-width: 46px; color: var(--text); }
.col-month.paid { color: var(--primary-hover); font-weight: 600; background: var(--primary-soft); }
/* 거래처명 열 고정(가로 스크롤 시) */
.sticky-l { position: sticky; left: 0; background: #fff; z-index: 1; }
thead .sticky-l { background: var(--muted); z-index: 2; }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
.state--err { color: var(--danger); }
.empty { padding: 3rem 1rem; text-align: center; color: var(--text); }
</style>
