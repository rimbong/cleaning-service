/**
 * 약품 재고와 얽힌 캐시 무효화.
 *
 * 재고가 바뀌면 서로 다른 queryKey 를 쓰는 여러 화면이 한꺼번에 옛 값이 된다.
 *   supplies       : 재고 현황 목록
 *   supply         : 품목 단건(이력 화면 상단 요약)
 *   supply-history : 입출고 이력
 *   supply-hazards : 위험 조합 경고 — 재고가 0 이 되거나 pH 구분을 바꾸면 판정이 달라진다
 *
 * 네 곳에 같은 코드를 흩어 두면 하나를 빠뜨리게 되므로(실제로 그래서 이력이 갱신되지 않는
 * 문제가 있었다) 이름을 붙여 한 곳에 모은다. 특히 hazards 는 안전 정보라 놓치면 안 된다.
 *
 * @param {import('@tanstack/vue-query').QueryClient} queryClient vue-query 클라이언트
 */
export function invalidateSupplyCaches(queryClient) {
    queryClient.invalidateQueries({ queryKey: ['supplies'] })
    queryClient.invalidateQueries({ queryKey: ['supply'] })
    queryClient.invalidateQueries({ queryKey: ['supply-history'] })
    queryClient.invalidateQueries({ queryKey: ['supply-hazards'] })
}
