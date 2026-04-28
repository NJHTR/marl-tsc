package com.njhtr.marltsc.signal.domain.service;

import org.springframework.stereotype.Component;

/**
 * Computes reward signal for DRL training based on traffic metrics change.
 *
 * <p>Reward is higher when: flow is high, speed is high, occupancy/queue/delay are low.</p>
 * <p>Range approximately [-1, 1], normalized for stable Q-learning.</p>
 */
@Component
public class RewardCalculator {

    private static final double MAX_FLOW = 2000.0;
    private static final double MAX_SPEED = 60.0;

    /**
     * Compute reward from absolute traffic state (used when no previous state is available).
     */
    public double compute(double flow, double speed, double occupancy,
                          double queueLength, double delay) {
        double throughputReward = 0.3 * (flow / MAX_FLOW);
        double speedReward = 0.2 * (speed / MAX_SPEED);
        double occupancyPenalty = -0.2 * occupancy;
        double queuePenalty = -0.15 * Math.min(1.0, queueLength / 200.0);
        double delayPenalty = -0.15 * Math.min(1.0, delay / 100.0);

        return clamp(throughputReward + speedReward + occupancyPenalty + queuePenalty + delayPenalty, -1, 1);
    }

    /**
     * Compute reward as improvement over previous state — the agent is rewarded for making things better.
     */
    public double computeDelta(double prevOccupancy, double prevQueue, double prevDelay,
                               double currOccupancy, double currQueue, double currDelay) {
        double occImprove = prevOccupancy - currOccupancy;
        double queueImprove = (prevQueue - currQueue) / 200.0;
        double delayImprove = (prevDelay - currDelay) / 100.0;

        return clamp(0.4 * occImprove + 0.3 * queueImprove + 0.3 * delayImprove, -1, 1);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
