/**
 * 적정가 재산정 화면의 캐시 무효화.
 *
 * 재산정 결과는 여러 화면의 값에서 파생된다.
 *   - 계약    : 월정액·청소 주기·요일·월 방문 횟수
 *   - 거래처  : 건물 규모(층수·세대수) — 검토 대상에 들어가는지까지 좌우한다
 *   - 단가 정책: 단가와 계수 — 모든 권장가가 한꺼번에 바뀐다
 *
 * 그래서 이 값들을 저장하는 쪽에서 재산정 캐시를 같이 지워야 하는데,
 * queryKey 가 서로 달라 그냥 두면 저장해도 옛 금액이 그대로 보인다.
 * 어디서 불러야 하는지 잊지 않도록 이름을 붙여 한 곳에 모아 둔다.
 *
 * @param {import('@tanstack/vue-query').QueryClient} queryClient vue-query 클라이언트
 */
export function invalidatePricingReview(queryClient) {
    queryClient.invalidateQueries({ queryKey: ['pricing-review'] })
}
