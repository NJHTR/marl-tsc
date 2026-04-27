package com.njhtr.marltsc.fusion.api.dto.response;

import lombok.Data;

@Data
public class TrafficFeatureResponse {
    private String intersectionId;
    private Long timestamp;
    private Double flow;
    private Double speed;
    private Double occupancy;
    private Double queueLength;
    private Double delay;
}
