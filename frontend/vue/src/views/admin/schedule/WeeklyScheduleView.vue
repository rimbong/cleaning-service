<script setup>
// 요일별 청소 스케줄 — 진행 중 계약을 청소 요일(월~일)별로 모아 보여준다.
// "오늘 어디 청소가지?"를 요일로 확인. 거래처명·주소·현관비번·주기 표시(현장 방문용).
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'

import { contractService } from '@/services/admin/contract/contractService'

const router = useRouter()

const { data, isLoading, isError } = useQuery({
    queryKey: ['schedule'],
    queryFn: () => contractService.getSchedule().then((res) => res.data.data),
    staleTime: 30_000,
})

const days = computed(() => data.value?.days ?? [])
// 오늘 요일 코드(강조용). JS getDay(): 0=일 ~ 6=토
const TODAY = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'][new Date().getDay()]

function goContract(id) {
    router.push({ name: 'admin-contract-detail', params: { id } })
}
</script>

<template>
    <section class="schedule">
        <p class="hint">진행 중 계약을 <strong>청소 요일</strong>별로 모았습니다. 계약 수정에서 요일을 지정하면 여기에 나타납니다. 오늘 요일은 강조됩니다.</p>

        <p v-if="isLoading" class="state">불러오는 중…</p>
        <p v-else-if="isError" class="state state--err">불러오지 못했습니다.</p>

        <div v-else class="board">
            <div
                v-for="d in days"
                :key="d.weekday"
                class="day"
                :class="{ today: d.weekday === TODAY }"
            >
                <div class="day-head">
                    <span class="day-label">{{ d.label }}</span>
                    <span class="day-count">{{ d.count }}</span>
                </div>
                <ul v-if="d.items.length" class="items">
                    <li v-for="it in d.items" :key="it.contractId" class="item" @click="goContract(it.contractId)">
                        <div class="item-name">{{ it.clientName || it.title }}</div>
                        <div v-if="it.address" class="item-sub">{{ it.address }}</div>
                        <div class="item-tags">
                            <span v-if="it.doorCode" class="tag tag--door">🔑 {{ it.doorCode }}</span>
                            <span v-if="it.cleaningCycleLabel && it.cleaningCycleLabel !== '매주'" class="tag tag--cycle">
                                {{ it.cleaningCycleLabel }}
                            </span>
                        </div>
                    </li>
                </ul>
                <p v-else class="empty">-</p>
            </div>
        </div>
    </section>
</template>

<style scoped>
.hint { font-size: 0.82rem; color: var(--text); margin: 0 0 1rem; }
.board {
    display: grid;
    grid-template-columns: repeat(7, minmax(150px, 1fr));
    gap: 0.6rem;
    overflow-x: auto;
}
@media (max-width: 900px) {
    .board { grid-template-columns: repeat(3, minmax(160px, 1fr)); }
}
@media (max-width: 560px) {
    .board { grid-template-columns: 1fr 1fr; }
}
.day {
    background: #fff;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    display: flex;
    flex-direction: column;
    min-height: 120px;
}
.day.today {
    border-color: var(--primary);
    box-shadow: 0 0 0 2px var(--primary-soft);
}
.day-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.5rem 0.7rem;
    border-bottom: 1px solid var(--border);
    background: var(--muted);
    border-radius: var(--radius) var(--radius) 0 0;
}
.day.today .day-head { background: var(--primary-soft); }
.day-label { font-weight: 700; color: var(--text-h); }
.day-count {
    font-size: 0.75rem;
    background: var(--primary);
    color: var(--primary-fg);
    border-radius: 999px;
    padding: 0.05rem 0.5rem;
}
.items { list-style: none; margin: 0; padding: 0.4rem; display: flex; flex-direction: column; gap: 0.4rem; }
.item {
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 0.5rem 0.6rem;
    cursor: pointer;
}
.item:hover { border-color: var(--primary); background: var(--muted); }
.item-name { font-weight: 600; color: var(--text-h); font-size: 0.9rem; }
.item-sub { font-size: 0.75rem; color: var(--text); margin-top: 0.15rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-tags { display: flex; flex-wrap: wrap; gap: 0.25rem; margin-top: 0.3rem; }
.tag { font-size: 0.7rem; padding: 0.05rem 0.4rem; border-radius: 999px; }
.tag--door { background: #fef3c7; color: #92400e; }
.tag--cycle { background: var(--primary-soft); color: var(--primary-hover); }
.empty { text-align: center; color: var(--text); padding: 1rem 0; margin: 0; }
.state { text-align: center; padding: 2rem 0; color: var(--text); }
.state--err { color: var(--danger); }
</style>
