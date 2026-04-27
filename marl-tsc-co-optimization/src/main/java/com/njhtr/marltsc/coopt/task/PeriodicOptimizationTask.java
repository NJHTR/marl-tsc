package com.njhtr.marltsc.coopt.task;

import com.njhtr.marltsc.coopt.service.api.CoOptimizationAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PeriodicOptimizationTask {

    private final CoOptimizationAppService coOptimizationAppService;

    @Scheduled(fixedRate = 5000)
    public void executePeriodicOptimization() {
        log.debug("Executing periodic optimization");
        // For now, optimize a default intersection
        // In production, this would iterate over all active intersections
        coOptimizationAppService.triggerOptimization("ALL");
    }
}
