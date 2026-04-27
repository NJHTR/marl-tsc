package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OptimizationResultResponse {
    private String logId;
    private String intersectionId;
    private LocalDateTime triggerTime;
    private Double jointReward;
    private SignalActionResponse signalAction;
    private Integer convergenceIterations;
}
