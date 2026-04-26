package com.njhtr.marltsc.signal.api.dto.request;

import lombok.Data;

/**
 * Phase adjust request DTO.
 */
@Data
public class PhaseAdjustRequest {

    private String planId;
    private Integer phaseId;
    private Integer suggestedGreenTime;
}
