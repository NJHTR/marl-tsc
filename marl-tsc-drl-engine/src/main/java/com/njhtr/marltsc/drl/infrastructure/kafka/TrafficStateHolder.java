package com.njhtr.marltsc.drl.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the latest traffic features per intersection, updated by Kafka consumption.
 * Provides a real-time data source for DRL agents without polling REST endpoints.
 */
@Slf4j
@Component
public class TrafficStateHolder {

    private final Map<String, TrafficFeatureSnapshot> latestState = new ConcurrentHashMap<>();

    public void update(String intersectionId, TrafficFeatureSnapshot snapshot) {
        latestState.put(intersectionId, snapshot);
    }

    public TrafficFeatureSnapshot get(String intersectionId) {
        return latestState.get(intersectionId);
    }

    public Map<String, TrafficFeatureSnapshot> getAll() {
        return Map.copyOf(latestState);
    }

    public int size() {
        return latestState.size();
    }

    public static class TrafficFeatureSnapshot {
        private double flow;
        private double speed;
        private double occupancy;
        private double queueLength;
        private double delay;
        private long timestamp;

        public TrafficFeatureSnapshot() {}

        public TrafficFeatureSnapshot(double flow, double speed, double occupancy,
                                       double queueLength, double delay, long timestamp) {
            this.flow = flow;
            this.speed = speed;
            this.occupancy = occupancy;
            this.queueLength = queueLength;
            this.delay = delay;
            this.timestamp = timestamp;
        }

        public double getFlow() { return flow; }
        public double getSpeed() { return speed; }
        public double getOccupancy() { return occupancy; }
        public double getQueueLength() { return queueLength; }
        public double getDelay() { return delay; }
        public long getTimestamp() { return timestamp; }
    }
}
