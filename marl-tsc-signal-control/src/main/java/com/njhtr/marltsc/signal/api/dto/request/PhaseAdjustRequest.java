package com.njhtr.marltsc.signal.api.dto.request;

import lombok.Data;

/**
 * Phase adjust request DTO — carries traffic state for DRL-driven optimization.
 */
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
