/**
 * 날짜/시간 input 개선 — 입력 박스 아무 곳이나 클릭하면 달력(피커)이 열리게 한다.
 *
 * 네이티브 <input type="date"> 는 기본적으로 우측 달력 아이콘을 눌러야만 피커가 열려
 * 사용성이 떨어진다. 문서 전역에 클릭 위임 리스너를 하나 걸어, 날짜류 input 을 클릭하면
 * HTMLInputElement.showPicker() 로 피커를 띄운다. 새로 렌더되는 폼에도 자동 적용된다.
 *
 * showPicker() 는 최신 크로미움/파이어폭스에서 지원한다. 미지원 브라우저에서는
 * 기존 동작(아이콘 클릭)만 유지되도록 조용히 무시한다.
 */

/** 피커를 여는 대상 input 타입 */
const PICKER_TYPES = ['date', 'datetime-local', 'month', 'week', 'time'];

/**
 * 전역 클릭 위임 리스너를 설치한다. 앱 시작 시 1회 호출.
 */
export function installDatePickerOpener() {
    document.addEventListener('click', (event) => {
        const el = event.target;
        if (!(el instanceof HTMLInputElement)) {
            return;
        }
        if (!PICKER_TYPES.includes(el.type)) {
            return;
        }
        if (el.disabled || el.readOnly) {
            return;
        }
        if (typeof el.showPicker !== 'function') {
            return;
        }
        try {
            el.showPicker();
        } catch (e) {
            // 이미 피커가 열려 있거나(아이콘 클릭과 중복) 지원되지 않는 상황 —
            // 기본 동작을 막지 않으므로 무시한다.
        }
    });
}
