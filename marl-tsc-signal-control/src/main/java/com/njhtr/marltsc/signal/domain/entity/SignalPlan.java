package com.njhtr.marltsc.signal.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Signal plan database entity.
 */
@Data
public class SignalPlan {

    private String planId;
    private String intersectionId;
    private String planName;
    private Integer cycleTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
