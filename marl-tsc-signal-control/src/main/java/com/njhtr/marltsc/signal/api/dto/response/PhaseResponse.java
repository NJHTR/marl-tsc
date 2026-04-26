package com.njhtr.marltsc.signal.api.dto.response;

import lombok.Data;

/**
 * Phase response DTO.
 */
@Data
public class PhaseResponse {

    private Integer phaseId;
    private String direction;
    private Integer greenTime;
    private Integer yellowTime;
    private Integer redTime;
}
