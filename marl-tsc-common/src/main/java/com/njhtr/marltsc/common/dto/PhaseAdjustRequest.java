package com.njhtr.marltsc.common.dto;

import lombok.Data;

@Data
public class PhaseAdjustRequest {

    private String planId;
    private Integer phaseId;
    private String intersectionId;
    private Integer suggestedGreenTime;

    /** Current traffic metrics — fed to the DRL agent as state observation */
    private Double flow;
    private Double speed;
    private Double occupancy;
    private Double queueLength;
    private Double delay;
}
