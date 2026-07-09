<script setup>
// 목록 페이지네이션 공용 컴포넌트.
// 서버 PageResponse(page: 1-based, totalPages, totalElements)를 그대로 받아 렌더한다.
// 페이지 변경은 update:page 이벤트로 부모에 알린다(v-model:page 사용 가능).
import { computed } from 'vue'

const props = defineProps({
    page: { type: Number, required: true },        // 현재 페이지(1-based)
    totalPages: { type: Number, default: 0 },      // 전체 페이지 수
    totalElements: { type: Number, default: 0 },   // 전체 건수
    // 현재 페이지 좌우로 보여줄 번호 개수
    around: { type: Number, default: 2 },
})

const emit = defineEmits(['update:page'])

// 현재 페이지 주변 번호 목록(1..totalPages 를 around 범위로 자른다)
const pages = computed(() => {
    const total = props.totalPages
    if (total <= 1) {
        return []
    }
    const start = Math.max(1, props.page - props.around)
    const end = Math.min(total, props.page + props.around)
    const list = []
    for (let i = start; i <= end; i += 1) {
        list.push(i)
    }
    return list
})

const canPrev = computed(() => props.page > 1)
const canNext = computed(() => props.page < props.totalPages)

function go(target) {
    if (target < 1 || target > props.totalPages || target === props.page) {
        return
    }
    emit('update:page', target)
}
</script>

<template>
    <nav v-if="totalPages > 1" class="pager" aria-label="페이지 이동">
        <button class="pager__btn" type="button" :disabled="!canPrev" @click="go(1)">처음</button>
        <button class="pager__btn" type="button" :disabled="!canPrev" @click="go(page - 1)">이전</button>

        <button
            v-for="p in pages"
            :key="p"
            class="pager__num"
            :class="{ 'is-current': p === page }"
            type="button"
            @click="go(p)"
        >
            {{ p }}
        </button>

        <button class="pager__btn" type="button" :disabled="!canNext" @click="go(page + 1)">다음</button>
        <button class="pager__btn" type="button" :disabled="!canNext" @click="go(totalPages)">끝</button>
    </nav>
</template>

<style scoped>
.pager {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 0.3rem;
    padding: 1rem;
    flex-wrap: wrap;
    border-top: 1px solid var(--border);
}

.pager__btn,
.pager__num {
    min-width: 2.1rem;
    padding: 0.35rem 0.6rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-h);
    cursor: pointer;
    font: inherit;
    font-size: 0.85rem;
}

.pager__btn:hover:not(:disabled),
.pager__num:hover:not(.is-current) {
    border-color: var(--primary);
    color: var(--primary);
}

.pager__btn:disabled {
    opacity: 0.45;
    cursor: default;
}

.pager__num.is-current {
    background: var(--primary);
    border-color: var(--primary);
    color: var(--primary-fg);
    font-weight: 700;
    cursor: default;
}
</style>
