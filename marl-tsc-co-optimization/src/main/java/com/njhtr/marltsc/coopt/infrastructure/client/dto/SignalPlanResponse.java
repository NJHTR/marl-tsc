package com.njhtr.marltsc.coopt.infrastructure.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class SignalPlanResponse {
    private String planId;
    private String intersectionId;
    private Integer cycleTime;
    private List<PhaseResponse> phases;
}
