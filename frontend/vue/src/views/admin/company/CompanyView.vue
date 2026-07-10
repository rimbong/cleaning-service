<script setup>
// 회사(공급자) 프로필 설정 — 세금계산서 발행 주체 정보. 단일 설정(조회 후 수정).
import { reactive, ref, onMounted } from 'vue'

import { companyService } from '@/services/admin/company/companyService'
import { useNotifyStore } from '@/stores/common/notify/notify'

const notify = useNotifyStore()
const loading = ref(true)
const saving = ref(false)

// 도장(인장) 이미지
const hasStamp = ref(false)
const stampUrl = ref('')
const stampInput = ref(null)
const stampBusy = ref(false)

const form = reactive({
    businessNumber: '',
    companyName: '',
    ownerName: '',
    address: '',
    businessType: '',
    businessItem: '',
    phone: '',
})

onMounted(async () => {
    try {
        const c = (await companyService.get()).data.data
        form.businessNumber = c.businessNumber ?? ''
        form.companyName = c.companyName ?? ''
        form.ownerName = c.ownerName ?? ''
        form.address = c.address ?? ''
        form.businessType = c.businessType ?? ''
        form.businessItem = c.businessItem ?? ''
        form.phone = c.phone ?? ''
        hasStamp.value = !!c.hasStamp
        if (hasStamp.value) {
            loadStampPreview()
        }
    } catch (e) {
        notify.bar('회사 정보를 불러오지 못했습니다.', { color: 'red' })
    } finally {
        loading.value = false
    }
})

/** 도장 미리보기(blob → object URL) 로드 */
async function loadStampPreview() {
    try {
        const blob = (await companyService.getStampBlob()).data
        if (stampUrl.value) {
            URL.revokeObjectURL(stampUrl.value)
        }
        stampUrl.value = URL.createObjectURL(blob)
    } catch (e) {
        stampUrl.value = ''
    }
}

/** 도장 파일 선택 버튼 → 숨긴 file input 클릭 */
function triggerStampSelect() {
    stampInput.value?.click()
}

/** 도장 이미지 업로드(선택 즉시) */
async function onStampSelected(e) {
    const file = e.target.files && e.target.files[0]
    if (!file) {
        return
    }
    stampBusy.value = true
    try {
        const res = await companyService.uploadStamp(file)
        hasStamp.value = !!res.data.data.hasStamp
        await loadStampPreview()
        notify.toast('도장이 등록되었습니다.', { type: 'success' })
    } catch (err) {
        notify.bar(err.response?.data?.message ?? '도장 등록에 실패했습니다.', { color: 'red' })
    } finally {
        stampBusy.value = false
        if (stampInput.value) {
            stampInput.value.value = '' // 같은 파일 재선택 가능하도록 초기화
        }
    }
}

/** 도장 이미지 삭제 */
async function onRemoveStamp() {
    if (!(await notify.confirm('등록된 도장을 삭제하시겠습니까?'))) {
        return
    }
    stampBusy.value = true
    try {
        await companyService.removeStamp()
        hasStamp.value = false
        if (stampUrl.value) {
            URL.revokeObjectURL(stampUrl.value)
            stampUrl.value = ''
        }
        notify.toast('도장이 삭제되었습니다.', { type: 'info' })
    } catch (err) {
        notify.bar(err.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    } finally {
        stampBusy.value = false
    }
}

async function onSubmit() {
    saving.value = true
    try {
        await companyService.update({
            businessNumber: form.businessNumber.trim() || null,
            companyName: form.companyName.trim() || null,
            ownerName: form.ownerName.trim() || null,
            address: form.address.trim() || null,
            businessType: form.businessType.trim() || null,
            businessItem: form.businessItem.trim() || null,
            phone: form.phone.trim() || null,
        })
        notify.toast('저장되었습니다.', { type: 'success' })
    } catch (e) {
        notify.bar(e.response?.data?.message ?? '저장에 실패했습니다.', { color: 'red' })
    } finally {
        saving.value = false
    }
}
</script>

<template>
    <section class="form-page">
        <p v-if="loading" class="state">불러오는 중…</p>
        <form v-else class="card" @submit.prevent="onSubmit">
            <p class="hint">세금계산서 발행 시 "공급자"란에 사용되는 우리 회사 정보입니다.</p>

            <div class="row">
                <div class="field">
                    <label>상호</label>
                    <input v-model="form.companyName" maxlength="100" placeholder="회사 상호" />
                </div>
                <div class="field">
                    <label>등록번호(사업자번호)</label>
                    <input v-model="form.businessNumber" maxlength="20" placeholder="000-00-00000" />
                </div>
            </div>

            <div class="row">
                <div class="field">
                    <label>대표자명</label>
                    <input v-model="form.ownerName" maxlength="50" />
                </div>
                <div class="field">
                    <label>연락처</label>
                    <input v-model="form.phone" maxlength="30" />
                </div>
            </div>

            <div class="field">
                <label>사업장 주소</label>
                <input v-model="form.address" maxlength="255" />
            </div>

            <div class="row">
                <div class="field">
                    <label>업태</label>
                    <input v-model="form.businessType" maxlength="50" placeholder="예: 서비스업" />
                </div>
                <div class="field">
                    <label>종목</label>
                    <input v-model="form.businessItem" maxlength="50" placeholder="예: 건물청소" />
                </div>
            </div>

            <div class="actions">
                <button class="btn btn--primary" type="submit" :disabled="saving">
                    {{ saving ? '저장 중…' : '저장' }}
                </button>
            </div>
        </form>

        <!-- 도장(인장) -->
        <div v-if="!loading" class="card stamp-card">
            <p class="hint">세금계산서 양식에 찍을 <strong>회사 도장(인장)</strong> 이미지입니다. 배경이 투명한 PNG 를 권장합니다. (양식 다운로드 시 "도장 포함"을 선택하면 찍힙니다)</p>
            <div class="stamp-row">
                <div class="stamp-preview">
                    <img v-if="stampUrl" :src="stampUrl" alt="도장 미리보기" />
                    <span v-else class="stamp-empty">등록된 도장 없음</span>
                </div>
                <div class="stamp-actions">
                    <input
                        ref="stampInput"
                        type="file"
                        accept="image/png,image/jpeg,image/gif"
                        class="stamp-file"
                        @change="onStampSelected"
                    />
                    <button type="button" class="btn btn--primary" :disabled="stampBusy" @click="triggerStampSelect">
                        {{ hasStamp ? '도장 변경' : '도장 등록' }}
                    </button>
                    <button v-if="hasStamp" type="button" class="btn btn--danger" :disabled="stampBusy" @click="onRemoveStamp">
                        삭제
                    </button>
                </div>
            </div>
        </div>
    </section>
</template>

<style scoped>
.form-page {
    max-width: 680px;
    margin: 0 auto;
}

.card {
    background: #fff;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.1rem;
}

.hint {
    color: var(--text);
    font-size: 0.85rem;
    margin: 0;
}

.row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.1rem;
}

@media (max-width: 560px) {
    .row {
        grid-template-columns: 1fr;
    }
}

.field {
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
}

.field label {
    font-size: 0.85rem;
    color: var(--text);
    font-weight: 600;
}

.field input {
    padding: 0.55rem 0.7rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    font: inherit;
    color: var(--text-h);
    background: #fff;
}

.field input:focus {
    outline: none;
    border-color: var(--primary);
    box-shadow: 0 0 0 3px var(--primary-soft);
}

.actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 0.5rem;
}

.btn {
    padding: 0.55rem 1.2rem;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    background: #fff;
    color: var(--text-h);
    cursor: pointer;
    font: inherit;
}

.btn--primary {
    background: var(--primary);
    border-color: var(--primary);
    color: var(--primary-fg);
}

.btn--primary:hover:not(:disabled) {
    background: var(--primary-hover);
}

.btn--primary:disabled {
    opacity: 0.6;
    cursor: default;
}

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}

/* 도장 카드 */
.stamp-card {
    margin-top: 1.25rem;
}

.stamp-row {
    display: flex;
    align-items: center;
    gap: 1.25rem;
    flex-wrap: wrap;
}

.stamp-preview {
    width: 120px;
    height: 120px;
    border: 1px dashed var(--border);
    border-radius: var(--radius);
    display: grid;
    place-items: center;
    background: var(--muted);
    overflow: hidden;
    flex-shrink: 0;
}

.stamp-preview img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
}

.stamp-empty {
    font-size: 0.8rem;
    color: var(--text);
}

.stamp-actions {
    display: flex;
    gap: 0.5rem;
    align-items: center;
}

.stamp-file {
    display: none;
}

.btn--danger {
    color: var(--danger);
}

.btn--danger:hover:not(:disabled) {
    border-color: var(--danger);
    background: var(--danger-soft);
}
</style>
