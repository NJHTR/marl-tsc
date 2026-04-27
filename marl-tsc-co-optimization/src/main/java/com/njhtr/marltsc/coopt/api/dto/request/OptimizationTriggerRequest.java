package com.njhtr.marltsc.coopt.api.dto.request;

import lombok.Data;

@Data
public class OptimizationTriggerRequest {
    private String intersectionId;
    private String strategy = "joint"; // default value
}
