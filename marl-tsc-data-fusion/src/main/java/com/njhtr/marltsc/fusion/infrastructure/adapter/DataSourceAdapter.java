package com.njhtr.marltsc.fusion.infrastructure.adapter;

import com.njhtr.marltsc.fusion.domain.entity.UnifiedTrafficData;
import com.njhtr.marltsc.fusion.domain.enums.DataSourceType;

public interface DataSourceAdapter {
    UnifiedTrafficData adapt(Object rawMessage);
    boolean supports(DataSourceType type);
}
