package com.njhtr.marltsc.stream.model;

import lombok.Data;

@Data
public class TrafficFlowFeature {
    private String intersectionId;
    private Long timestamp;
    private Double flow;
    private Double speed;
    private Double occupancy;
    private Double queueLength;
    private Double delay;
}
