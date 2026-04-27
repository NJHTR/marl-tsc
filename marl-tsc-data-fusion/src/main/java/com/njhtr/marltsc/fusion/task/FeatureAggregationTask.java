package com.njhtr.marltsc.fusion.task;

import com.njhtr.marltsc.fusion.service.api.DataFusionAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureAggregationTask {

    private final DataFusionAppService dataFusionAppService;

    @Scheduled(fixedRate = 5000)
    public void aggregateFeatures() {
        dataFusionAppService.aggregateFeatures();
    }
}
