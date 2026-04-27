package com.njhtr.marltsc.fusion.infrastructure.adapter;

import com.njhtr.marltsc.fusion.domain.entity.GeoPointBO;
import com.njhtr.marltsc.fusion.domain.entity.UnifiedTrafficData;
import com.njhtr.marltsc.fusion.domain.enums.DataSourceType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class LoopDetectorAdapter implements DataSourceAdapter {

    @Override
    public UnifiedTrafficData adapt(Object rawMessage) {
        Map<String, Object> raw = (Map<String, Object>) rawMessage;
        UnifiedTrafficData data = new UnifiedTrafficData();
        data.setDataId((String) raw.getOrDefault("dataId", UUID.randomUUID().toString()));
        data.setSourceType(DataSourceType.LOOP_DETECTOR);
        data.setTimestamp(toLong(raw.get("timestamp"), System.currentTimeMillis()));
        if (raw.containsKey("longitude") && raw.containsKey("latitude")) {
            data.setLocation(new GeoPointBO(
                    toDouble(raw.get("longitude")),
                    toDouble(raw.get("latitude"))
            ));
        }
        data.setRoadSegmentId((String) raw.get("roadSegmentId"));
        data.setIntersectionId((String) raw.get("intersectionId"));
        data.setFeatures(raw);
        return data;
    }

    @Override
    public boolean supports(DataSourceType type) {
        return type == DataSourceType.LOOP_DETECTOR;
    }

    private Long toLong(Object value, Long defaultValue) {
        if (value instanceof Number n) return n.longValue();
        return defaultValue;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}
