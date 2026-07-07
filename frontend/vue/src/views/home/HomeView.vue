<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
    <section class="home">
        <h2>{{ t('home.heading') }}</h2>
        <p class="desc">{{ t('home.desc') }}</p>
    </section>
</template>

<style scoped>
.home {
    max-width: 720px;
}

.desc {
    color: var(--text);
    margin: 0.25rem 0 1rem;
}
</style>
