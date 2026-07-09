import { createI18n } from 'vue-i18n'

import ko from './locales/ko.json'
import en from './locales/en.json'
import { setApiLocale } from '@/common/plugins/http/axios'

/** 지원 언어 목록 */
export const SUPPORTED_LOCALES = ['ko', 'en']
const DEFAULT_LOCALE = 'ko'

const i18n = createI18n({
    legacy: false, // Composition API 모드(useI18n) 사용
    locale: DEFAULT_LOCALE,
    fallbackLocale: 'en',
    messages: {
        ko,
        en,
    },
})

// 초기 API 요청 언어도 화면 언어와 맞춰둔다.
setApiLocale(DEFAULT_LOCALE)

/**
 * 화면 언어(vue-i18n)와 API 언어(Accept-Language)를 함께 전환한다.
 *  - vue-i18n locale  → 화면 UI 텍스트
 *  - Accept-Language  → 이후 API 응답(에러 메시지 등)을 백엔드가 해당 언어로 localize
 * @param {string} locale 'ko' | 'en'
 */
export function setLocale(locale) {
    if (!SUPPORTED_LOCALES.includes(locale)) {
        return
    }
    i18n.global.locale.value = locale
    setApiLocale(locale)
}

export default i18n
