package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.Data;

@Data
public class PhaseAdjustRequest {
    private String planId;
    private Integer phaseId;
    private Integer suggestedGreenTime;
}
