package com.njhtr.marltsc.web.service.impl;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.api.dto.response.*;
import com.njhtr.marltsc.web.infrastructure.client.*;
import com.njhtr.marltsc.web.infrastructure.client.dto.*;
import com.njhtr.marltsc.web.service.api.DashboardAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAppServiceImpl implements DashboardAppService {

    private final SignalControlClient signalControlClient;
    private final RoutePlanningClient routePlanningClient;
    private final CoOptimizationClient coOptimizationClient;
    private final DataFusionClient dataFusionClient;
    private final DrlEngineClient drlEngineClient;

    @Override
    public DashboardResponse getDashboardData(String intersectionId) {
        DashboardResponse response = new DashboardResponse();
        response.setIntersectionId(intersectionId);
        response.setLastUpdateTime(System.currentTimeMillis());

        // 1. Fetch signal plan
        fetchSignalPlan(intersectionId).ifPresent(response::setSignalPlan);

        // 2. Fetch traffic features
        fetchTrafficFeature(intersectionId).ifPresent(response::setTrafficFeature);

        // 3. Fetch optimization status
        fetchOptimizationStatus(intersectionId).ifPresent(response::setOptimizationStatus);

        // 4. Fetch active routes (placeholder)
        response.setActiveRoutes(new ArrayList<>());

        return response;
    }

    @Override
    public List<IntersectionSummaryResponse> listIntersections() {
        List<IntersectionSummaryResponse> list = new ArrayList<>();

        addIntersection(list, "INT-001", "中央大道与解放路交叉口", "正常", "低");
        addIntersection(list, "INT-002", "人民路与建设路交叉口", "繁忙", "中");
        addIntersection(list, "INT-003", "长江路与黄河路交叉口", "拥堵", "高");
        addIntersection(list, "INT-004", "南京路与中山路交叉口", "正常", "低");

        return list;
    }

    @Override
    public OptimizeResponse triggerOptimization(String intersectionId, String strategy) {
        OptimizeResponse resp = new OptimizeResponse();
        try {
            OptimizationTriggerRequest req = new OptimizationTriggerRequest();
            req.setIntersectionId(intersectionId);
            if (strategy != null) req.setStrategy(strategy);

            ApiResult<OptimizationResultResponse> result = coOptimizationClient.triggerOptimize(req);

            if (result != null && result.getData() != null) {
                resp.setSuccess(true);
                resp.setResultId(result.getData().getLogId());
                resp.setEstimatedTime(result.getData().getConvergenceIterations());
                resp.setMessage("优化完成");
            } else {
                resp.setSuccess(false);
                resp.setMessage("优化未返回结果: " + (result != null ? result.getMessage() : "null"));
            }
        } catch (Exception e) {
            log.warn("Failed to trigger optimization for {}: {}", intersectionId, e.getMessage());
            resp.setSuccess(false);
            resp.setMessage("优化服务不可用: " + e.getMessage());
        }
        return resp;
    }

    private Optional<SignalPlanSummary> fetchSignalPlan(String intersectionId) {
        try {
            ApiResult<SignalPlanResponse> result = signalControlClient.getPlan(intersectionId);
            if (result == null || result.getData() == null) return Optional.empty();

            SignalPlanResponse plan = result.getData();
            SignalPlanSummary summary = new SignalPlanSummary();
            summary.setPlanId(plan.getPlanId());
            summary.setCycleTime(plan.getCycleTime());
            summary.setCurrentPhase(1);

            if (plan.getPhases() != null) {
                List<PhaseSummary> phases = plan.getPhases().stream().map(p -> {
                    PhaseSummary ps = new PhaseSummary();
                    ps.setPhaseId(p.getPhaseId());
                    ps.setDirection(p.getDirection());
                    ps.setGreenTime(p.getGreenTime());
                    ps.setStatus("active");
                    return ps;
                }).toList();
                summary.setPhases(phases);
            }
            return Optional.of(summary);
        } catch (Exception e) {
            log.warn("Failed to fetch signal plan for {}: {}", intersectionId, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<TrafficFeatureSummary> fetchTrafficFeature(String intersectionId) {
        try {
            ApiResult<TrafficFeatureResponse> result = dataFusionClient.getFeature(intersectionId);
            if (result == null || result.getData() == null) return Optional.empty();

            TrafficFeatureResponse feature = result.getData();
            TrafficFeatureSummary summary = new TrafficFeatureSummary();
            summary.setFlow(feature.getFlow());
            summary.setSpeed(feature.getSpeed());
            summary.setOccupancy(feature.getOccupancy());
            summary.setQueueLength(feature.getQueueLength());
            summary.setDelay(feature.getDelay());
            return Optional.of(summary);
        } catch (Exception e) {
            log.warn("Failed to fetch traffic feature for {}: {}", intersectionId, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> fetchOptimizationStatus(String intersectionId) {
        try {
            ApiResult<OptimizationResultResponse> result = coOptimizationClient.getResult(intersectionId);
            if (result != null && result.getData() != null) {
                return Optional.of("已优化: reward=" + result.getData().getJointReward());
            }
            return Optional.of("待优化");
        } catch (Exception e) {
            log.warn("Failed to fetch optimization status for {}: {}", intersectionId, e.getMessage());
            return Optional.empty();
        }
    }

    private void addIntersection(List<IntersectionSummaryResponse> list, String id, String name,
                                  String status, String congestionLevel) {
        IntersectionSummaryResponse r = new IntersectionSummaryResponse();
        r.setIntersectionId(id);
        r.setName(name);
        r.setStatus(status);
        r.setCongestionLevel(congestionLevel);
        r.setLastUpdateTime(System.currentTimeMillis());
        list.add(r);
    }
}
