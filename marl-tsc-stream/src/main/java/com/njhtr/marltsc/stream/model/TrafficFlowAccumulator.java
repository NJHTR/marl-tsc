package com.njhtr.marltsc.stream.model;

import lombok.Data;

@Data
public class TrafficFlowAccumulator {
    private long count;
    private double sumFlow;
    private double sumSpeed;
    private double sumOccupancy;
    private double maxQueueLength;
    private long windowStart;
    private long windowEnd;
}
