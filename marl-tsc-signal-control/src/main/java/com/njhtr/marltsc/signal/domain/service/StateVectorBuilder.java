package com.njhtr.marltsc.signal.domain.service;

import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Builds 8-dimensional state vectors for the DRL agent from raw traffic metrics.
 *
 * <p>The DRL engine expects stateSize=8. We use 5 traffic features + 3 contextual features.</p>
 * <ul>
 *   <li>[0] flow normalized by capacity (0~1)</li>
 *   <li>[1] speed normalized by free-flow speed (0~1)</li>
 *   <li>[2] occupancy (0~1)</li>
 *   <li>[3] queueLength normalized (0~1)</li>
 *   <li>[4] delay normalized (0~1)</li>
 *   <li>[5] sin(hour * 2π / 24)</li>
 *   <li>[6] cos(hour * 2π / 24)</li>
 *   <li>[7] current green ratio (greenTime / cycleTime)</li>
 * </ul>
 */
@Component
public class StateVectorBuilder {

    private static final double MAX_FLOW = 2000.0;
    private static final double MAX_SPEED = 60.0;
    private static final double MAX_QUEUE = 200.0;
    private static final double MAX_DELAY = 100.0;

    public double[] build(double flow, double speed, double occupancy,
                          double queueLength, double delay,
                          int currentGreenTime, int cycleTime) {
        double[] state = new double[8];

        state[0] = clamp(flow / MAX_FLOW, 0, 1);
        state[1] = clamp(speed / MAX_SPEED, 0, 1);
        state[2] = clamp(occupancy, 0, 1);
        state[3] = clamp(queueLength / MAX_QUEUE, 0, 1);
        state[4] = clamp(delay / MAX_DELAY, 0, 1);

        LocalTime now = LocalTime.now();
        double hourAngle = (now.getHour() + now.getMinute() / 60.0) * 2 * Math.PI / 24;
        state[5] = Math.sin(hourAngle);
        state[6] = Math.cos(hourAngle);

        state[7] = cycleTime > 0 ? clamp((double) currentGreenTime / cycleTime, 0, 1) : 0;

        return state;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
