package com.njhtr.marltsc.coopt.infrastructure.client.dto;

import lombok.Data;

@Data
public class PhaseAdjustRequest {
    private String intersectionId;
    private Integer phaseId;
    private Integer greenTime;
}
