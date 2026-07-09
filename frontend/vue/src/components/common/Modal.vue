<script setup>
// 공용 모달(다이얼로그) 컴포넌트.
// 표시/숨김은 부모가 v-if 로 감싸서 제어한다(마운트=열림, 언마운트=닫힘).
// 배경(바깥) 클릭으로 닫는 동작은 closeOnBackdrop 프로퍼티로 on/off 한다(기본 off).
//   <Modal :close-on-backdrop="false" @close="..."> ... </Modal>
// Teleport 로 <body> 아래에 렌더해 부모의 overflow/z-index 에 갇히지 않게 한다.
import { onBeforeUnmount, onMounted } from 'vue'

const props = defineProps({
    /** 헤더 제목(없으면 header 슬롯 사용) */
    title: { type: String, default: '' },
    /** 배경(바깥) 클릭 시 닫기 on/off — 기본 off(실수로 닫힘 방지) */
    closeOnBackdrop: { type: Boolean, default: false },
    /** ESC 키로 닫기 on/off */
    closeOnEsc: { type: Boolean, default: true },
    /** 우상단 X 닫기 버튼 표시 여부 */
    closable: { type: Boolean, default: true },
    /** 본문 최대 폭(CSS width 값) */
    width: { type: String, default: 'min(90vw, 460px)' },
})

const emit = defineEmits(['close'])

/** 배경 클릭 — closeOnBackdrop 가 켜져 있을 때만 닫는다 */
function onBackdrop() {
    if (props.closeOnBackdrop) {
        emit('close')
    }
}

/** ESC 키 — closeOnEsc 가 켜져 있을 때만 닫는다 */
function onKeydown(e) {
    if (props.closeOnEsc && e.key === 'Escape') {
        emit('close')
    }
}

onMounted(() => {
    document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
    document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
    <Teleport to="body">
        <div class="dlg-overlay" @click.self="onBackdrop">
            <div class="dlg" role="dialog" aria-modal="true" :style="{ width }">
                <div v-if="title || $slots.header" class="dlg__header">
                    <slot name="header">
                        <h3 class="dlg__title">{{ title }}</h3>
                    </slot>
                    <button
                        v-if="closable"
                        class="dlg__x"
                        type="button"
                        aria-label="닫기"
                        @click="emit('close')"
                    >
                        ✕
                    </button>
                </div>

                <div class="dlg__body">
                    <slot />
                </div>

                <div v-if="$slots.actions" class="dlg__actions">
                    <slot name="actions" />
                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.dlg-overlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.45);
    display: grid;
    place-items: center;
    z-index: 900;
    padding: 1rem;
}

.dlg {
    background: #fff;
    border-radius: 12px;
    padding: 1.5rem;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
    max-height: 90vh;
    overflow-y: auto;
}

.dlg__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1rem;
}

.dlg__title {
    margin: 0;
    font-size: 1.05rem;
    color: var(--text-h);
}

.dlg__x {
    border: 0;
    background: transparent;
    color: var(--text);
    cursor: pointer;
    font-size: 1rem;
    line-height: 1;
    padding: 0.15rem;
}

.dlg__x:hover {
    color: var(--text-h);
}

.dlg__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    margin-top: 1rem;
}
</style>
