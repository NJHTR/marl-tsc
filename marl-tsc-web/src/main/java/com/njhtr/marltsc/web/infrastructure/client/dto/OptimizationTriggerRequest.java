package com.njhtr.marltsc.web.infrastructure.client.dto;

import lombok.Data;

@Data
public class OptimizationTriggerRequest {
    private String intersectionId;
    private String strategy = "joint";
}
