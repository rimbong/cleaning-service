import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * 목록의 페이지 번호를 URL 쿼리(?page=N)와 동기화하는 컴포저블.
 *
 * 상세 화면으로 갔다가 브라우저 뒤로가기 또는 "목록" 버튼으로 돌아와도
 * 보던 페이지가 그대로 복원된다. 목록 컴포넌트가 재마운트되더라도 초기값을
 * URL 쿼리에서 읽어오기 때문에, 로컬 ref(1)로 두면 항상 1페이지로 리셋되던
 * 문제를 막는다.
 *
 * 사용:
 *   const { page } = usePageQuery()   // 기존 const page = ref(1) 대체
 *
 * @param {string} [key='page'] URL 쿼리 파라미터 이름
 * @returns {{ page: import('vue').Ref<number> }} 1-based 페이지 ref
 */
export function usePageQuery(key = 'page') {
    const route = useRoute()
    const router = useRouter()

    /** 쿼리 값을 1 이상 정수로 파싱(비정상 값은 1) */
    function parse(raw) {
        const n = Number(raw)
        return Number.isInteger(n) && n >= 1 ? n : 1
    }

    const page = ref(parse(route.query[key]))

    // 페이지 변경 → URL 쿼리에 반영. 히스토리 항목을 늘리지 않도록 replace 를 쓴다
    // (상세에서 뒤로가기 시 페이지 단위로 여러 번 눌러야 하는 상황 방지).
    watch(page, (val) => {
        if (parse(route.query[key]) === val) {
            return
        }
        const query = { ...route.query }
        if (val <= 1) {
            delete query[key]
        } else {
            query[key] = String(val)
        }
        router.replace({ query })
    })

    return { page }
}
