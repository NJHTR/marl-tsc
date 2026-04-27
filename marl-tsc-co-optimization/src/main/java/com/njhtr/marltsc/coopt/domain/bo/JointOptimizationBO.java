package com.njhtr.marltsc.coopt.domain.bo;

import lombok.Data;
import java.util.List;

@Data
public class JointOptimizationBO {
    private String intersectionId;
    private SignalPlanBO currentSignalPlan;
    private PredictedNetworkStateBO predictedNetworkState;
    private SignalActionBO suggestedSignalAction;
    private List<RouteChangeBO> suggestedRouteChanges;
}
