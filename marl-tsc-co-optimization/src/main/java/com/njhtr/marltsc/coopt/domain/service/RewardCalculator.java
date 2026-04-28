package com.njhtr.marltsc.coopt.domain.service;

import org.springframework.stereotype.Component;

@Component
public class RewardCalculator {

    private static final double MAX_FLOW = 2000.0;
    private static final double MAX_SPEED = 60.0;

    public double compute(double flow, double speed, double occupancy,
                          double queueLength, double delay) {
        double throughputReward = 0.3 * (flow / MAX_FLOW);
        double speedReward = 0.2 * (speed / MAX_SPEED);
        double occupancyPenalty = -0.2 * occupancy;
        double queuePenalty = -0.15 * Math.min(1.0, queueLength / 200.0);
        double delayPenalty = -0.15 * Math.min(1.0, delay / 100.0);

        return clamp(throughputReward + speedReward + occupancyPenalty + queuePenalty + delayPenalty, -1, 1);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
