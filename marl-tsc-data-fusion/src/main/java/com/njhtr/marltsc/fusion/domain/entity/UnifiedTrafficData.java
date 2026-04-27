package com.njhtr.marltsc.fusion.domain.entity;

import com.njhtr.marltsc.fusion.domain.enums.DataSourceType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class UnifiedTrafficData {
    private String dataId;
    private DataSourceType sourceType;
    private Long timestamp;
    private GeoPointBO location;
    private String roadSegmentId;
    private String intersectionId;
    private Map<String, Object> features = new HashMap<>();
    private DataQualityBO quality;
    private LocalDateTime createTime = LocalDateTime.now();
}
