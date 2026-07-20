<script setup>
// 청소 용역 견적 산정 가이드 — 견적 화면에서 바로 여는 시세 참고 자료.
//
// 계단·공용부 정기청소는 여기 범위만 적어두고, 실제 금액 산출은 권장가 계산기가 한다
// (단가가 DB 에 있어 최저임금이 오르면 그쪽만 고치면 되기 때문). 중복 관리하지 않는다.
import { useRouter } from 'vue-router'

const router = useRouter()

const COST_STRUCTURE = [
    ['인건비', '작업 인원 x 인당 일당(또는 투입 시간). 견적의 핵심.', '50~70%'],
    ['자재비', '세제·소모품(수세미·장갑·비닐 등)', '5~10%'],
    ['장비·교통비', '고압세척기·스팀기 감가, 차량·유류·주차·이동시간', '5~15%'],
    ['관리비(간접비)', '보험·4대보험·사무·A/S 대비', '10~15%'],
    ['이윤(마진)', '업체 순이익', '15~30%'],
]

const MOVE_IN = [
    { size: '24평', seoulNew: '24만', seoulOld: '26만', incheon: '22~24만' },
    { size: '34평', seoulNew: '34만', seoulOld: '37만', incheon: '31~34만' },
    { size: '43평', seoulNew: '43만', seoulOld: '47만', incheon: '40~44만' },
    { size: '52평', seoulNew: '52만', seoulOld: '57만', incheon: '48~53만' },
]

const OFFICE = [
    ['50평 미만', '20~30만원', '150~200평', '40~70만원'],
    ['50~80평', '25~40만원', '200~250평', '45~80만원'],
    ['80~120평', '30~50만원', '250~300평', '50~100만원'],
    ['120~150평', '35~60만원', '1회성 대청소', '평당 8천~1.2만원'],
]

const FACTORS = [
    ['면적·규모', '넓은 평수, 고층, 다세대 많음', '소형·저층'],
    ['오염도', '구축·장기 방치·곰팡이·기름 찌든때', '신축·관리 잘 된 건물'],
    ['난이도', '복층·높은 천장·유리 많음·붙박이장', '단순 구조, 가구 없음'],
    ['주기·계약', '단발성(1회)', '장기 정기계약 → 회당 단가 내려감'],
    ['부가작업', '왁스·광택·고압세척·소독·유리외벽', '기본 청소만'],
    ['지역·접근성', '도심, 주차난, 원거리', '접근 쉬움'],
]

const CHECKLIST = [
    ['면적(평)·층수', '단가의 기준. 입주는 평당, 정기는 층수·세대수로 산정'],
    ['건물 상태·연식', '구축·심한 오염은 평당 10~20% 또는 인원·시간 추가'],
    ['투입 인원 x 시간', '인건비가 견적의 핵심. "몇 명이 몇 시간" 을 먼저 확정'],
    ['설비 개수', '좌변기·소변기·창호·베란다 수 → 정기청소 단가에 직접 반영'],
    ['부가작업 여부', '왁스·고압·소독·유리는 별도 항목으로 분리 견적'],
    ['주기·계약기간', '장기 정기계약이면 회당 단가 할인 제시(수주에 유리)'],
    ['접근성·주차·이동', '원거리·주차난은 교통비/시간에 반영'],
    ['기본 포함 / 할증 명시', '견적서에 포함 범위와 추가요금을 분리 → 현장 추가금 분쟁 예방'],
]
</script>

<template>
    <section class="guide">
        <div class="toolbar">
            <p class="lead">
                지역·건물 상태·업체 규모에 따라 편차가 큰 <b>시장 평균 범위</b>입니다.
                실제 견적은 현장 확인 후 산정하고, 이 자료는 기준선을 잡는 데 쓰세요.
            </p>
            <button class="btn" @click="router.push({ name: 'admin-quotes' })">견적으로 돌아가기</button>
        </div>

        <div class="note note--link">
            <b>계단·공용부 정기청소</b>는 이 화면에 금액을 적어두지 않습니다.
            층수·세대수로 계산하는 <b>권장가 계산기</b>가 견적 등록 화면에 붙어 있고,
            단가는 <RouterLink :to="{ name: 'admin-pricing-policy' }">단가 정책</RouterLink> 에서 바꿉니다.
            두 곳에 금액을 적어두면 한쪽만 고치는 일이 생깁니다.
        </div>

        <h3>1. 모든 견적의 뼈대 — 원가 구조</h3>
        <div class="formula">
            견적가 = 인건비 + 자재비 + 장비·교통비 + 관리비 + 이윤
        </div>
        <div class="table-wrap">
            <table class="table">
                <thead><tr><th>항목</th><th>내용</th><th class="r">대략 비중</th></tr></thead>
                <tbody>
                    <tr v-for="row in COST_STRUCTURE" :key="row[0]">
                        <td class="k">{{ row[0] }}</td><td>{{ row[1] }}</td><td class="r">{{ row[2] }}</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="tip">
            <b>실무 감각:</b> "몇 명이 며칠 붙는 일인가" 를 먼저 잡으면 견적의 70%는 끝납니다.
        </div>

        <h3>2. 입주·이사청소 — 평당 단가 방식</h3>
        <div class="formula">
            입주청소비 = 평수 x 평당 단가 + 할증(복층·심한오염·베란다/붙박이장 추가)
        </div>
        <div class="kpi">
            <div><div class="kpi__big">약 1만원/평</div><div class="kpi__lbl">신축 평당 단가</div></div>
            <div><div class="kpi__big">약 1.1만원/평</div><div class="kpi__lbl">구축 평당 단가(오염 많음)</div></div>
            <div><div class="kpi__big">+10~20%</div><div class="kpi__lbl">심한 오염·곰팡이 전처리</div></div>
        </div>
        <div class="table-wrap">
            <table class="table">
                <thead><tr><th>평형</th><th class="r">서울 신축</th><th class="r">서울 구축</th><th class="r">인천</th></tr></thead>
                <tbody>
                    <tr v-for="m in MOVE_IN" :key="m.size">
                        <td class="k">{{ m.size }}</td>
                        <td class="r">{{ m.seoulNew }}원</td>
                        <td class="r">{{ m.seoulOld }}원</td>
                        <td class="r">{{ m.incheon }}원</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="note">
            <b>할증이 붙는 경우:</b> 베란다 3개 이상, 붙박이장 2세트 이상, 복층·층고 3m 이상,
            구축의 심한 찌든때·곰팡이, 리모델링 먼지, 유리·새시 과다.
            견적서에 <b>기본 포함 범위</b>와 <b>할증 항목</b>을 반드시 나눠 적으세요(현장 분쟁 예방).
        </div>

        <h3>3. 상가·사무실 정기청소 (주 1회 기준)</h3>
        <div class="table-wrap">
            <table class="table">
                <thead><tr><th>면적</th><th class="r">월 비용</th><th>면적</th><th class="r">월 비용</th></tr></thead>
                <tbody>
                    <tr v-for="row in OFFICE" :key="row[0]">
                        <td class="k">{{ row[0] }}</td><td class="r">{{ row[1] }}</td>
                        <td class="k">{{ row[2] }}</td><td class="r">{{ row[3] }}</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="note">
            <b>주기 환산:</b> 주 2회는 주 1회의 약 150~170% 입니다(단순 2배가 아님 — 한 번 갈 때 몰아 하는 효율).
            매일(주 5~6회)은 사실상 상주 인력이라 1인 월 급여 + 4대보험 + 관리비로 다시 산정합니다.
            1회성 대청소는 소형이라도 출동 최소금액(25~30만원)을 두는 것이 일반적입니다.
        </div>

        <h3>4. 가격을 올리고 내리는 변수</h3>
        <div class="table-wrap">
            <table class="table">
                <thead><tr><th>구분</th><th>올리는 요인</th><th>낮추는 요인</th></tr></thead>
                <tbody>
                    <tr v-for="row in FACTORS" :key="row[0]">
                        <td class="k">{{ row[0] }}</td><td>{{ row[1] }}</td><td class="muted">{{ row[2] }}</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <h3>5. 현장 견적 체크리스트</h3>
        <div class="table-wrap">
            <table class="table">
                <thead><tr><th>확인 항목</th><th>왜 중요한가 / 어떻게 반영</th></tr></thead>
                <tbody>
                    <tr v-for="row in CHECKLIST" :key="row[0]">
                        <td class="k">{{ row[0] }}</td><td>{{ row[1] }}</td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="tip">
            <b>수주 팁:</b> 경쟁 견적에서는 명확한 포함 범위, 장기계약 할인, 사진 기반 상태 진단을 함께 제시하면
            단가 방어에 유리합니다. 최저가 경쟁보다 "이 금액에 이만큼 한다" 를 문서로 보여주는 쪽이 재계약률이 높습니다.
        </div>

        <p class="footer">
            국내 청소 플랫폼·업체가 공개한 단가를 종합한 참고용 시세입니다.
            지역·업체·건물 상태에 따라 편차가 크므로 실제 계약은 현장 확인 후 산정하세요.
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
.formula { background: #0f172a; color: #e2e8f0; border-radius: 12px; padding: 0.9rem 1.1rem; font-size: 0.88rem; margin: 0.6rem 0; }
.note { background: #fff8e6; border: 1px solid #f0e0a8; border-radius: var(--radius); padding: 0.85rem 1rem; font-size: 0.83rem; color: #6b5900; line-height: 1.65; margin: 0.9rem 0; }
.note--link { background: #eaf1fd; border-color: #cfe0fb; color: #1e40af; }
.note--link a { color: #1e40af; font-weight: 700; }
.tip { background: #e7f6ee; border: 1px solid #b7e0c8; border-radius: var(--radius); padding: 0.85rem 1rem; font-size: 0.83rem; color: #155e37; line-height: 1.65; margin: 0.9rem 0; }
.kpi { display: flex; gap: 0.7rem; flex-wrap: wrap; margin: 0.8rem 0; }
.kpi > div { flex: 1; min-width: 140px; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); padding: 0.8rem 0.9rem; border-top: 3px solid var(--primary); }
.kpi__big { font-size: 1.15rem; font-weight: 800; color: var(--primary); letter-spacing: -0.4px; }
.kpi__lbl { font-size: 0.76rem; color: var(--text); margin-top: 0.1rem; }
.table-wrap { background: #fff; border: 1px solid var(--border); border-radius: var(--radius); box-shadow: var(--shadow); overflow-x: auto; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 0.6rem 0.9rem; text-align: left; border-bottom: 1px solid var(--border); font-size: 0.85rem; vertical-align: top; }
.table th { background: var(--muted); font-size: 0.79rem; color: var(--text); white-space: nowrap; }
.table tbody tr:last-child td { border-bottom: none; }
.table td.k { font-weight: 700; color: var(--text-h); white-space: nowrap; }
.table .r { text-align: right; white-space: nowrap; font-variant-numeric: tabular-nums; }
.muted { color: var(--text); }
.footer { margin-top: 2rem; padding-top: 1rem; border-top: 1px solid var(--border); font-size: 0.78rem; color: var(--text); line-height: 1.6; }
</style>
