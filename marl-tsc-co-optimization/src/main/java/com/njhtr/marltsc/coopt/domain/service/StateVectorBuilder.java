package com.njhtr.marltsc.coopt.domain.service;

import org.springframework.stereotype.Component;

import java.time.LocalTime;

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
