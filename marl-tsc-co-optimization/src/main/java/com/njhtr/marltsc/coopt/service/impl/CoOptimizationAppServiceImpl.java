package com.njhtr.marltsc.coopt.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njhtr.marltsc.common.exception.BusinessException;
import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.coopt.api.dto.response.OptimizationResultResponse;
import com.njhtr.marltsc.coopt.api.dto.response.RouteAdjustmentResponse;
import com.njhtr.marltsc.coopt.api.dto.response.SignalActionResponse;
import com.njhtr.marltsc.coopt.domain.bo.*;
import com.njhtr.marltsc.coopt.domain.entity.OptimizationLog;
import com.njhtr.marltsc.coopt.domain.service.CoOptimizationDomainService;
import com.njhtr.marltsc.coopt.event.publisher.OptimizationEventPublisher;
import com.njhtr.marltsc.coopt.infrastructure.client.RoutePlanningClient;
import com.njhtr.marltsc.coopt.infrastructure.client.SignalControlClient;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.PhaseAdjustRequest;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.RouteRequest;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.RouteResponse;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.SignalPlanResponse;
import com.njhtr.marltsc.coopt.infrastructure.mapper.OptimizationLogMapper;
import com.njhtr.marltsc.coopt.service.api.CoOptimizationAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoOptimizationAppServiceImpl implements CoOptimizationAppService {

    private final SignalControlClient signalControlClient;
    private final RoutePlanningClient routePlanningClient;
    private final OptimizationLogMapper optimizationLogMapper;
    private final OptimizationEventPublisher eventPublisher;
    private final CoOptimizationDomainService domainService = new CoOptimizationDomainService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, OptimizationResultResponse> latestResults = new HashMap<>();

    @Override
    public void triggerOptimization(String intersectionId) {
        log.info("Triggering optimization for intersection: {}", intersectionId);

        // 获取信号控制计划,失败时使用null
        SignalPlanResponse currentPlan = null;
        try {
            ApiResult<SignalPlanResponse> planResult = signalControlClient.getCurrentPlan(intersectionId);
            currentPlan = planResult != null && planResult.getData() != null ? planResult.getData() : null;
        } catch (Exception e) {
            log.warn("Failed to get signal plan for intersection {}, proceeding without it: {}", intersectionId, e.getMessage());
        }

        // 获取路网状态,失败时使用空Map降级
        Map<String, Double> networkStatus = Collections.emptyMap();
        try {
            ApiResult<Map<String, Double>> statusResult = routePlanningClient.getNetworkStatus();
            networkStatus = statusResult != null && statusResult.getData() != null ? statusResult.getData() : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to get network status, using empty map as fallback: {}", e.getMessage());
        }

        PredictedNetworkStateBO currentState = new PredictedNetworkStateBO();
        currentState.setSegmentTravelTimes(new HashMap<>(networkStatus));
        currentState.setSegmentCongestionProbs(new HashMap<>());

        JointOptimizationBO jointBO = new JointOptimizationBO();
        jointBO.setIntersectionId(intersectionId);
        if (currentPlan != null) {
            SignalPlanBO planBO = new SignalPlanBO();
            planBO.setPlanId(currentPlan.getPlanId());
            planBO.setIntersectionId(currentPlan.getIntersectionId());
            planBO.setCycleTime(currentPlan.getCycleTime());
            jointBO.setCurrentSignalPlan(planBO);
        }
        jointBO.setPredictedNetworkState(currentState);

        Map<String, SignalActionBO> signalActions = new HashMap<>();
        Map<String, RouteChangeBO> routeChanges = new HashMap<>();

        PredictedNetworkStateBO previousState = null;
        PredictedNetworkStateBO predictedState = currentState;
        int iterations = 0;
        double bestReward = Double.NEGATIVE_INFINITY;
        SignalActionBO bestSignalAction = null;

        while (iterations < 3) {
            iterations++;

            SignalActionBO signalAction = new SignalActionBO();
            signalAction.setPhaseId(1);
            signalAction.setGreenTime(30 + iterations * 10);
            signalActions.put(intersectionId, signalAction);

            previousState = predictedState;
            predictedState = domainService.predictNetworkState(predictedState, signalActions);

            RouteChangeBO routeChange = new RouteChangeBO();
            routeChange.setVehicleId("vehicle_" + iterations);
            routeChange.setNewRoute(Arrays.asList(intersectionId, "next_" + intersectionId));
            routeChanges.put(routeChange.getVehicleId(), routeChange);

            double reward = domainService.evaluateJointPolicy(signalActions, routeChanges);
            if (reward > bestReward) {
                bestReward = reward;
                bestSignalAction = signalAction;
            }

            if (domainService.isConverged(previousState, predictedState)) {
                log.info("Optimization converged after {} iterations", iterations);
                break;
            }
        }

        // 应用信号控制调整,失败时记录警告但继续执行
        if (bestSignalAction != null) {
            try {
                PhaseAdjustRequest adjustRequest = new PhaseAdjustRequest();
                adjustRequest.setIntersectionId(intersectionId);
                adjustRequest.setPhaseId(bestSignalAction.getPhaseId());
                adjustRequest.setGreenTime(bestSignalAction.getGreenTime());
                signalControlClient.adjustPhase(adjustRequest);
            } catch (Exception e) {
                log.warn("Failed to adjust signal phase for intersection {}: {}", intersectionId, e.getMessage());
            }
        }

        List<RouteAdjustmentResponse> routeAdjustments = new ArrayList<>();
        for (RouteChangeBO rc : routeChanges.values()) {
            RouteRequest routeRequest = new RouteRequest();
            routeRequest.setOrigin(rc.getVehicleId());
            routeRequest.setDestination("dest_" + rc.getVehicleId());
            try {
                ApiResult<RouteResponse> routeResult = routePlanningClient.computeRoute(routeRequest);
                if (routeResult != null && routeResult.getData() != null) {
                    RouteAdjustmentResponse adj = new RouteAdjustmentResponse();
                    adj.setVehicleId(rc.getVehicleId());
                    adj.setNewPath(rc.getNewRoute());
                    adj.setEstimatedTimeSaved(10.0);
                    routeAdjustments.add(adj);
                }
            } catch (Exception e) {
                log.warn("Failed to compute route for vehicle {}", rc.getVehicleId());
            }
        }

        OptimizationLog logEntity = new OptimizationLog();
        logEntity.setLogId(UUID.randomUUID().toString());
        logEntity.setTriggerTime(LocalDateTime.now());
        logEntity.setIntersectionId(intersectionId);
        logEntity.setJointReward(bestReward);
        try {
            logEntity.setSignalActionJson(objectMapper.writeValueAsString(bestSignalAction));
            logEntity.setRouteActionJson(objectMapper.writeValueAsString(routeChanges));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize actions", e);
        }
        logEntity.setStatus(1);
        optimizationLogMapper.insert(logEntity);

        OptimizationResultResponse response = new OptimizationResultResponse();
        response.setLogId(logEntity.getLogId());
        response.setIntersectionId(intersectionId);
        response.setTriggerTime(logEntity.getTriggerTime());
        response.setJointReward(bestReward);
        if (bestSignalAction != null) {
            SignalActionResponse sar = new SignalActionResponse();
            sar.setPhaseId(bestSignalAction.getPhaseId());
            sar.setGreenTime(bestSignalAction.getGreenTime());
            response.setSignalAction(sar);
        }
        response.setRouteAdjustments(routeAdjustments);
        response.setConvergenceIterations(iterations);

        latestResults.put(intersectionId, response);
        eventPublisher.publishOptimizationCompleted(response);
    }

    @Override
    public OptimizationResultResponse getLatestResult(String intersectionId) {
        OptimizationResultResponse result = latestResults.get(intersectionId);
        if (result == null) {
            throw new BusinessException(404, "No optimization result found for intersection: " + intersectionId);
        }
        return result;
    }
}
