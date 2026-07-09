<script setup>
// 전역 알림 UI 를 실제로 "그리는" 컴포넌트. App.vue 에 한 번만 둔다.
// 어느 화면이든 useNotifyStore() 의 alert()/confirm()/toast()/bar()/spinner 를 호출하면
// 상태가 바뀌고, 그 상태를 여기서 구독해 모달/토스트/바/스피너를 표시한다.
//   화면(호출)  ──>  notify store(상태)  ──>  NotifyHost(표시)
import { useI18n } from 'vue-i18n'

import { useNotifyStore } from '@/stores/common/notify/notify'

const { t } = useI18n()

// 스토어 인스턴스. state/actions 를 notify.xxx 로 그대로 쓴다.
// 주의: const { alertState } = notify 처럼 구조분해하면 반응성이 깨질 수 있어(원시값 복사)
//       템플릿에서는 notify.alertState.visible 처럼 스토어를 통째로 참조한다.
const notify = useNotifyStore()

/** 토스트 type 별 아이콘 문자(레거시는 PNG 아이콘 — 자산 의존 없애려 문자로 대체) */
const TOAST_ICONS = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
}

function iconFor(type) {
    return TOAST_ICONS[type] || TOAST_ICONS.info
}
</script>

<template>
    <!-- Teleport: 이 마크업을 <body> 바로 아래로 이동시켜 렌더링(Vue3 내장 기능).
         부모의 overflow/z-index 에 갇히지 않아 오버레이 UI 에 필수적인 패턴이다. -->
    <Teleport to="body">
        <!-- ─────────── Alert 모달 ─────────── -->
        <div v-if="notify.alertState.visible" class="nf-overlay">
            <div class="nf-modal" role="alertdialog">
                <p class="nf-modal__text">{{ notify.alertState.text }}</p>
                <div class="nf-modal__actions">
                    <button class="nf-btn nf-btn--primary" type="button" @click="notify.closeAlert()">
                        {{ t('common.ok') }}
                    </button>
                </div>
            </div>
        </div>

        <!-- ─────────── Confirm 모달 ─────────── -->
        <div v-if="notify.confirmState.visible" class="nf-overlay">
            <div class="nf-modal" role="dialog">
                <p class="nf-modal__text">{{ notify.confirmState.text }}</p>
                <div class="nf-modal__actions">
                    <button class="nf-btn" type="button" @click="notify.answerConfirm(false)">
                        {{ t('common.cancel') }}
                    </button>
                    <button class="nf-btn nf-btn--primary" type="button" @click="notify.answerConfirm(true)">
                        {{ t('common.ok') }}
                    </button>
                </div>
            </div>
        </div>

        <!-- ─────────── Alert Bar (상단 부착형 배너) ───────────
             화면 최상단에 붙어 높이가 열리며 나타나고(0→50px), 시간이 지나면 닫힌다. -->
        <Transition name="nf-bar">
            <div
                v-if="notify.barState.visible"
                class="nf-bar"
                :class="'nf-bar--' + notify.barState.color"
                role="status"
            >
                <span class="nf-bar__icon">✓</span>
                <span class="nf-bar__msg">{{ notify.barState.text }}</span>
                <button class="nf-bar__close" type="button" @click="notify.closeBar()">✕</button>
            </div>
        </Transition>

        <!-- ─────────── Toast (상단 중앙, 위에서 내려옴) ───────────
             TransitionGroup: 목록 항목이 추가/제거될 때 CSS 트랜지션 적용(Vue3 내장).
             위(-100px)에서 내려와 나타나고, 사라질 때 다시 위로 올라간다(레거시 동작 재현).
             ※ v-if 를 걸지 않는 이유: 마지막 토스트 제거 시 컨테이너째 언마운트되면
               퇴장 애니메이션이 생략된다. 빈 컨테이너는 pointer-events:none 이라 무해. -->
        <TransitionGroup name="nf-toast" tag="div" class="nf-toasts">
            <div
                v-for="item in notify.toasts"
                :key="item.id"
                class="nf-toast"
                @click="notify.removeToast(item.id)"
            >
                <span class="nf-toast__icon" :class="'nf-toast__icon--' + item.type">{{ iconFor(item.type) }}</span>
                <span class="nf-toast__text">{{ item.text }}</span>
            </div>
        </TransitionGroup>

        <!-- ─────────── Spinner ─────────── -->
        <div v-if="notify.isSpinnerVisible" class="nf-overlay nf-overlay--spinner">
            <div class="nf-spinner" aria-label="loading"></div>
        </div>

        <!-- ─────────── Progress Bar (진행률 %) ─────────── -->
        <div v-if="notify.progressState.visible" class="nf-overlay">
            <div class="nf-progress">
                <p class="nf-progress__title">{{ notify.progressState.title }}</p>
                <div class="nf-progress__track">
                    <div class="nf-progress__fill" :style="{ width: notify.progressState.percent + '%' }"></div>
                </div>
                <p class="nf-progress__percent">{{ notify.progressState.percent }}%</p>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
/* 오버레이(모달/스피너 공용 배경) */
.nf-overlay {
    position: fixed;
    inset: 0;
    display: grid;
    place-items: center;
    background: rgba(15, 23, 42, 0.45);
    z-index: 1000;
}

.nf-overlay--spinner {
    background: rgba(255, 255, 255, 0.5);
    z-index: 1100;
}

/* 모달 */
.nf-modal {
    min-width: 300px;
    max-width: 90vw;
    padding: 1.5rem;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.nf-modal__text {
    margin: 0 0 1.25rem;
    white-space: pre-wrap;
    color: var(--text-h);
}

.nf-modal__actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
}

.nf-btn {
    padding: 0.45rem 1.1rem;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: #fff;
    cursor: pointer;
}

.nf-btn--primary {
    background: var(--text-h);
    border-color: var(--text-h);
    color: #fff;
}

/* ─────────── Alert Bar (레거시 alert-pop-wrap 스타일 이관) ─────────── */
.nf-bar {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 50px;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    padding: 14px 20px;
    overflow: hidden;
    box-shadow: 0 2px 5px 5px rgba(0, 0, 0, 0.05);
    z-index: 1300;
}

.nf-bar--red {
    background: rgba(255, 44, 48, 0.95);
}

.nf-bar--green {
    background: rgba(51, 190, 98, 0.95);
}

.nf-bar--yellow {
    background: #ffca2c;
}

.nf-bar--blue {
    background: #3dd5f3;
}

.nf-bar__icon {
    color: #fff;
    margin-right: 6px;
    font-size: 13px;
}

.nf-bar__msg {
    color: #fff;
    font-size: 13px;
    font-weight: 300;
    cursor: default;
}

.nf-bar__close {
    margin-left: auto;
    border: 0;
    background: transparent;
    color: #fff;
    cursor: pointer;
    transition: opacity 0.25s ease;
}

.nf-bar__close:hover {
    opacity: 0.7;
}

/* 바 등장/퇴장: 높이가 열리고 닫히는 레거시 모션(0.7s cubic-bezier) 재현 */
.nf-bar-enter-active,
.nf-bar-leave-active {
    transition:
        height 0.7s cubic-bezier(1, 0.09, 0.18, 0.66),
        padding 0.7s cubic-bezier(1, 0.09, 0.18, 0.66),
        opacity 0.7s cubic-bezier(1, 0.09, 0.18, 0.66);
}

.nf-bar-enter-from,
.nf-bar-leave-to {
    height: 0;
    padding-top: 0;
    padding-bottom: 0;
    opacity: 0;
}

/* ─────────── Toast (레거시 toastMessage 스타일 이관: 상단 중앙, 폭 90%) ─────────── */
.nf-toasts {
    position: fixed;
    top: 80px;
    left: 5%;
    width: 90%;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    z-index: 1200;
    /* 컨테이너가 클릭을 막지 않게(토스트 자체만 클릭 대상) */
    pointer-events: none;
}

.nf-toast {
    display: flex;
    align-items: center;
    padding: 12px 20px;
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.85);
    box-shadow: 0 0 7px 0 rgba(0, 0, 0, 0.25);
    color: #000;
    font-size: 14px;
    font-weight: 100;
    cursor: pointer;
    pointer-events: auto; /* 클릭해서 닫기 가능 */
}

.nf-toast__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    margin-right: 8px;
    border-radius: 50%;
    color: #fff;
    font-size: 12px;
    flex-shrink: 0;
}

.nf-toast__icon--success {
    background: #33be62;
}

.nf-toast__icon--error {
    background: #ff2c30;
}

.nf-toast__icon--info {
    background: #3dd5f3;
}

/* 토스트 등장/퇴장: 위(-100px)에서 내려오고, 사라질 때 다시 위로(레거시 top:-100px→80px 재현) */
.nf-toast-enter-from,
.nf-toast-leave-to {
    opacity: 0;
    transform: translateY(-100px);
}

.nf-toast-enter-active,
.nf-toast-leave-active {
    transition: all 0.3s ease;
}

/* TransitionGroup 에서 항목 제거로 나머지가 자리 이동할 때도 부드럽게 */
.nf-toast-move {
    transition: transform 0.3s ease;
}

/* 스피너 */
.nf-spinner {
    width: 44px;
    height: 44px;
    border: 4px solid var(--border);
    border-top-color: var(--text-h);
    border-radius: 50%;
    animation: nf-spin 0.8s linear infinite;
}

@keyframes nf-spin {
    to {
        transform: rotate(360deg);
    }
}

/* 프로그레스 바 */
.nf-progress {
    min-width: 320px;
    max-width: 90vw;
    padding: 1.5rem;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.nf-progress__title {
    margin: 0 0 0.75rem;
    color: var(--text-h);
    font-weight: 600;
}

.nf-progress__track {
    height: 10px;
    border-radius: 999px;
    background: var(--border);
    overflow: hidden;
}

.nf-progress__fill {
    height: 100%;
    border-radius: 999px;
    background: var(--text-h);
    transition: width 0.15s ease;
}

.nf-progress__percent {
    margin: 0.5rem 0 0;
    text-align: right;
    font-size: 0.85rem;
    color: var(--text);
}
</style>
