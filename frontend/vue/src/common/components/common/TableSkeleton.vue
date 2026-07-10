<script setup>
// 목록 로딩 중 표시하는 스켈레톤(뼈대) — 표 형태의 자리표시자를 은은한 shimmer 로 보여준다.
// 텍스트 "불러오는 중…" 대신 실제 목록과 비슷한 레이아웃을 먼저 그려 체감 로딩을 줄인다.
defineProps({
    /** 표시할 행 수 */
    rows: { type: Number, default: 5 },
})
</script>

<template>
    <div class="skeleton-wrap" aria-busy="true" aria-live="polite">
        <div v-for="n in rows" :key="n" class="skeleton-row">
            <span class="skeleton-bar skeleton-bar--lg"></span>
            <span class="skeleton-bar skeleton-bar--md"></span>
            <span class="skeleton-bar skeleton-bar--sm"></span>
        </div>
    </div>
</template>

<style scoped>
.skeleton-wrap {
    background: #fff;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    overflow: hidden;
}

.skeleton-row {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 0.95rem 1rem;
    border-bottom: 1px solid var(--border);
}

.skeleton-row:last-child {
    border-bottom: none;
}

.skeleton-bar {
    height: 0.85rem;
    border-radius: 6px;
    background: linear-gradient(90deg, var(--muted-2) 25%, var(--muted) 37%, var(--muted-2) 63%);
    background-size: 400% 100%;
    animation: skeleton-shimmer 1.4s ease infinite;
}

.skeleton-bar--lg {
    flex: 2;
}

.skeleton-bar--md {
    flex: 1.2;
}

.skeleton-bar--sm {
    flex: 0.6;
}

@keyframes skeleton-shimmer {
    0% {
        background-position: 100% 50%;
    }
    100% {
        background-position: 0 50%;
    }
}

/* 접근성: 모션 최소화 선호 시 애니메이션 정지 */
@media (prefers-reduced-motion: reduce) {
    .skeleton-bar {
        animation: none;
    }
}
</style>
