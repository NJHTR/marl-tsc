package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.Data;
import java.util.List;

@Data
public class SignalPlanResponse {
    private String planId;
    private String intersectionId;
    private String planName;
    private Integer cycleTime;
    private List<PhaseResponse> phases;
}
