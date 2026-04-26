package com.njhtr.marltsc.signal.domain.entity;

import lombok.Data;

/**
 * Signal phase database entity.
 */
@Data
public class SignalPhase {

    private Integer phaseId;
    private String planId;
    private String direction;
    private Integer greenTime;
    private Integer yellowTime;
    private Integer redTime;
    private Integer sequence;
}
