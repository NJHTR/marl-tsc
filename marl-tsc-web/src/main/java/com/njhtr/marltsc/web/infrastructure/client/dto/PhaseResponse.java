package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.Data;

@Data
public class PhaseResponse {
    private Integer phaseId;
    private String direction;
    private Integer greenTime;
    private Integer yellowTime;
    private Integer redTime;
}
