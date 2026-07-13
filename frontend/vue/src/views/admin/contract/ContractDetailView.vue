<script setup>
// 계약 상세 — 정보 표시 + 수정/삭제/목록 진입 + 계약서 첨부 파일 관리.
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

import { contractService, WEEKDAYS } from '@/services/admin/contract/contractService'
import { useNotifyStore } from '@/stores/common/notify/notify'

/** 요일 코드 배열 → "월·수·금" 라벨. 비면 '-'. */
function weekdayLabels(codes) {
    if (!Array.isArray(codes) || !codes.length) {
        return '-'
    }
    return WEEKDAYS.filter((d) => codes.includes(d.value)).map((d) => d.label).join('·')
}

const props = defineProps({
    id: { type: [String, Number], required: true },
})

const router = useRouter()
const notify = useNotifyStore()
const queryClient = useQueryClient()

// 단건도 캐시(목록에서 넘어오면 즉시 표시 가능)
const { data, isLoading, isError } = useQuery({
    queryKey: ['contract', computed(() => String(props.id))],
    queryFn: () => contractService.get(props.id).then((res) => res.data.data),
    staleTime: 30_000,
})

const contract = computed(() => data.value)

/**
 * 목록으로 이동 — 뒤로가기 히스토리가 있으면 그대로 돌아가(목록에서 보던 페이지 유지),
 * 딥링크로 바로 상세에 들어온 경우엔 목록 route 로 이동한다.
 */
function goList() {
    if (window.history.state?.back) {
        router.back()
    } else {
        router.push({ name: 'admin-contracts' })
    }
}

const removeMutation = useMutation({
    mutationFn: () => contractService.remove(props.id),
    onSuccess: () => {
        notify.toast('삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['contracts'] })
        router.replace({ name: 'admin-contracts' })
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    },
})

async function onDelete() {
    if (!(await notify.confirm(`'${contract.value.title}' 계약을 삭제하시겠습니까?`))) {
        return
    }
    removeMutation.mutate()
}

function fmt(v) {
    return v ? String(v) : '-'
}

function fmtDate(v) {
    return v ? String(v).slice(0, 10) : '-'
}

function fmtDateTime(v) {
    return v ? String(v).slice(0, 16).replace('T', ' ') : '-'
}

function fmtMoney(v) {
    return v != null ? Number(v).toLocaleString('ko-KR') + '원' : '-'
}

function fmtSize(bytes) {
    if (bytes == null) {
        return '-'
    }
    if (bytes < 1024) {
        return bytes + ' B'
    }
    if (bytes < 1024 * 1024) {
        return (bytes / 1024).toFixed(1) + ' KB'
    }
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// ===== 계약서(HWP) 양식 =====
const withStamp = ref(false) // 양식 다운로드 시 회사 도장 포함 여부
const downloadingDoc = ref(false)

/** 빈 양식에 계약/거래처/회사정보를 채운 계약서 HWP 를 내려받는다. */
async function onDownloadDocument() {
    downloadingDoc.value = true
    try {
        const fallbackName = `계약서_${contract.value?.clientName ?? props.id}.hwp`
        await contractService.downloadDocument(props.id, fallbackName, withStamp.value)
    } catch (e) {
        notify.bar(e.response?.data?.message ?? '계약서를 만들지 못했습니다.', { color: 'red' })
    } finally {
        downloadingDoc.value = false
    }
}

// ===== 계약서 첨부 파일 =====
const fileInput = ref(null)
const uploading = ref(false)
const attachmentsKey = computed(() => ['contract-attachments', String(props.id)])

const { data: attachmentData } = useQuery({
    queryKey: attachmentsKey,
    queryFn: () => contractService.listAttachments(props.id).then((res) => res.data.data),
    staleTime: 30_000,
})
const attachments = computed(() => attachmentData.value ?? [])

const uploadMutation = useMutation({
    mutationFn: (file) => contractService.uploadAttachment(props.id, file),
    onSuccess: () => {
        notify.toast('파일이 첨부되었습니다.', { type: 'success' })
        queryClient.invalidateQueries({ queryKey: ['contract-attachments', String(props.id)] })
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '첨부에 실패했습니다.', { color: 'red' })
    },
})

const removeAttachmentMutation = useMutation({
    mutationFn: (attachmentId) => contractService.removeAttachment(props.id, attachmentId),
    onSuccess: () => {
        notify.toast('첨부가 삭제되었습니다.', { type: 'info' })
        queryClient.invalidateQueries({ queryKey: ['contract-attachments', String(props.id)] })
    },
    onError: (e) => {
        notify.bar(e.response?.data?.message ?? '삭제에 실패했습니다.', { color: 'red' })
    },
})

function onPickFile() {
    fileInput.value?.click()
}

async function onFileChange(e) {
    const file = e.target.files?.[0]
    if (!file) {
        return
    }
    uploading.value = true
    try {
        await uploadMutation.mutateAsync(file)
    } finally {
        uploading.value = false
        e.target.value = '' // 같은 파일 재선택 가능하도록 초기화
    }
}

async function onDownload(a) {
    try {
        await contractService.downloadAttachment(props.id, a.id, a.originalFilename)
    } catch (err) {
        notify.bar('다운로드에 실패했습니다.', { color: 'red' })
    }
}

async function onRemoveAttachment(a) {
    if (!(await notify.confirm(`'${a.originalFilename}' 첨부를 삭제하시겠습니까?`))) {
        return
    }
    removeAttachmentMutation.mutate(a.id)
}
</script>

<template>
    <section class="detail-page">
        <p v-if="isLoading" class="state">불러오는 중…</p>
        <p v-else-if="isError" class="state state--err">계약을 불러오지 못했습니다.</p>

        <template v-else-if="contract">
            <div class="detail-head">
                <button class="btn btn--ghost" type="button" @click="goList">
                    ← 목록
                </button>
                <div class="detail-head__actions">
                    <RouterLink class="btn" :to="{ name: 'admin-contract-edit', params: { id: contract.id } }">수정</RouterLink>
                    <button class="btn btn--danger" type="button" @click="onDelete">삭제</button>
                </div>
            </div>

            <div class="card">
                <div class="card-head">
                    <h2 class="card-title">{{ contract.title }}</h2>
                    <span v-if="contract.status" class="tag" :class="'tag--' + contract.status.toLowerCase()">
                        {{ contract.statusLabel }}
                    </span>
                </div>
                <dl class="info">
                    <div class="info-row">
                        <dt>거래처</dt>
                        <dd>
                            <RouterLink
                                v-if="contract.clientId"
                                class="link"
                                :to="{ name: 'admin-client-detail', params: { id: contract.clientId } }"
                            >
                                {{ contract.clientName }}
                            </RouterLink>
                            <template v-else>-</template>
                        </dd>
                    </div>
                    <div class="info-row">
                        <dt>월 청구금액</dt>
                        <dd>{{ fmtMoney(contract.monthlyFee) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>청구일</dt>
                        <dd>{{ contract.billingDay ? '매월 ' + contract.billingDay + '일' : '-' }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>계약 시작일</dt>
                        <dd>{{ fmtDate(contract.startDate) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>계약 종료일</dt>
                        <dd>{{ contract.endDate ? fmtDate(contract.endDate) : '무기한' }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>계약서 보관 위치</dt>
                        <dd>{{ fmt(contract.documentLocation) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>수금 방법</dt>
                        <dd>{{ fmt(contract.paymentMethod) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>출입문 비번</dt>
                        <dd>{{ fmt(contract.doorCode) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>청소 요일</dt>
                        <dd>{{ weekdayLabels(contract.cleaningWeekdays) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>청소 주기</dt>
                        <dd>{{ fmt(contract.cleaningCycleLabel) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>부가세 기준</dt>
                        <dd>{{ fmt(contract.vatTypeLabel) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>초도청소비</dt>
                        <dd>{{ contract.initialFee != null ? fmtMoney(contract.initialFee) : '-' }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>청소 범위</dt>
                        <dd>{{ fmt(contract.cleaningScope) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>기본 서비스 항목</dt>
                        <dd>{{ fmt(contract.serviceItems) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>메모</dt>
                        <dd class="info-multi">{{ fmt(contract.memo) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>등록일</dt>
                        <dd>{{ fmtDateTime(contract.createdAt) }}</dd>
                    </div>
                    <div class="info-row">
                        <dt>수정일</dt>
                        <dd>{{ fmtDateTime(contract.updatedAt) }}</dd>
                    </div>
                </dl>
            </div>

            <!-- 계약서(HWP) 양식 출력 -->
            <div class="card doc-card">
                <div class="doc-head">
                    <h3 class="doc-title">계약서 양식</h3>
                    <label class="stamp-toggle">
                        <input v-model="withStamp" type="checkbox" />
                        도장 포함
                    </label>
                    <button
                        class="btn btn--primary btn--sm"
                        type="button"
                        :disabled="downloadingDoc"
                        @click="onDownloadDocument"
                    >
                        {{ downloadingDoc ? '만드는 중…' : '계약서(HWP) 다운로드' }}
                    </button>
                </div>
                <p class="doc-hint">
                    계단청소 용역 계약서 양식에 이 계약의 내용과 회사정보를 채워 내려받습니다.
                    도장은 회사정보에 등록된 이미지를 사용합니다.
                </p>
            </div>

            <!-- 계약서 첨부 파일 -->
            <div class="card attach-card">
                <div class="attach-head">
                    <h3 class="attach-title">계약서 파일 <span class="count">{{ attachments.length }}</span></h3>
                    <button class="btn btn--primary btn--sm" type="button" :disabled="uploading" @click="onPickFile">
                        {{ uploading ? '업로드 중…' : '+ 파일 첨부' }}
                    </button>
                    <input
                        ref="fileInput"
                        type="file"
                        class="file-hidden"
                        accept=".pdf,.png,.jpg,.jpeg,.gif,.webp,.hwp,.hwpx,.doc,.docx,.xls,.xlsx"
                        @change="onFileChange"
                    />
                </div>

                <ul v-if="attachments.length" class="attach-list">
                    <li v-for="a in attachments" :key="a.id" class="attach-item">
                        <button class="attach-item__name" type="button" @click="onDownload(a)" :title="'다운로드: ' + a.originalFilename">
                            <span class="attach-item__icon">📄</span>
                            <span class="attach-item__filename">{{ a.originalFilename }}</span>
                        </button>
                        <span class="attach-item__size">{{ fmtSize(a.fileSize) }}</span>
                        <button class="btn btn--sm btn--danger" type="button" @click="onRemoveAttachment(a)">삭제</button>
                    </li>
                </ul>
                <p v-else class="attach-empty">첨부된 계약서 파일이 없습니다. (PDF·이미지·문서, 최대 20MB)</p>
            </div>
        </template>
    </section>
</template>

<style scoped>
.detail-page {
    max-width: 680px;
    margin: 0 auto;
}

.detail-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
}

.detail-head__actions {
    display: flex;
    gap: 0.5rem;
}

/* 버튼은 전역 .btn 계열(style.css) 사용 */

.card {
    background: #fff;
    border: 1px solid var(--border);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    padding: 1.5rem;
}

.card-head {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin: 0 0 1.25rem;
}

.card-title {
    margin: 0;
    font-size: 1.25rem;
}

.info {
    margin: 0;
    display: flex;
    flex-direction: column;
}

.info-row {
    display: grid;
    grid-template-columns: 120px 1fr;
    gap: 1rem;
    padding: 0.7rem 0;
    border-bottom: 1px solid var(--border);
}

.info-row:last-child {
    border-bottom: none;
}

.info-row dt {
    color: var(--text);
    font-size: 0.88rem;
    font-weight: 600;
}

.info-row dd {
    margin: 0;
    color: var(--text-h);
}

.info-multi {
    white-space: pre-wrap;
}

.link {
    color: var(--primary);
    font-weight: 600;
    text-decoration: none;
}

.link:hover {
    text-decoration: underline;
}

.tag {
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 999px;
    font-size: 0.78rem;
    font-weight: 600;
}

.tag--active {
    background: var(--primary-soft);
    color: var(--primary-hover);
}

.tag--ended {
    background: #e5e7eb;
    color: #4b5563;
}

.tag--suspended {
    background: #fef3c7;
    color: #92400e;
}

/* 계약서 첨부 파일 섹션 */
/* 계약서(HWP) 양식 출력 섹션 */
.doc-card {
    margin-top: 1.25rem;
}

.doc-head {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}

.doc-title {
    margin: 0;
    font-size: 1.05rem;
    flex: 1;
}

.stamp-toggle {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.85rem;
    color: var(--text);
    cursor: pointer;
}

.doc-hint {
    margin: 0.75rem 0 0;
    font-size: 0.85rem;
    color: var(--text-muted, var(--text));
    line-height: 1.5;
}

.attach-card {
    margin-top: 1.25rem;
}

.attach-head {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 1rem;
}

.attach-title {
    margin: 0;
    font-size: 1.05rem;
    flex: 1;
}

.attach-title .count {
    color: var(--primary);
    font-weight: 700;
    margin-left: 0.25rem;
}

.btn--primary {
    background: var(--primary);
    border-color: var(--primary);
    color: var(--primary-fg);
}

.btn--primary:hover:not(:disabled) {
    background: var(--primary-hover);
    color: var(--primary-fg);
}

.btn--primary:disabled {
    opacity: 0.6;
    cursor: default;
}

.btn--sm {
    padding: 0.3rem 0.7rem;
    font-size: 0.82rem;
}

.file-hidden {
    display: none;
}

.attach-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
}

.attach-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.6rem 0.25rem;
    border-bottom: 1px solid var(--border);
}

.attach-item:last-child {
    border-bottom: none;
}

.attach-item__name {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 0.4rem;
    background: none;
    border: none;
    padding: 0;
    cursor: pointer;
    color: var(--primary);
    font: inherit;
    text-align: left;
}

.attach-item__name:hover .attach-item__filename {
    text-decoration: underline;
}

.attach-item__filename {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-weight: 600;
}

.attach-item__size {
    color: var(--text);
    font-size: 0.82rem;
    white-space: nowrap;
}

.attach-empty {
    color: var(--text);
    text-align: center;
    padding: 1.5rem 0;
    margin: 0;
}

.state {
    color: var(--text);
    padding: 2rem 0;
    text-align: center;
}

.state--err {
    color: var(--danger);
}
</style>
