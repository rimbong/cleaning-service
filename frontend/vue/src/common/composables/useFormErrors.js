import { reactive } from 'vue'

/**
 * 폼 필드별 검증 에러를 관리하는 컴포저블.
 *
 * 저장 실패를 상단 토스트로만 알리던 방식 대신, 각 입력 필드 아래에
 * 에러 메시지를 인라인으로 표시하기 위한 공용 상태/헬퍼를 제공한다.
 *
 * 사용:
 *   const { errors, setError, clearError, reset, hasErrors } = useFormErrors()
 *   function validate() {
 *       reset()
 *       if (!form.title.trim()) setError('title', '제목은 필수입니다.')
 *       return !hasErrors()
 *   }
 *   // template: :class="{ 'has-error': errors.title }" + <p v-if="errors.title" class="err-msg">{{ errors.title }}</p>
 *   //           입력에는 @input="clearError('title')" 로 수정 즉시 에러 해제
 *
 * @returns {{
 *   errors: Record<string, string>,
 *   setError: (field: string, message: string) => void,
 *   clearError: (field: string) => void,
 *   reset: () => void,
 *   hasErrors: () => boolean
 * }}
 */
export function useFormErrors() {
    const errors = reactive({})

    /** 특정 필드에 에러 메시지를 설정 */
    function setError(field, message) {
        errors[field] = message
    }

    /** 특정 필드의 에러를 해제(입력 수정 시 호출) */
    function clearError(field) {
        if (errors[field]) {
            delete errors[field]
        }
    }

    /** 모든 에러 초기화(검증 시작 시 호출) */
    function reset() {
        Object.keys(errors).forEach((key) => {
            delete errors[key]
        })
    }

    /** 현재 에러가 하나라도 있는지 */
    function hasErrors() {
        return Object.keys(errors).length > 0
    }

    return { errors, setError, clearError, reset, hasErrors }
}
