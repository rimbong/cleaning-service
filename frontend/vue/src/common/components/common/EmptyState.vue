<script setup>
// 빈 목록/결과 없음 안내 — 아이콘 + 메시지 + (선택) 액션 버튼 슬롯.
// 목록마다 제각각이던 "등록된 ~ 없습니다" 표시를 하나의 형태로 통일한다.
defineProps({
    /** 상단 아이콘(이모지) */
    icon: { type: String, default: '📭' },
    /** 안내 메시지 */
    message: { type: String, default: '데이터가 없습니다.' },
})
</script>

<template>
    <div class="empty-state">
        <div class="empty-state__icon">{{ icon }}</div>
        <p class="empty-state__msg">{{ message }}</p>
        <div v-if="$slots.default" class="empty-state__action">
            <slot />
        </div>
    </div>
</template>

<style scoped>
.empty-state {
    padding: 3rem 1rem;
    text-align: center;
    color: var(--text);
}

.empty-state__icon {
    font-size: 2.4rem;
    margin-bottom: 0.6rem;
    opacity: 0.75;
}

.empty-state__msg {
    margin: 0 0 1rem;
}
</style>
