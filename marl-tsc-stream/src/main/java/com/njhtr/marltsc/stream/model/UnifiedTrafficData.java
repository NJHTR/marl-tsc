package com.njhtr.marltsc.stream.model;

import lombok.Data;

import java.util.Map;

@Data
public class UnifiedTrafficData {
    private String dataId;
    private String sourceType;
    private Long timestamp;
    private String intersectionId;
    private String roadSegmentId;
    private Map<String, Object> features;
}
