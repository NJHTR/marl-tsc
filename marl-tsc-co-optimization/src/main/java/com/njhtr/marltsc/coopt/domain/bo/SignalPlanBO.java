package com.njhtr.marltsc.coopt.domain.bo;

import lombok.Data;
import java.util.List;

@Data
public class SignalPlanBO {
    private String planId;
    private String intersectionId;
    private Integer cycleTime;
    private List<PhaseBO> phases;
}
