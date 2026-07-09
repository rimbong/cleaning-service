/**
 * <pre>
 *   biz — 본격 비즈니스 도메인 루트.
 *
 *   CleanHub 의 실제 도메인(견적·거래처·계약·정산·지출·회사 등)은 모두 이 biz 패키지 하위에
 *   도메인별로 둔다. 공용 인프라(auth·common·config·filter·interceptor·error·util)와 명확히
 *   분리하기 위함이다(head 프레임워크의 3분 구조 정합).
 *
 *   [배치 규칙] 도메인마다 표준 레이어
 *     com.boot.cleanhub.biz.&lt;도메인&gt;/          예: biz.client, biz.settlement
 *       controller/   REST/화면 컨트롤러
 *       service/      비즈니스 로직
 *       repository/   JPA/MyBatis 데이터 접근
 *       domain/       엔티티/도메인 모델
 *       dto/          요청/응답 DTO
 *
 *   [대민(USER)/관리자(ADMIN) 구분]
 *     패키지로 admin/public 을 나누지 않는다(한 도메인이 두 트리로 흩어져 응집도가 깨짐).
 *     접근권한은 웹/보안 관심사로 아래 3가지로 표현한다:
 *       (1) 컨트롤러 이름   : ClientAdminController(관리자) / ClientController(대민)
 *       (2) URL           : /api/admin/clients(관리자) / /api/clients(대민)
 *       (3) 보안 규칙       : config 의 SessionSecurityConfig / JwtApiSecurityConfig 에서 URL 로 인가
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
package com.boot.cleanhub.biz;
