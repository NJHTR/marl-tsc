package com.njhtr.marltsc.coopt.infrastructure.client.dto;

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
