package com.njhtr.marltsc.fusion.domain.service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 交通流模拟引擎
 *
 * <p>基于真实城市交通的日变化规律生成逼真的交通数据。
 * 日流量曲线参考实际城市主干道检测器数据：早高峰7-9点、晚高峰17-19点。
 * 可以替换为从开放数据集（PeMS/OpenITS）导入的实时数据。 </p>
 */
@Slf4j
@Service
public class TrafficSimulationEngine {

    private static final List<String> DEFAULT_INTERSECTIONS = List.of(
            "INT-001", "INT-002", "INT-003", "INT-004",
            "INT-005", "INT-006", "INT-007", "INT-008", "INT-009"
    );

    // Road capacity per intersection (vehicles/hour)
    private static final Map<String, Integer> ROAD_CAPACITY = new HashMap<>();

    // Root mean square noise factor for realistic variation
    private static final double NOISE_STD = 0.08;

    @Data
    public static class TrafficSnapshot {
        private final String intersectionId;
        private final double flow;          // vehicles/hour
        private final double speed;         // km/h
        private final double occupancy;     // 0~1
        private final double queueLength;   // meters
        private final double delay;         // seconds
        private final LocalDateTime timestamp;
        private final String congestionLevel;

        public TrafficSnapshot(String id, double flow, double speed, double occupancy,
                               double queue, double delay, LocalDateTime ts) {
            this.intersectionId = id;
            this.flow = Math.round(flow * 10) / 10.0;
            this.speed = Math.round(speed * 10) / 10.0;
            this.occupancy = Math.min(1.0, Math.round(occupancy * 1000) / 1000.0);
            this.queueLength = Math.round(queue * 10) / 10.0;
            this.delay = Math.round(delay * 10) / 10.0;
            this.timestamp = ts;
            this.congestionLevel = deriveLevel(this.occupancy);
        }
    }

    @PostConstruct
    public void init() {
        // Base capacity: arterial roads ~1800 veh/h/lane, 3 lanes
        for (String id : DEFAULT_INTERSECTIONS) {
            int idx = Integer.parseInt(id.replace("INT-", ""));
            ROAD_CAPACITY.put(id, 1500 + (idx % 5) * 100); // 1500~1900
        }
        log.info("TrafficSimulationEngine initialized with {} intersections", DEFAULT_INTERSECTIONS.size());
    }

    /**
     * 日流量系数: 基于真实城市主干道24小时流量曲线
     * 返回值 0~1，表示当前时间相对于道路容量的负载系数
     */
    public static double diurnalFactor(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        double t = hour + minute / 60.0;

        // Diurnal curve: piecewise linear approximation from real traffic data
        if (t < 5) return 0.08;                              // 0:00-5:00 深夜
        if (t < 6) return 0.08 + 0.12 * (t - 5);             // 5:00-6:00 上升
        if (t < 7) return 0.20 + 0.25 * (t - 6);             // 6:00-7:00 早高峰前
        if (t < 9) return 0.45 + 0.22 * (t - 7);             // 7:00-9:00 早高峰 → 0.67~0.89
        if (t < 12) return 0.85 - 0.08 * (t - 9);            // 9:00-12:00 回落 → 0.85~0.61
        if (t < 14) return 0.55 - 0.05 * (t - 12);           // 12:00-14:00 午间谷 → 0.55~0.45
        if (t < 17) return 0.45 + 0.10 * (t - 14);           // 14:00-17:00 回升 → 0.45~0.75
        if (t < 19) return 0.75 + 0.07 * (t - 17);           // 17:00-19:00 晚高峰 → 0.75~0.89
        if (t < 21) return 0.85 - 0.10 * (t - 19);           // 19:00-21:00 下降 → 0.85~0.65
        if (t < 23) return 0.45 - 0.15 * (t - 21);           // 21:00-23:00 → 0.45~0.15
        return 0.12;                                          // 23:00-24:00
    }

    /**
     * 根据当前时间生成所有路口的交通快照
     */
    public List<TrafficSnapshot> generateAll(LocalDateTime now) {
        return DEFAULT_INTERSECTIONS.stream()
                .map(id -> generate(id, now))
                .collect(Collectors.toList());
    }

    /**
     * 生成单个路口的交通数据
     */
    public TrafficSnapshot generate(String intersectionId, LocalDateTime now) {
        double baseFactor = diurnalFactor(now.toLocalTime());
        int capacity = ROAD_CAPACITY.getOrDefault(intersectionId, 1600);

        // Add spatial variation: each intersection has slightly different pattern
        int idNum = Integer.parseInt(intersectionId.replace("INT-", ""));
        double spatialOffset = ((idNum % 7) - 3) * 0.03;  // -0.09 ~ +0.09
        double factor = Math.max(0.02, Math.min(0.95, baseFactor + spatialOffset));

        // Add realistic noise (Gaussian)
        double noise = ThreadLocalRandom.current().nextGaussian() * NOISE_STD;
        factor = Math.max(0.02, Math.min(0.95, factor + noise));

        // Derive traffic metrics
        double flow = factor * capacity;
        double speed = freeFlowSpeed(factor);
        double occupancy = occupancyFromFlow(factor);
        double queueLength = queueFromOccupancy(occupancy, capacity);
        double delay = delayFromOccupancy(occupancy);

        return new TrafficSnapshot(intersectionId, flow, speed, occupancy, queueLength, delay, now);
    }

    /**
     * 速度-流量关系 (BPR函数变体): 流量增大速度下降
     */
    private static double freeFlowSpeed(double factor) {
        double baseSpeed = 55 + ThreadLocalRandom.current().nextDouble() * 10; // 55~65 km/h
        if (factor < 0.3) return baseSpeed;
        if (factor < 0.6) return baseSpeed * (1 - 0.3 * (factor - 0.3) / 0.3);
        if (factor < 0.85) return baseSpeed * (0.7 - 0.4 * (factor - 0.6) / 0.25);
        return baseSpeed * (0.3 - 0.2 * (factor - 0.85) / 0.1);
    }

    /**
     * 占用率-流量关系
     */
    private static double occupancyFromFlow(double factor) {
        if (factor < 0.1) return factor * 2.5;          // 低流量线性
        if (factor < 0.5) return 0.25 + (factor - 0.1) * 0.75;
        if (factor < 0.8) return 0.55 + (factor - 0.5) * 1.0;
        return 0.85 + (factor - 0.8) * 0.5;              // 接近饱和
    }

    private static double queueFromOccupancy(double occ, int capacity) {
        if (occ < 0.3) return occ * 60;                  // 0~18m
        if (occ < 0.6) return 18 + (occ - 0.3) * 120;   // 18~54m
        if (occ < 0.85) return 54 + (occ - 0.6) * 200;  // 54~104m
        return 104 + (occ - 0.85) * 300;                 // 104~149m
    }

    private static double delayFromOccupancy(double occ) {
        if (occ < 0.3) return occ * 20;
        if (occ < 0.6) return 6 + (occ - 0.3) * 40;
        if (occ < 0.85) return 18 + (occ - 0.6) * 100;
        return 43 + (occ - 0.85) * 200;
    }

    private static String deriveLevel(double occ) {
        if (occ > 0.8) return "严重拥堵";
        if (occ > 0.6) return "拥堵";
        if (occ > 0.4) return "缓行";
        if (occ > 0.2) return "基本畅通";
        return "畅通";
    }
}
