package com.njhtr.marltsc.fusion.domain.service;

import com.njhtr.marltsc.fusion.domain.entity.DataQualityBO;
import com.njhtr.marltsc.fusion.domain.entity.UnifiedTrafficData;
import com.njhtr.marltsc.fusion.domain.enums.DataSourceType;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DataQualityAssessor {

    private static final String[] EXPECTED_FEATURES = {"flow", "speed", "occupancy", "queueLength", "delay"};
    private static final long TIMELINESS_WINDOW_MS = 30000;

    public DataQualityBO assess(UnifiedTrafficData data) {
        double completeness = calculateCompleteness(data);
        double timeliness = calculateTimeliness(data);
        double accuracy = getAccuracyBySource(data.getSourceType());
        double confidence = completeness * 0.3 + timeliness * 0.3 + accuracy * 0.4;
        confidence = Math.min(1.0, Math.max(0.0, confidence));
        return new DataQualityBO(completeness, timeliness, accuracy, confidence);
    }

    public DataQualityBO assessBatch(List<UnifiedTrafficData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new DataQualityBO(0.0, 0.0, 0.0, 0.0);
        }
        double avgCompleteness = dataList.stream()
                .mapToDouble(this::calculateCompleteness).average().orElse(0);
        double avgTimeliness = dataList.stream()
                .mapToDouble(this::calculateTimeliness).average().orElse(0);
        double avgAccuracy = dataList.stream()
                .mapToDouble(d -> getAccuracyBySource(d.getSourceType())).average().orElse(0);
        double confidence = avgCompleteness * 0.3 + avgTimeliness * 0.3 + avgAccuracy * 0.4;
        confidence = Math.min(1.0, Math.max(0.0, confidence));
        return new DataQualityBO(avgCompleteness, avgTimeliness, avgAccuracy, confidence);
    }

    private double calculateCompleteness(UnifiedTrafficData data) {
        if (data.getFeatures() == null || data.getFeatures().isEmpty()) return 0.0;
        long presentCount = Arrays.stream(EXPECTED_FEATURES)
                .filter(f -> data.getFeatures().containsKey(f))
                .count();
        return (double) presentCount / EXPECTED_FEATURES.length;
    }

    private double calculateTimeliness(UnifiedTrafficData data) {
        if (data.getTimestamp() == null) return 0.0;
        long age = System.currentTimeMillis() - data.getTimestamp();
        return Math.max(0.0, 1.0 - (double) age / TIMELINESS_WINDOW_MS);
    }

    private double getAccuracyBySource(DataSourceType sourceType) {
        if (sourceType == null) return 0.5;
        return switch (sourceType) {
            case LOOP_DETECTOR -> 0.95;
            case VIDEO_DETECTOR -> 0.85;
            case RADAR_DETECTOR -> 0.90;
            case FLOATING_CAR -> 0.70;
            case SIGNAL_CONTROLLER -> 0.98;
            case WEATHER -> 0.80;
            case EVENT -> 0.60;
        };
    }
}
