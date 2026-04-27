package com.njhtr.marltsc.fusion.infrastructure.adapter;

import com.njhtr.marltsc.common.exception.BusinessException;
import com.njhtr.marltsc.common.exception.ErrorCode;
import com.njhtr.marltsc.fusion.domain.enums.DataSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSourceAdapterFactory {

    private final List<DataSourceAdapter> adapters;

    public DataSourceAdapter getAdapter(DataSourceType type) {
        return adapters.stream()
                .filter(a -> a.supports(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "No adapter found for data source type: " + type));
    }
}
