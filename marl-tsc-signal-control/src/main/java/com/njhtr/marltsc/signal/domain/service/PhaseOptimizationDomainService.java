package com.njhtr.marltsc.signal.domain.service;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase optimization domain service.
 *
 * <p>Pure logic service for signal timing calculations based on traffic flow data.</p>
 */
@Component
public class PhaseOptimizationDomainService {

    /**
     * Calculate green ratio based on Webster's method.
     *
     * <p>Formula: g = (y / Y) * (C - L)</p>
     * <p>where y is flow ratio, Y is total flow ratio, C is cycle time, L is total lost time</p>
     *
     * @param flowRatios list of flow ratios for each phase
     * @param cycleTime signal cycle time in seconds
     * @param lostTime total lost time in seconds
     * @return calculated green ratio
     */
    public double calculateGreenRatio(List<Double> flowRatios, int cycleTime, int lostTime) {
        if (flowRatios == null || flowRatios.isEmpty() || cycleTime <= 0) {
            return 0.0;
        }

        double totalFlowRatio = flowRatios.stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalFlowRatio <= 0) {
            return 0.0;
        }

        double maxFlowRatio = flowRatios.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        // Webster's green ratio formula: g = (y / Y) * (C - L)
        return (maxFlowRatio / totalFlowRatio) * (cycleTime - lostTime);
    }
}
