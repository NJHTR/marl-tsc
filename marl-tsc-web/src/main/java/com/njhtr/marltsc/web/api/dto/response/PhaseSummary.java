package com.njhtr.marltsc.web.api.dto.response;

import lombok.Data;

@Data
public class PhaseSummary {
    private Integer phaseId;
    private String direction;
    private Integer greenTime;
    private String status;
}
