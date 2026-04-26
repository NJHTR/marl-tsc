package com.njhtr.marltsc.signal.service.impl;

import com.njhtr.marltsc.common.exception.BusinessException;
import com.njhtr.marltsc.common.exception.ErrorCode;
import com.njhtr.marltsc.signal.api.dto.request.PhaseAdjustRequest;
import com.njhtr.marltsc.signal.api.dto.response.PhaseResponse;
import com.njhtr.marltsc.signal.api.dto.response.SignalPlanResponse;
import com.njhtr.marltsc.signal.domain.entity.SignalPlan;
import com.njhtr.marltsc.signal.domain.service.PhaseOptimizationDomainService;
import com.njhtr.marltsc.signal.infrastructure.mapper.SignalPlanMapper;
import com.njhtr.marltsc.signal.service.api.SignalControlAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Signal control application service implementation.
 *
 * <p>Thin glue layer: fetch data -> delegate to domain service -> save -> convert to DTO</p>
 */
@Service
@RequiredArgsConstructor
public class SignalControlAppServiceImpl implements SignalControlAppService {

    private final SignalPlanMapper planMapper;
    private final PhaseOptimizationDomainService domainService;

    @Override
    public SignalPlanResponse getCurrentPlan(String intersectionId) {
        Assert.hasText(intersectionId, "路口ID不能为空");

        List<SignalPlan> plans = planMapper.selectByIntersectionId(intersectionId);
        if (plans.isEmpty()) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND.getCode(), "路口无有效信号方案");
        }

        SignalPlan plan = plans.get(0);
        return convertToResponse(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPhase(PhaseAdjustRequest request) {
        Assert.hasText(request.getPlanId(), "方案ID不能为空");

        SignalPlan plan = planMapper.selectById(request.getPlanId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND.getCode(), "信号方案不存在");
        }

        // Calculate optimal green time via domain service (pure logic)
        List<Double> flowRatios = List.of(0.3, 0.4, 0.2, 0.1);
        int lostTime = 12;
        double greenRatio = domainService.calculateGreenRatio(flowRatios, plan.getCycleTime(), lostTime);

        int optimalGreen = (int) Math.round(greenRatio);
        if (request.getSuggestedGreenTime() != null && request.getSuggestedGreenTime() > 0) {
            optimalGreen = request.getSuggestedGreenTime();
        }

        // Update plan
        plan.setCycleTime(plan.getCycleTime());
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.update(plan);
    }

    private SignalPlanResponse convertToResponse(SignalPlan plan) {
        SignalPlanResponse response = new SignalPlanResponse();
        response.setPlanId(plan.getPlanId());
        response.setIntersectionId(plan.getIntersectionId());
        response.setPlanName(plan.getPlanName());
        response.setCycleTime(plan.getCycleTime());
        // Phase details would be populated from a phase mapper in production
        response.setPhases(Collections.emptyList());
        return response;
    }
}
