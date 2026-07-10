<script setup>
// 거래처 선택 필드 — 버튼을 누르면 모달이 열리고, 거기서 건물명으로 검색해 고른다.
// 드롭다운은 거래처가 많으면 찾기 어려워, 검색 가능한 모달로 대체한다.
// v-model 로 clientId(선택 없음이면 null 또는 '')를 주고받는다.
import { computed, ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'

import Modal from '@/common/components/common/Modal.vue'
import { clientService } from '@/services/admin/client/clientService'

const props = defineProps({
    /** 선택된 거래처 id (없으면 null/'') */
    modelValue: { type: [Number, String], default: null },
    /** 버튼에 표시할 안내 문구(미선택 시) */
    placeholder: { type: String, default: '거래처 선택' },
    /** 거래처 미연결(신규 고객) 허용 — 견적 폼처럼 비워둘 수 있는 경우 true */
    allowEmpty: { type: Boolean, default: false },
    /** 검증 실패 강조(빨간 테두리) */
    invalid: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

// 전용 옵션 API(페이징 없이 전량) — 거래처가 많아도 누락 없이 검색 가능
const { data } = useQuery({
    queryKey: ['client-options'],
    queryFn: () => clientService.options().then((res) => res.data.data),
    staleTime: 30_000,
})
const options = computed(() => data.value ?? [])

/** 선택된 거래처의 건물명 — 버튼 표시용 */
const selectedName = computed(() => {
    if (props.modelValue == null || props.modelValue === '') {
        return ''
    }
    const found = options.value.find((c) => c.id === Number(props.modelValue))
    return found ? found.name : ''
})

const open = ref(false)
const keyword = ref('')

/** 검색어로 필터링한 거래처 목록 */
const filtered = computed(() => {
    const k = keyword.value.trim().toLowerCase()
    if (!k) {
        return options.value
    }
    return options.value.filter((c) => c.name.toLowerCase().includes(k))
})

function openPicker() {
    keyword.value = ''
    open.value = true
}

function pick(id) {
    emit('update:modelValue', id)
    open.value = false
}

/** 거래처 미연결로 비우기(allowEmpty 인 경우만 노출) */
function pickNone() {
    emit('update:modelValue', '')
    open.value = false
}
</script>

<template>
    <div class="picker">
        <button
            type="button"
            class="picker-btn"
            :class="{ 'is-empty': !selectedName, 'is-invalid': invalid }"
            @click="openPicker"
        >
            <span class="picker-label">{{ selectedName || placeholder }}</span>
            <span class="picker-caret">▾</span>
        </button>

        <Modal v-if="open" title="거래처 선택" width="min(92vw, 520px)" @close="open = false">
            <div class="pick-search">
                <input v-model="keyword" type="text" placeholder="건물명으로 검색" />
            </div>
            <ul class="pick-list">
                <li v-if="allowEmpty" class="pick-item pick-item--none" @click="pickNone">
                    거래처 미연결(신규 고객)
                </li>
                <li
                    v-for="c in filtered"
                    :key="c.id"
                    class="pick-item"
                    :class="{ 'is-selected': Number(modelValue) === c.id }"
                    @click="pick(c.id)"
                >
                    {{ c.name }}
                </li>
                <li v-if="!filtered.length" class="pick-empty">검색 결과가 없습니다.</li>
            </ul>
        </Modal>
    </div>
</template>

<style scoped>
.picker-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
    padding: 0.55rem 0.7rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-h);
    cursor: pointer;
    font: inherit;
    text-align: left;
}

.picker-btn:hover {
    border-color: var(--primary);
}

.picker-btn.is-empty .picker-label {
    color: var(--text);
}

.picker-btn.is-invalid {
    border-color: var(--danger);
}

.picker-caret {
    color: var(--text);
    font-size: 0.8rem;
}

.pick-search input {
    width: 100%;
    padding: 0.55rem 0.7rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    font: inherit;
}

.pick-search input:focus {
    outline: none;
    border-color: var(--primary);
    box-shadow: 0 0 0 3px var(--primary-soft);
}

.pick-list {
    list-style: none;
    margin: 0.75rem 0 0;
    padding: 0;
    max-height: 45vh;
    overflow-y: auto;
    border: 1px solid var(--border);
    border-radius: var(--radius);
}

.pick-item {
    padding: 0.6rem 0.75rem;
    border-bottom: 1px solid var(--border);
    cursor: pointer;
}

.pick-item:last-child {
    border-bottom: none;
}

.pick-item:hover {
    background: var(--muted);
}

.pick-item.is-selected {
    background: var(--primary-soft);
    color: var(--primary-hover);
    font-weight: 600;
}

.pick-item--none {
    color: var(--text);
    font-style: italic;
}

.pick-empty {
    padding: 1.25rem 0.75rem;
    text-align: center;
    color: var(--text);
}
</style>
