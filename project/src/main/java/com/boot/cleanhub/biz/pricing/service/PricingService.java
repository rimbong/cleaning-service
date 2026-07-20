package com.boot.cleanhub.biz.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.contract.domain.CleaningCycle;
import com.boot.cleanhub.biz.contract.domain.Contract;
import com.boot.cleanhub.biz.contract.repository.ContractRepository;
import com.boot.cleanhub.biz.pricing.domain.PricingPolicy;
import com.boot.cleanhub.biz.pricing.domain.VisitFrequency;
import com.boot.cleanhub.biz.pricing.dto.PriceEstimateLine;
import com.boot.cleanhub.biz.pricing.dto.PriceEstimateRequest;
import com.boot.cleanhub.biz.pricing.dto.PriceEstimateResponse;
import com.boot.cleanhub.biz.pricing.dto.PriceReviewResponse;
import com.boot.cleanhub.biz.pricing.dto.PriceReviewRow;
import com.boot.cleanhub.biz.pricing.dto.PriceReviewSkipped;
import com.boot.cleanhub.biz.pricing.dto.PricingPolicyRequest;
import com.boot.cleanhub.biz.pricing.dto.PricingPolicyResponse;
import com.boot.cleanhub.biz.pricing.repository.PricingPolicyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <pre>
 *   계단·공용부 정기청소 권장가 산정 서비스.
 *
 *   공식: (기본 출동료 + 층수x층당 + 세대수x세대당 + 화장실수x화장실당 + 엘리베이터) x 주기계수
 *        → 반올림 단위로 맞춤
 *
 *   결과는 <b>권장가(참고용)</b>다. 실제 계약 금액은 흥정·경쟁 상황에 따라 달라지므로
 *   이 값을 강제로 적용하지 않는다. 화면에서도 실제 금액 입력란을 따로 두고
 *   '적용' 버튼을 눌렀을 때만 옮긴다.
 *
 *   반올림은 HALF_UP 을 쓴다. 원본 HTML 계산기가 JS Math.round(양수 기준 반올림)를
 *   쓰고 있어 같은 입력에 같은 금액이 나와야 하기 때문이다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PricingService {

    /** 정책 행이 사라졌을 때 되살릴 기본값 — V22 의 초기값과 같아야 한다. */
    private static final long DEFAULT_BASE_FEE = 20000L;
    private static final long DEFAULT_PER_FLOOR = 6000L;
    private static final long DEFAULT_PER_HOUSEHOLD = 1500L;
    private static final long DEFAULT_PER_TOILET = 15000L;
    private static final long DEFAULT_ELEVATOR_FEE = 5000L;
    private static final long DEFAULT_ROUNDING_UNIT = 1000L;
    /** 원 단가표 6단계에 곡선을 맞춘 값(오차 5% 이내) — V25 의 초기값과 같아야 한다. */
    private static final BigDecimal DEFAULT_COEF_BASE = new BigDecimal("0.6224");
    private static final BigDecimal DEFAULT_COEF_EXPONENT = new BigDecimal("0.6949");

    private final PricingPolicyRepository pricingPolicyRepository;
    private final ContractRepository contractRepository;

    // ===================== 단가 정책 =====================

    public PricingPolicyResponse getPolicy() {
        return PricingPolicyResponse.from(getSingleton());
    }

    @Transactional
    public PricingPolicyResponse updatePolicy(PricingPolicyRequest request) {
        PricingPolicy policy = getSingleton();
        policy.setBaseFee(request.getBaseFee());
        policy.setPerFloor(request.getPerFloor());
        policy.setPerHousehold(request.getPerHousehold());
        policy.setPerToilet(request.getPerToilet());
        policy.setElevatorFee(request.getElevatorFee());
        policy.setCoefBase(request.getCoefBase());
        policy.setCoefExponent(request.getCoefExponent());
        policy.setRoundingUnit(request.getRoundingUnit());
        policy.setMemo(request.getMemo());
        return PricingPolicyResponse.from(pricingPolicyRepository.saveAndFlush(policy));
    }

    // ===================== 권장가 산정 =====================

    /**
     * 건물 규모와 주기로 권장 월 청소비를 계산한다.
     *
     * @param request 층수·세대수·화장실·엘리베이터·주기
     * @return 권장가와 산출 근거
     */
    public PriceEstimateResponse estimate(PriceEstimateRequest request) {
        return calculate(getSingleton(),
                nullToZero(request.getFloors()),
                nullToZero(request.getHouseholdCount()),
                nullToZero(request.getSharedToilets()),
                nullToZero(request.getExtraFloors()),
                Boolean.TRUE.equals(request.getHasElevator()),
                request.getVisitsPerMonth());
    }

    /**
     * 권장가 계산 본체 — 적정가 재산정 화면에서도 같은 로직을 쓴다.
     *
     * @param policy       단가 정책
     * @param floors       지상 층수
     * @param households   세대수
     * @param toilets      공용 화장실 수
     * @param extraFloors  지하·옥상 추가 층
     * @param hasElevator    엘리베이터 유무
     * @param visitsPerMonth 월 방문 횟수
     * @return 권장가와 산출 근거
     */
    public PriceEstimateResponse calculate(PricingPolicy policy, int floors, int households,
            int toilets, int extraFloors, boolean hasElevator, int visitsPerMonth) {

        int totalFloors = floors + extraFloors;
        long floorAmount = totalFloors * policy.getPerFloor();
        long householdAmount = households * policy.getPerHousehold();
        long toiletAmount = toilets * policy.getPerToilet();
        long elevatorAmount = hasElevator ? policy.getElevatorFee() : 0L;

        long subtotal = policy.getBaseFee() + floorAmount + householdAmount + toiletAmount + elevatorAmount;

        BigDecimal coefficient = policy.coefficientFor(visitsPerMonth);
        long total = roundTo(BigDecimal.valueOf(subtotal).multiply(coefficient), policy.getRoundingUnit());

        // 1회 방문 환산 — 고객이 "한 번 올 때 얼마냐"고 물을 때 바로 답할 수 있게.
        long perVisit = BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(visitsPerMonth), 0, RoundingMode.HALF_UP)
                .longValue();

        List<PriceEstimateLine> breakdown = new ArrayList<>();
        breakdown.add(new PriceEstimateLine("기본 출동료", "", policy.getBaseFee()));
        breakdown.add(new PriceEstimateLine("층당",
                describe(totalFloors, "개층", policy.getPerFloor()), floorAmount));
        breakdown.add(new PriceEstimateLine("세대당",
                describe(households, "세대", policy.getPerHousehold()), householdAmount));
        if (toilets > 0) {
            breakdown.add(new PriceEstimateLine("공용 화장실",
                    describe(toilets, "개", policy.getPerToilet()), toiletAmount));
        }
        if (hasElevator) {
            breakdown.add(new PriceEstimateLine("엘리베이터", "", elevatorAmount));
        }
        breakdown.add(new PriceEstimateLine("소계", "", subtotal));

        return new PriceEstimateResponse(total, subtotal, perVisit, visitsPerMonth, coefficient, breakdown);
    }

    /**
     * 계약이 쓸 월 방문 횟수를 정한다.
     *
     * 계약에 저장된 값을 그대로 쓴다. 계산 규칙은 계약 도메인(Contract.deriveVisitsPerMonth)
     * 한 곳에만 두고, 저장할 때 서버가 채워 넣는다. 여기서 다시 계산하면 규칙이 두 곳이 되어
     * 서로 어긋난다(격주·매월이 요일 개수를 무시하던 버그가 그렇게 났다).
     *
     * 값이 없는 경우(V26 이전 데이터 등)만 같은 규칙으로 유도한다.
     *
     * @param contract 계약
     * @return 월 방문 횟수(판단할 수 없으면 null)
     */
    private Integer resolveVisitsPerMonth(Contract contract) {
        if (contract.getVisitsPerMonth() != null && contract.getVisitsPerMonth() >= 1) {
            return contract.getVisitsPerMonth();
        }
        return contract.deriveVisitsPerMonth();
    }

    // ===================== 적정가 재산정 =====================

    /**
     * 진행 중 계약의 현재 월정액과 지금 기준 권장가를 나란히 비교한다.
     *
     * 거래처 단가가 몇 년 전에 정해진 채 그대로인 경우가 많아 "지금 얼마가 적정인가"를
     * 한 번에 보려는 용도다. 금액을 자동으로 바꾸지 않는다 — 인상은 협상이기 때문이다.
     *
     * 건물 규모나 청소 주기가 없는 계약은 계산 근거가 없어 제외하고, 몇 건을 왜 제외했는지
     * 같이 돌려준다(조용히 빼면 전체를 검토한 것으로 오해한다).
     *
     * @return 비교 결과와 제외 건수
     */
    public PriceReviewResponse review() {
        PricingPolicy policy = getSingleton();
        List<PriceReviewRow> rows = new ArrayList<>();
        List<PriceReviewSkipped> skipped = new ArrayList<>();

        for (Contract contract : contractRepository.findActiveWithClient()) {
            Client client = contract.getClient();
            if (client == null || client.getFloors() == null || client.getHouseholdCount() == null) {
                skipped.add(toSkipped(contract, client, PriceReviewSkipped.Reason.NO_BUILDING));
                continue;
            }
            Integer visitsPerMonth = resolveVisitsPerMonth(contract);
            if (visitsPerMonth == null) {
                skipped.add(toSkipped(contract, client, PriceReviewSkipped.Reason.NO_VISITS));
                continue;
            }

            PriceEstimateResponse estimate = calculate(policy,
                    client.getFloors(),
                    client.getHouseholdCount(),
                    nullToZero(client.getSharedToilets()),
                    nullToZero(client.getExtraFloors()),
                    Boolean.TRUE.equals(client.getHasElevator()),
                    visitsPerMonth);

            rows.add(new PriceReviewRow(
                    contract.getId(),
                    client.getId(),
                    client.getName(),
                    contract.getTitle(),
                    buildingSummary(client),
                    contract.getMonthlyFee() != null ? contract.getMonthlyFee() : 0L,
                    estimate,
                    contract.getCleaningCycle() == CleaningCycle.WEEKLY));
        }

        // 올려야 할 금액이 큰 순 — 먼저 손볼 거래처가 위로 온다.
        rows.sort(Comparator.comparingLong(PriceReviewRow::getDifference).reversed());
        // 빠진 것은 거래처명 순 — 고치러 다닐 때 찾기 쉽게.
        skipped.sort(Comparator.comparing(
                s -> s.getClientName() != null ? s.getClientName() : ""));
        return new PriceReviewResponse(rows, skipped);
    }

    /** 재산정에서 빠진 계약을 이유와 함께 담는다 */
    private PriceReviewSkipped toSkipped(Contract contract, Client client, PriceReviewSkipped.Reason reason) {
        return new PriceReviewSkipped(
                contract.getId(),
                client != null ? client.getId() : null,
                client != null ? client.getName() : null,
                contract.getTitle(),
                reason);
    }

    /** "5층 10세대 · 공용화장실 2" 형태의 건물 요약 */
    private String buildingSummary(Client client) {
        StringBuilder sb = new StringBuilder();
        sb.append(client.getFloors()).append("층 ").append(client.getHouseholdCount()).append("세대");
        if (client.getSharedToilets() != null && client.getSharedToilets() > 0) {
            sb.append(" · 화장실 ").append(client.getSharedToilets());
        }
        if (client.getExtraFloors() != null && client.getExtraFloors() > 0) {
            sb.append(" · 추가 ").append(client.getExtraFloors()).append("층");
        }
        if (Boolean.TRUE.equals(client.getHasElevator())) {
            sb.append(" · 엘베");
        }
        return sb.toString();
    }

    // ===================== 내부 =====================

    /**
     * 반올림 단위에 맞춰 금액을 정리한다.
     * HALF_UP 인 이유는 원본 HTML 계산기(JS Math.round)와 같은 값이 나와야 하기 때문이다.
     */
    private long roundTo(BigDecimal amount, long unit) {
        if (unit <= 1L) {
            return amount.setScale(0, RoundingMode.HALF_UP).longValue();
        }
        BigDecimal unitValue = BigDecimal.valueOf(unit);
        return amount.divide(unitValue, 0, RoundingMode.HALF_UP)
                .multiply(unitValue)
                .longValue();
    }

    /** "5개층 x 6,000원" 형태의 근거 문구 */
    private String describe(int count, String unitName, long unitPrice) {
        return count + unitName + " x " + String.format("%,d", unitPrice) + "원";
    }

    private int nullToZero(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * 단일 행 획득. V22 가 초기값을 넣어두므로 정상적으로는 항상 존재한다.
     * 누군가 지운 경우에 대비해 기본값으로 되살리되, 조용히 넘어가면 엉뚱한 금액이
     * 나갈 수 있으므로 경고 로그를 남긴다.
     */
    @Transactional
    PricingPolicy getSingleton() {
        return pricingPolicyRepository.findAll().stream().findFirst().orElseGet(() -> {
            log.warn("단가 정책 행이 없어 기본값으로 생성합니다. 단가를 확인하세요(관리자 > 단가 정책).");
            PricingPolicy p = new PricingPolicy();
            p.setBaseFee(DEFAULT_BASE_FEE);
            p.setPerFloor(DEFAULT_PER_FLOOR);
            p.setPerHousehold(DEFAULT_PER_HOUSEHOLD);
            p.setPerToilet(DEFAULT_PER_TOILET);
            p.setElevatorFee(DEFAULT_ELEVATOR_FEE);
            p.setCoefBase(DEFAULT_COEF_BASE);
            p.setCoefExponent(DEFAULT_COEF_EXPONENT);
            p.setRoundingUnit(DEFAULT_ROUNDING_UNIT);
            return pricingPolicyRepository.save(p);
        });
    }
}
