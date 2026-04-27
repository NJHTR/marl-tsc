package com.njhtr.marltsc.coopt.infrastructure.client.dto;

import lombok.Data;

@Data
public class PhaseResponse {
    private String phaseId;
    private String direction;
    private Integer greenTime;
}
