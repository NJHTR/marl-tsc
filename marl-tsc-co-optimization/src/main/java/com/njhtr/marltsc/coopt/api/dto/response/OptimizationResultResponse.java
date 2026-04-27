package com.njhtr.marltsc.coopt.api.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OptimizationResultResponse {
    private String logId;
    private String intersectionId;
    private LocalDateTime triggerTime;
    private Double jointReward;
    private SignalActionResponse signalAction;
    private List<RouteAdjustmentResponse> routeAdjustments;
    private Integer convergenceIterations;
}
