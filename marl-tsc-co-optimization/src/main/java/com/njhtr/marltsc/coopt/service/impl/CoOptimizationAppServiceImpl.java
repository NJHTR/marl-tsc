package com.njhtr.marltsc.coopt.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njhtr.marltsc.common.exception.BusinessException;
import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.coopt.api.dto.response.OptimizationResultResponse;
import com.njhtr.marltsc.coopt.api.dto.response.RouteAdjustmentResponse;
import com.njhtr.marltsc.coopt.api.dto.response.SignalActionResponse;
import com.njhtr.marltsc.coopt.domain.entity.OptimizationLog;
import com.njhtr.marltsc.coopt.domain.service.RewardCalculator;
import com.njhtr.marltsc.coopt.domain.service.StateVectorBuilder;
import com.njhtr.marltsc.coopt.event.publisher.OptimizationEventPublisher;
import com.njhtr.marltsc.coopt.infrastructure.client.DataFusionClient;
import com.njhtr.marltsc.coopt.infrastructure.client.DrlEngineClient;
import com.njhtr.marltsc.coopt.infrastructure.client.SignalControlClient;
import com.njhtr.marltsc.coopt.infrastructure.client.dto.*;
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

    private static final int GREEN_TIME_STEP = 5;
    private static final int DEFAULT_GREEN_TIME = 30;
    private static final int DEFAULT_CYCLE_TIME = 120;

    private final SignalControlClient signalControlClient;
    private final DataFusionClient dataFusionClient;
    private final DrlEngineClient drlEngineClient;
    private final OptimizationLogMapper optimizationLogMapper;
    private final OptimizationEventPublisher eventPublisher;
    private final StateVectorBuilder stateVectorBuilder;
    private final RewardCalculator rewardCalculator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, OptimizationResultResponse> latestResults = new HashMap<>();

    @Override
    public void triggerOptimization(String intersectionId) {
        if ("ALL".equals(intersectionId)) {
            triggerForAllIntersections();
        } else {
            optimizeSingle(intersectionId);
        }
    }

    private void triggerForAllIntersections() {
        List<TrafficSnapshotResponse> snapshots = fetchAllSnapshots();
        if (snapshots.isEmpty()) {
            log.debug("No traffic snapshots available, skipping periodic optimization");
            return;
        }
        for (TrafficSnapshotResponse snap : snapshots) {
            try {
                optimizeWithSnapshot(snap);
            } catch (Exception e) {
                log.warn("Optimization failed for {}: {}", snap.getIntersectionId(), e.getMessage());
            }
        }
    }

    private void optimizeSingle(String intersectionId) {
        TrafficSnapshotResponse snap = fetchSnapshot(intersectionId);
        if (snap == null) {
            log.warn("No traffic snapshot for {}, skipping", intersectionId);
            return;
        }
        optimizeWithSnapshot(snap);
    }

    /**
     * Core DRL-driven optimization for a single intersection:
     * traffic snapshot → state vector → DRL decide → execute → reward → DRL train
     */
    private void optimizeWithSnapshot(TrafficSnapshotResponse snap) {
        String intersectionId = snap.getIntersectionId();

        int cycleTime = DEFAULT_CYCLE_TIME;
        int currentGreen = DEFAULT_GREEN_TIME;
        String planId = intersectionId;

        SignalPlanResponse plan = fetchSignalPlan(intersectionId);
        if (plan != null) {
            planId = plan.getPlanId();
            cycleTime = plan.getCycleTime() != null ? plan.getCycleTime() : DEFAULT_CYCLE_TIME;
        }

        double[] state = stateVectorBuilder.build(
                snap.getFlow(), snap.getSpeed(), snap.getOccupancy(),
                snap.getQueueLength(), snap.getDelay(),
                currentGreen, cycleTime);

        DrlInferenceRequest inferReq = new DrlInferenceRequest(intersectionId, state);
        int action;
        int greenTime;
        double confidence = 0;
        try {
            ApiResult<DrlActionResponse> result = drlEngineClient.decide(inferReq);
            if (result != null && result.getData() != null) {
                action = result.getData().getAction();
                greenTime = actionToGreenTime(action);
                confidence = result.getData().getConfidence();
                log.info("DRL decided: {} action={} green={}s conf={}", intersectionId, action, greenTime, String.format("%.2f", confidence));
            } else {
                action = 1;
                greenTime = DEFAULT_GREEN_TIME;
                log.info("DRL returned empty, using default green={}s for {}", greenTime, intersectionId);
            }
        } catch (Exception e) {
            action = 1;
            greenTime = DEFAULT_GREEN_TIME;
            log.warn("DRL decide failed for {}, using default: {}", intersectionId, e.getMessage());
        }

        try {
            PhaseAdjustRequest adjustReq = new PhaseAdjustRequest();
            adjustReq.setPlanId(planId);
            adjustReq.setIntersectionId(intersectionId);
            adjustReq.setPhaseId(1);
            adjustReq.setSuggestedGreenTime(greenTime);
            signalControlClient.adjustPhase(adjustReq);
        } catch (Exception e) {
            log.warn("Signal adjust failed for {}: {}", intersectionId, e.getMessage());
        }

        double reward = rewardCalculator.compute(
                snap.getFlow(), snap.getSpeed(), snap.getOccupancy(),
                snap.getQueueLength(), snap.getDelay());

        DrlTrainRequest trainReq = new DrlTrainRequest(intersectionId, state, action, reward, state, false);
        try {
            drlEngineClient.train(trainReq);
        } catch (Exception e) {
            log.warn("DRL train failed for {}: {}", intersectionId, e.getMessage());
        }

        OptimizationLog logEntity = new OptimizationLog();
        logEntity.setLogId(UUID.randomUUID().toString());
        logEntity.setTriggerTime(LocalDateTime.now());
        logEntity.setIntersectionId(intersectionId);
        logEntity.setJointReward(reward);
        try {
            Map<String, Object> actionMap = new LinkedHashMap<>();
            actionMap.put("action", action);
            actionMap.put("greenTime", greenTime);
            actionMap.put("confidence", confidence);
            actionMap.put("occupancy", snap.getOccupancy());
            actionMap.put("congestion", snap.getCongestionLevel());
            logEntity.setSignalActionJson(objectMapper.writeValueAsString(actionMap));
            logEntity.setRouteActionJson("{}");
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize actions", e);
        }
        logEntity.setStatus(1);
        try {
            optimizationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("Failed to persist optimization log: {}", e.getMessage());
        }

        OptimizationResultResponse response = new OptimizationResultResponse();
        response.setLogId(logEntity.getLogId());
        response.setIntersectionId(intersectionId);
        response.setTriggerTime(logEntity.getTriggerTime());
        response.setJointReward(reward);
        SignalActionResponse sar = new SignalActionResponse();
        sar.setPhaseId(1);
        sar.setGreenTime(greenTime);
        response.setSignalAction(sar);
        response.setRouteAdjustments(Collections.emptyList());
        response.setConvergenceIterations(1);

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

    private List<TrafficSnapshotResponse> fetchAllSnapshots() {
        try {
            ApiResult<List<TrafficSnapshotResponse>> result = dataFusionClient.getAllSnapshots();
            return result != null && result.getData() != null ? result.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch traffic snapshots: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private TrafficSnapshotResponse fetchSnapshot(String intersectionId) {
        try {
            ApiResult<TrafficSnapshotResponse> result = dataFusionClient.getSnapshot(intersectionId);
            return result != null ? result.getData() : null;
        } catch (Exception e) {
            log.warn("Failed to fetch snapshot for {}: {}", intersectionId, e.getMessage());
            return null;
        }
    }

    private SignalPlanResponse fetchSignalPlan(String intersectionId) {
        try {
            ApiResult<SignalPlanResponse> result = signalControlClient.getCurrentPlan(intersectionId);
            return result != null ? result.getData() : null;
        } catch (Exception e) {
            log.warn("Failed to fetch signal plan for {}: {}", intersectionId, e.getMessage());
            return null;
        }
    }

    static int actionToGreenTime(int action) {
        return (3 + action * 2) * GREEN_TIME_STEP;
    }
}
