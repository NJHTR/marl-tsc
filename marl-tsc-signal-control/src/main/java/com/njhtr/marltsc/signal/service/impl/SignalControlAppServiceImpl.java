package com.njhtr.marltsc.signal.service.impl;

import com.njhtr.marltsc.common.exception.BusinessException;
import com.njhtr.marltsc.common.exception.ErrorCode;
import com.njhtr.marltsc.common.dto.PhaseAdjustRequest;
import com.njhtr.marltsc.common.dto.PhaseResponse;
import com.njhtr.marltsc.common.dto.SignalPlanResponse;
import com.njhtr.marltsc.signal.domain.entity.SignalPhase;
import com.njhtr.marltsc.signal.domain.entity.SignalPlan;
import com.njhtr.marltsc.signal.domain.service.PhaseOptimizationDomainService;
import com.njhtr.marltsc.common.domain.RewardCalculator;
import com.njhtr.marltsc.common.domain.StateVectorBuilder;
import com.njhtr.marltsc.signal.infrastructure.client.DrlEngineClient;
import com.njhtr.marltsc.signal.infrastructure.client.dto.DrlActionResponse;
import com.njhtr.marltsc.signal.infrastructure.mapper.SignalPhaseMapper;
import com.njhtr.marltsc.signal.infrastructure.mapper.SignalPlanMapper;
import com.njhtr.marltsc.signal.service.api.SignalControlAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalControlAppServiceImpl implements SignalControlAppService {

    private static final int GREEN_TIME_STEP = 5;

    private final SignalPlanMapper planMapper;
    private final SignalPhaseMapper phaseMapper;
    private final PhaseOptimizationDomainService domainService;
    private final StateVectorBuilder stateVectorBuilder;
    private final RewardCalculator rewardCalculator;
    private final DrlEngineClient drlClient;

    @Override
    public SignalPlanResponse getCurrentPlan(String intersectionId) {
        Assert.hasText(intersectionId, "路口ID不能为空");

        List<SignalPlan> plans = planMapper.selectByIntersectionId(intersectionId);
        if (plans.isEmpty()) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND.getCode(), "路口无有效信号方案");
        }

        return convertToResponse(plans.get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPhase(PhaseAdjustRequest request) {
        Assert.hasText(request.getPlanId(), "方案ID不能为空");

        SignalPlan plan = planMapper.selectById(request.getPlanId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND.getCode(), "信号方案不存在");
        }

        String intersectionId = request.getIntersectionId() != null
                ? request.getIntersectionId() : plan.getIntersectionId();

        int greenTime;
        if (hasTrafficState(request)) {
            greenTime = drlDrivenAdjust(request, plan, intersectionId);
        } else {
            greenTime = fallbackAdjust(request, plan);
        }

        plan.setUpdateTime(LocalDateTime.now());
        planMapper.update(plan);

        log.info("Phase adjusted: plan={}, intersection={}, greenTime={}s",
                request.getPlanId(), intersectionId, greenTime);
    }

    private boolean hasTrafficState(PhaseAdjustRequest request) {
        return request.getFlow() != null && request.getSpeed() != null
                && request.getOccupancy() != null && request.getQueueLength() != null
                && request.getDelay() != null;
    }

    /**
     * DRL-driven adjustment: build state → infer action → map to green time → train on outcome.
     */
    private int drlDrivenAdjust(PhaseAdjustRequest request, SignalPlan plan, String intersectionId) {
        int currentGreen = request.getSuggestedGreenTime() != null ? request.getSuggestedGreenTime() : 30;

        double[] state = stateVectorBuilder.build(
                request.getFlow(), request.getSpeed(), request.getOccupancy(),
                request.getQueueLength(), request.getDelay(),
                currentGreen, plan.getCycleTime());

        Optional<DrlActionResponse> decision = drlClient.decide(intersectionId, state);

        int action;
        int greenTime;
        if (decision.isPresent()) {
            action = decision.get().getAction();
            greenTime = actionToGreenTime(action);
            log.info("DRL decided: intersection={}, action={}, greenTime={}s, confidence={}",
                    intersectionId, action, greenTime, decision.get().getConfidence());
        } else {
            action = 1;
            greenTime = fallbackWebster(plan);
            log.info("DRL unavailable, fallback Webster: intersection={}, greenTime={}s",
                    intersectionId, greenTime);
        }

        double reward = rewardCalculator.compute(
                request.getFlow(), request.getSpeed(), request.getOccupancy(),
                request.getQueueLength(), request.getDelay());

        drlClient.train(intersectionId, state, action, reward, state, false);

        return greenTime;
    }

    /**
     * Fallback when no traffic state is provided: use Webster or explicit suggestion.
     */
    private int fallbackAdjust(PhaseAdjustRequest request, SignalPlan plan) {
        if (request.getSuggestedGreenTime() != null && request.getSuggestedGreenTime() > 0) {
            return request.getSuggestedGreenTime();
        }
        return fallbackWebster(plan);
    }

    private int fallbackWebster(SignalPlan plan) {
        List<Double> flowRatios = List.of(0.3, 0.4, 0.2, 0.1);
        double greenRatio = domainService.calculateGreenRatio(flowRatios, plan.getCycleTime(), 12);
        return Math.max(10, (int) Math.round(greenRatio));
    }

    /**
     * Maps discrete DRL action (0..3) to green time in seconds.
     * action 0 → 15s, action 1 → 25s, action 2 → 35s, action 3 → 45s
     */
    static int actionToGreenTime(int action) {
        return (3 + action * 2) * GREEN_TIME_STEP;
    }

    private SignalPlanResponse convertToResponse(SignalPlan plan) {
        SignalPlanResponse response = new SignalPlanResponse();
        response.setPlanId(plan.getPlanId());
        response.setIntersectionId(plan.getIntersectionId());
        response.setPlanName(plan.getPlanName());
        response.setCycleTime(plan.getCycleTime());
        response.setPhases(loadPhases(plan.getPlanId()));
        return response;
    }

    private List<PhaseResponse> loadPhases(String planId) {
        List<SignalPhase> phases = phaseMapper.selectByPlanId(planId);
        if (phases == null || phases.isEmpty()) {
            return Collections.emptyList();
        }
        return phases.stream().map(p -> {
            PhaseResponse r = new PhaseResponse();
            r.setPhaseId(p.getPhaseId());
            r.setDirection(p.getDirection());
            r.setGreenTime(p.getGreenTime());
            return r;
        }).toList();
    }
}
