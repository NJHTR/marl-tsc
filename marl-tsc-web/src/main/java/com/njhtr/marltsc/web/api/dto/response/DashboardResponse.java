package com.njhtr.marltsc.web.api.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class DashboardResponse {
    private String intersectionId;
    private SignalPlanSummary signalPlan;
    private TrafficFeatureSummary trafficFeature;
    private String optimizationStatus;
    private Long lastUpdateTime;
    private List<RouteSummary> activeRoutes;
}
