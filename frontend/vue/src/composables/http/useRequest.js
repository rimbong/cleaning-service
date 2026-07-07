import { ref } from 'vue'

import { get, post, put, del } from '@/plugins/http/axios'

/**
 * API 요청의 반응형 상태(data/error/isPending)를 관리하는 컴포저블.
 * Vue 컴포넌트의 setup 안에서 사용한다.
 *
 * @returns {{data: import('vue').Ref, error: import('vue').Ref, isPending: import('vue').Ref, exec: Function}}
 */
export function useRequest() {
    const data = ref(null)
    const error = ref(null)
    const isPending = ref(false)

    /**
     * axios 요청(Promise)을 실행하고 상태를 자동 갱신한다.
     * @param {Promise} axiosPromise - get()/post() 등의 반환값
     * @returns {Promise} 성공 시 응답 본문(response.data)
     */
    const exec = async (axiosPromise) => {
        isPending.value = true
        error.value = null
        data.value = null
        try {
            const response = await axiosPromise
            data.value = response.data
            return response.data
        } catch (err) {
            error.value = err
            throw err
        } finally {
            isPending.value = false
        }
    }

    return {
        data,
        error,
        isPending,
        exec,
    }
}

/**
 * GET/POST 등을 바로 호출하는 편의 컴포저블. 내부적으로 useRequest 를 쓴다.
 *
 * @returns {{data, error, isPending, $get, $post, $put, $delete}}
 */
export function useRequestMethods() {
    const { exec, ...states } = useRequest()

    return {
        ...states,
        $get: (url, config) => exec(get(url, config)),
        $post: (url, data, config) => exec(post(url, data, config)),
        $put: (url, data, config) => exec(put(url, data, config)),
        $delete: (url, config) => exec(del(url, config)),
    }
}
