package com.njhtr.marltsc.web.service.impl;

import com.njhtr.marltsc.common.dto.IntersectionInfoResponse;
import com.njhtr.marltsc.common.dto.SignalPlanResponse;
import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.web.api.dto.response.*;
import com.njhtr.marltsc.web.infrastructure.client.*;
import com.njhtr.marltsc.web.infrastructure.client.dto.*;
import com.njhtr.marltsc.web.service.api.DashboardAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

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

        fetchSignalPlan(intersectionId).ifPresent(response::setSignalPlan);
        fetchTrafficFeature(intersectionId).ifPresent(response::setTrafficFeature);
        fetchOptimizationStatus(intersectionId).ifPresent(response::setOptimizationStatus);
        response.setActiveRoutes(fetchActiveRoutes(intersectionId));

        return response;
    }

    @Override
    public List<IntersectionSummaryResponse> listIntersections() {
        try {
            ApiResult<List<IntersectionInfoResponse>> result = dataFusionClient.listIntersections();
            if (result != null && result.getData() != null) {
                return result.getData().stream()
                        .map(this::toSummary)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch intersections from data-fusion: {}", e.getMessage());
        }
        return Collections.emptyList();
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

    private IntersectionSummaryResponse toSummary(IntersectionInfoResponse info) {
        IntersectionSummaryResponse r = new IntersectionSummaryResponse();
        r.setIntersectionId(info.getIntersectionId());
        r.setName(info.getName());
        r.setStatus("正常");
        r.setCongestionLevel("未知");
        r.setLastUpdateTime(System.currentTimeMillis());
        return r;
    }

    private List<RouteSummary> fetchActiveRoutes(String intersectionId) {
        try {
            ApiResult<Map<String, Object>> result = routePlanningClient.getNetworkStatus();
            if (result == null || result.getData() == null) return Collections.emptyList();

            List<RouteSummary> routes = new ArrayList<>();
            result.getData().forEach((segId, segData) -> {
                RouteSummary rs = new RouteSummary();
                rs.setRouteId(segId);
                rs.setStatus("active");
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) segData;
                Object travelTime = data.get("travelTime");
                if (travelTime instanceof Number) {
                    rs.setEstimatedTime(((Number) travelTime).doubleValue());
                }
                routes.add(rs);
            });
            return routes;
        } catch (Exception e) {
            log.warn("Failed to fetch route network status: {}", e.getMessage());
            return Collections.emptyList();
        }
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
}
