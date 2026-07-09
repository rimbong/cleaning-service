<script setup>
import { RouterView } from 'vue-router'

import NotifyHost from '@/common/components/notify/NotifyHost.vue'
</script>

<template>
    <!-- 레이아웃(대민/관리자)은 각 라우트가 감싸므로 여기서는 화면만 렌더한다 -->
    <RouterView />

    <!-- 전역 알림 표시 컴포넌트(알럿/컨펌/토스트/스피너) — 앱에서 유일하게 여기 한 번만 둔다 -->
    <NotifyHost />
</template>
