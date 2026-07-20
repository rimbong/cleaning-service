<script setup>
// 청소 약품 pH별 사용 가이드 — 재고 화면에서 바로 볼 수 있는 참고 자료.
//
// 내용은 일반적인 청소 화학 원리를 정리한 것이다. 실제 사용 시에는 제품마다
// 성분·농도가 다르므로 라벨과 주의사항(MSDS)이 우선한다.
import { useRouter } from 'vue-router'

const router = useRouter()

const PH_SCALE = [
    { label: '강산성', range: 'pH 0~2', cls: 's1' },
    { label: '산성', range: 'pH 3~6', cls: 's2' },
    { label: '중성', range: 'pH 6~8', cls: 's3' },
    { label: '알칼리성', range: 'pH 8~11', cls: 's4' },
    { label: '강알칼리', range: 'pH 12~14', cls: 's5' },
]

const CATEGORIES = [
    {
        key: 'acid',
        label: '산성',
        range: 'pH 0~6',
        soil: '알칼리성 오염 — 물때(스케일), 요석·소변 자국, 녹, 시멘트·백화, 워터스팟',
        where: '변기 안쪽, 욕실 타일·유리, 세면대·수전, 주전자',
        how: '도포 후 2~10분 방치(굳은 물때는 더 길게) → 문질러 닦기 → 물로 충분히 헹굼',
        avoid: '대리석·천연석(부식), 콘크리트, 법랑, 금속(알루미늄·철)',
        warn: '락스와 절대 혼합 금지 — 염소가스 발생. 장갑·환기 필수.',
    },
    {
        key: 'neutral',
        label: '중성',
        range: 'pH 6~8',
        soil: '가벼운 일반 오염 — 먼지, 가벼운 기름때, 손때, 일상 얼룩',
        where: '원목·마루, 대리석, 도장면, 가전 표면, 식기, 예민한 바닥재',
        how: '물에 희석 → 닦기 → 필요 시 물걸레 마무리. 방치 불필요.',
        avoid: '없음 — 재질 손상 위험이 가장 낮다',
        warn: '찌든 기름때·굳은 물때·곰팡이에는 세정력이 부족하다. 그때는 알칼리/산/표백제로 바꾼다.',
    },
    {
        key: 'alkali',
        label: '알칼리성',
        range: 'pH 8~14',
        soil: '산성·기름성 오염 — 기름때, 찌든 때, 음식물, 손기름, 그을음',
        where: '가스레인지·후드·오븐, 주방 벽·바닥, 작업장 유분 바닥, 배수구',
        how: '도포 후 수 분 방치해 기름을 분해시킨 뒤 닦기 → 충분히 헹굼',
        avoid: '알루미늄·아연(부식·변색), 천연석 일부, 도장·왁스면, 목재',
        warn: '강알칼리는 화상 위험 — 장갑·보안경 필수. 피부에 닿아 미끈거리면 위험 신호이니 즉시 세척. 암모니아 제품은 락스와 혼합 금지.',
    },
    {
        key: 'oxidizer',
        label: '표백·산화계',
        range: '종류마다 다름',
        soil: '색소·곰팡이·세균 — 곰팡이, 얼룩 색소, 냄새, 살균',
        where: '욕실 실리콘·줄눈 곰팡이, 배수구 살균, 흰 빨래, 도마 소독',
        how: '염소계는 물에 희석(고농도 금지). 곰팡이엔 젤·키친타월 도포 후 방치 → 헹굼. 산소계는 40~50도 온수에서 효과가 오른다.',
        avoid: '색 있는 섬유(탈색), 금속(부식), 대리석',
        warn: '락스는 산성세제·식초·암모니아·알코올과 절대 혼합 금지. 항상 단독으로, 찬물에, 환기하며.',
    },
    {
        key: 'enzyme',
        label: '효소계',
        range: '약알칼리~중성',
        soil: '단백질·전분·유지 — 혈액, 음식물, 배수구 유기물, 반려동물 얼룩',
        where: '세탁, 배수구, 주방 하수구, 반려동물 오염',
        how: '미지근한 물(30~40도)에 쓰고 시간을 두고 반응시켜야 효과가 난다',
        avoid: '뜨거운 물, 강한 산/알칼리와 병용(효소가 파괴된다)',
        warn: '순하고 재질 부담이 적으며 냄새의 원인인 유기물 자체를 없앤다.',
    },
]

const SOIL_TO_PH = [
    { soil: '욕실·주전자 하얀 물때', ph: '산성', cls: 'acid', why: '미네랄(알칼리성)을 산이 녹인다' },
    { soil: '변기 안쪽 누런 요석', ph: '산성', cls: 'acid', why: '소변석·물때는 산성 변기세정제' },
    { soil: '녹·쇠붙이 얼룩', ph: '산성', cls: 'acid', why: '옥살산·인산 계열 녹 제거제' },
    { soil: '가스레인지·후드 기름때', ph: '알칼리성', cls: 'alkali', why: '기름을 비누화해 분해' },
    { soil: '주방 벽 찌든 손기름', ph: '알칼리성', cls: 'alkali', why: '유분·손때에 강하다' },
    { soil: '막힌 하수구(기름·머리카락)', ph: '알칼리 / 효소', cls: 'alkali', why: '강알칼리로 즉시, 또는 효소로 서서히' },
    { soil: '욕실 실리콘 검은 곰팡이', ph: '표백(염소계)', cls: 'oxidizer', why: '곰팡이 살균·색소 제거' },
    { soil: '흰 빨래 얼룩·행주 살균', ph: '표백', cls: 'oxidizer', why: '흰옷은 염소/산소계, 색옷은 산소계' },
    { soil: '혈액·음식물·반려동물 얼룩', ph: '효소', cls: 'enzyme', why: '단백질·유기물을 표적 분해' },
    { soil: '원목·대리석·자동차 도장', ph: '중성', cls: 'neutral', why: '산/알칼리는 재질을 상하게 한다' },
    { soil: '일상 먼지·가벼운 얼룩', ph: '중성', cls: 'neutral', why: '다목적 중성 클리너로 충분' },
]

const NEVER_MIX = [
    { mix: '락스(염소계) + 산성세제 / 식초 / 구연산', gas: '염소가스', risk: '호흡기 손상, 소량으로도 중독·질식' },
    { mix: '락스 + 암모니아(유리세정제 등)', gas: '클로라민 가스', risk: '호흡곤란·폐 손상' },
    { mix: '락스 + 알코올', gas: '클로로포름 등', risk: '독성·마취성 증기' },
    { mix: '과산화수소 + 식초', gas: '과초산', risk: '부식성·자극성 증기' },
    { mix: '서로 다른 배수구 세정제', gas: '예측 불가 반응', risk: '발열·분출·가스' },
]

const SAFETY = [
    ['희석', '원액을 그대로 쓰지 않는다. 진하다고 더 잘 지워지지 않으며 재질·건강에 해롭다.'],
    ['보호구', '고무장갑 기본. 강산·강알칼리·분무 작업엔 보안경·마스크.'],
    ['환기', '창문·환풍기 가동. 밀폐된 욕실에서 락스를 오래 쓰지 않는다.'],
    ['테스트', '처음 쓰는 표면은 눈에 안 띄는 구석에 먼저 시험한다(특히 천연석·목재·도장면).'],
    ['방치시간', '도포 후 몇 분 두면 문지르는 힘이 크게 줄어든다. 단, 너무 오래 두면 부식 위험.'],
    ['보관', '원래 용기에 라벨을 유지해 보관. 어린이·반려동물 손이 닿지 않는 곳에.'],
]
</script>

<template>
    <section class="guide">
        <div class="toolbar">
            <p class="lead">
                세제 선택의 대부분은 원칙 하나로 끝납니다.
                <b>오염의 성질과 반대되는 pH를 써야 잘 지워집니다.</b>
            </p>
            <button class="btn" @click="router.push({ name: 'admin-supplies' })">재고로 돌아가기</button>
        </div>

        <div class="scale">
            <div v-for="s in PH_SCALE" :key="s.label" class="scale__cell" :class="s.cls">
                <div>{{ s.label }}</div>
                <div class="scale__range">{{ s.range }}</div>
            </div>
        </div>
        <div class="scale__legend">
            <span>물때·요석·녹 제거</span>
            <span>기름·단백질·때 제거</span>
        </div>

        <div class="note">
            <b>기억법:</b> 물때는 산으로, 기름때는 알칼리로, 예민한 건 중성으로, 곰팡이·색은 표백제로, 유기물은 효소로.
            물때(하얀 미네랄)와 기름때(끈적한 유분)가 청소의 양대 오염이고, 이 둘은 서로 반대 pH로 지웁니다.
        </div>

        <h3>성질별 상세</h3>
        <div class="cards">
            <div v-for="c in CATEGORIES" :key="c.key" class="cat" :class="'cat--' + c.key">
                <div class="cat__head">
                    <span class="ph" :class="'ph--' + c.key">{{ c.label }}</span>
                    <span class="cat__range">{{ c.range }}</span>
                </div>
                <dl>
                    <dt>잘 지우는 오염</dt><dd>{{ c.soil }}</dd>
                    <dt>쓰는 곳</dt><dd>{{ c.where }}</dd>
                    <dt>사용법</dt><dd>{{ c.how }}</dd>
                    <dt>피해야 할 재질</dt><dd>{{ c.avoid }}</dd>
                </dl>
                <p class="cat__warn">{{ c.warn }}</p>
            </div>
        </div>

        <h3>이 오염엔 뭘 쓰지? (역방향 조견표)</h3>
        <div class="table-wrap">
            <table class="table">
                <thead>
                    <tr><th>오염 / 상황</th><th>추천</th><th>이유</th></tr>
                </thead>
                <tbody>
                    <tr v-for="s in SOIL_TO_PH" :key="s.soil">
                        <td>{{ s.soil }}</td>
                        <td><span class="ph" :class="'ph--' + s.cls">{{ s.ph }}</span></td>
                        <td class="muted">{{ s.why }}</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <h3 class="danger-title">절대 섞으면 안 되는 조합</h3>
        <p class="lead">청소 사고의 대부분은 "더 강하게" 하려고 섞다가 납니다. 아래는 생명에 위험할 수 있습니다.</p>
        <div class="table-wrap table-wrap--danger">
            <table class="table">
                <thead>
                    <tr><th>섞는 조합</th><th>생성 물질</th><th>위험</th></tr>
                </thead>
                <tbody>
                    <tr v-for="m in NEVER_MIX" :key="m.mix">
                        <td>{{ m.mix }}</td>
                        <td><b>{{ m.gas }}</b></td>
                        <td>{{ m.risk }}</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="note note--danger">
            <b>철칙:</b> 세제는 한 번에 한 종류만. 다른 세제로 바꿀 땐 반드시 물로 완전히 헹군 뒤 사용.
            특히 락스는 언제나 단독으로, 찬물에, 환기하며 씁니다.
        </div>

        <h3>공통 안전 수칙</h3>
        <div class="table-wrap">
            <table class="table">
                <tbody>
                    <tr v-for="row in SAFETY" :key="row[0]">
                        <td class="k">{{ row[0] }}</td>
                        <td>{{ row[1] }}</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <p class="footer">
            일반적인 청소 화학 원리를 정리한 참고 자료입니다.
            실제 사용 시에는 제품마다 성분·농도가 다르므로 각 제품의 사용설명서와 주의사항(MSDS)을 따르세요.
        </p>
    </section>
</template>

<style scoped>
.guide { max-width: 900px; margin: 0 auto; }
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1.2rem; flex-wrap: wrap; }
.lead { margin: 0; font-size: 0.9rem; color: var(--text); line-height: 1.6; flex: 1; min-width: 260px; }
.btn { padding: 0.5rem 0.9rem; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; color: var(--text-h); cursor: pointer; font: inherit; white-space: nowrap; }
.btn:hover { border-color: var(--primary); color: var(--primary); }
h3 { font-size: 1.02rem; margin: 2rem 0 0.6rem; color: var(--text-h); }
.danger-title { color: #8f1d1d; }
.scale { display: flex; border-radius: 10px; overflow: hidden; border: 1px solid var(--border); font-size: 0.76rem; text-align: center; color: #fff; font-weight: 700; }
.scale__cell { flex: 1; padding: 0.55rem 0.25rem; }
.scale__range { font-weight: 500; opacity: 0.9; font-size: 0.72rem; }
.s1 { background: #d64828; } .s2 { background: #e8743b; } .s3 { background: #4a9d6a; }
.s4 { background: #3b7de8; } .s5 { background: #254a8f; }
.scale__legend { display: flex; justify-content: space-between; font-size: 0.73rem; color: var(--text); margin: 0.35rem 0 0; }
.note { background: #fff8e6; border: 1px solid #f0e0a8; border-radius: var(--radius); padding: 0.85rem 1rem; font-size: 0.83rem; color: #6b5900; line-height: 1.6; margin-top: 1rem; }
.note--danger { background: #fdeaea; border-color: #f3c4c4; color: #7a2323; }
.cards { display: grid; gap: 0.9rem; }
.cat { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); padding: 1rem 1.1rem; border-top: 4px solid var(--border); }
.cat--acid { border-top-color: #e8743b; }
.cat--neutral { border-top-color: #4a9d6a; }
.cat--alkali { border-top-color: #3b7de8; }
.cat--oxidizer { border-top-color: #8b5cf6; }
.cat--enzyme { border-top-color: #0d9488; }
.cat__head { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.7rem; }
.cat__range { font-size: 0.78rem; color: var(--text); }
.cat dl { display: grid; grid-template-columns: 7.2rem 1fr; gap: 0.35rem 0.8rem; margin: 0; font-size: 0.83rem; }
.cat dt { font-weight: 700; color: var(--text-h); }
.cat dd { margin: 0; color: var(--text); line-height: 1.55; }
.cat__warn { margin: 0.8rem 0 0; padding: 0.5rem 0.7rem; background: #fdeaea; border-radius: 8px; font-size: 0.8rem; color: #8f1d1d; line-height: 1.5; }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow-x: auto; }
.table-wrap--danger { border-color: #f3c4c4; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 0.6rem 0.9rem; text-align: left; border-bottom: 1px solid var(--border); font-size: 0.85rem; vertical-align: top; }
.table th { background: var(--muted); font-size: 0.79rem; color: var(--text); white-space: nowrap; }
.table-wrap--danger .table th { background: #f7d9d9; color: #8f1d1d; }
.table tbody tr:last-child td { border-bottom: none; }
.table td.k { font-weight: 700; color: var(--text-h); white-space: nowrap; }
.muted { color: var(--text); }
.ph { display: inline-block; padding: 0.13rem 0.5rem; border-radius: 999px; font-size: 0.75rem; font-weight: 600; white-space: nowrap; }
.ph--acid { background: #fdf1ea; color: #d15a26; }
.ph--neutral { background: #eef7f1; color: #2f7d51; }
.ph--alkali { background: #eaf1fd; color: #2b62c4; }
.ph--oxidizer { background: #f2ecfd; color: #6d43d1; }
.ph--enzyme { background: #e7f6f4; color: #0b6f68; }
.footer { margin-top: 2rem; padding-top: 1rem; border-top: 1px solid var(--border); font-size: 0.78rem; color: var(--text); line-height: 1.6; }
</style>
