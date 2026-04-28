package com.njhtr.marltsc.common.dto;

import lombok.Data;

@Data
public class PhaseResponse {
    private Integer phaseId;
    private String direction;
    private Integer greenTime;
}
