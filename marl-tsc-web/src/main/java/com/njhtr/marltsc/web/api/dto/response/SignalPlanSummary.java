package com.njhtr.marltsc.web.api.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class SignalPlanSummary {
    private String planId;
    private Integer cycleTime;
    private Integer currentPhase;
    private List<PhaseSummary> phases;
}
