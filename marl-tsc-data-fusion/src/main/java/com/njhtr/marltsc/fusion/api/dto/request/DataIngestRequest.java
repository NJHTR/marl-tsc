package com.njhtr.marltsc.fusion.api.dto.request;

import com.njhtr.marltsc.fusion.domain.enums.DataSourceType;
import lombok.Data;

import java.util.Map;

@Data
public class DataIngestRequest {
    private DataSourceType sourceType;
    private Map<String, Object> rawData;
    private Long timestamp;
    private String intersectionId;
}
