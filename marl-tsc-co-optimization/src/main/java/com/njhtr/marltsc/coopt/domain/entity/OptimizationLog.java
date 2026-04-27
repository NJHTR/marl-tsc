package com.njhtr.marltsc.coopt.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OptimizationLog {
    private String logId;
    private LocalDateTime triggerTime;
    private String intersectionId;
    private Double jointReward;
    private String signalActionJson;
    private String routeActionJson;
    private Integer status;
}
