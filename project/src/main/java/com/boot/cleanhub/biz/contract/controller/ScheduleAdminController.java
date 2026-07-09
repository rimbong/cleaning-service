package com.boot.cleanhub.biz.contract.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.biz.contract.dto.WeeklyScheduleResponse;
import com.boot.cleanhub.biz.contract.service.ContractService;
import com.boot.cleanhub.common.api.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   청소 스케줄 API — 관리자 전용. 진행 중 계약을 청소 요일별로 보여준다
 *   (아버님이 "오늘 어디 청소가지?"를 요일로 확인).
 *   경로가 /api/admin/** 이므로 ROLE_ADMIN 보호.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/schedule")
@RequiredArgsConstructor
public class ScheduleAdminController {

    private final ContractService contractService;

    /** 주간 청소 스케줄(월~일 요일별 거래처 목록) */
    @GetMapping
    public ApiResponse<WeeklyScheduleResponse> weekly() {
        return ApiResponse.ok(contractService.getWeeklySchedule());
    }
}
