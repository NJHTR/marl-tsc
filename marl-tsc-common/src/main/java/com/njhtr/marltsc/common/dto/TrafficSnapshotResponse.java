package com.njhtr.marltsc.common.dto;

import lombok.Data;

@Data
public class TrafficSnapshotResponse {
    private String intersectionId;
    private double flow;
    private double speed;
    private double occupancy;
    private double queueLength;
    private double delay;
    private long timestamp;
    private String congestionLevel;
}
