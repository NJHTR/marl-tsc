package com.njhtr.marltsc.coopt.infrastructure.client.dto;

import lombok.Data;

@Data
public class PhaseAdjustRequest {
    private String planId;
    private String intersectionId;
    private Integer phaseId;
    private Integer suggestedGreenTime;
}
