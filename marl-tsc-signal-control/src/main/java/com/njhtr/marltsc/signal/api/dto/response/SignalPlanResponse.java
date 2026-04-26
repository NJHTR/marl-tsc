package com.njhtr.marltsc.signal.api.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Signal plan response DTO.
 */
@Data
public class SignalPlanResponse {

    private String planId;
    private String intersectionId;
    private String planName;
    private Integer cycleTime;
    private List<PhaseResponse> phases;
}
