package com.njhtr.marltsc.fusion.domain.service;

import com.njhtr.marltsc.fusion.domain.entity.UnifiedTrafficData;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SpatioTemporalAlignmentEngine {

    public List<UnifiedTrafficData> align(List<UnifiedTrafficData> rawData, long windowSizeMs, long stepSizeMs) {
        if (rawData == null || rawData.isEmpty()) {
            return Collections.emptyList();
        }

        long minTime = rawData.stream().mapToLong(UnifiedTrafficData::getTimestamp).min().orElse(0);
        long maxTime = rawData.stream().mapToLong(UnifiedTrafficData::getTimestamp).max().orElse(0);

        List<UnifiedTrafficData> result = new ArrayList<>();

        for (long windowStart = minTime; windowStart + windowSizeMs <= maxTime + stepSizeMs; windowStart += stepSizeMs) {
            long windowEnd = windowStart + windowSizeMs;
            long ws = windowStart;
            long we = windowEnd;

            List<UnifiedTrafficData> windowData = rawData.stream()
                    .filter(d -> d.getTimestamp() >= ws && d.getTimestamp() < we)
                    .collect(Collectors.toList());

            if (windowData.isEmpty()) continue;

            result.add(fuseWindow(windowData));
        }

        return result;
    }

    private UnifiedTrafficData fuseWindow(List<UnifiedTrafficData> windowData) {
        UnifiedTrafficData first = windowData.get(0);
        UnifiedTrafficData fused = new UnifiedTrafficData();
        fused.setDataId("fused_" + first.getDataId() + "_" + System.currentTimeMillis());
        fused.setSourceType(first.getSourceType());
        fused.setTimestamp(windowData.get(windowData.size() - 1).getTimestamp());
        fused.setLocation(first.getLocation());
        fused.setRoadSegmentId(first.getRoadSegmentId());
        fused.setIntersectionId(first.getIntersectionId());

        Set<String> allKeys = new HashSet<>();
        for (UnifiedTrafficData d : windowData) {
            if (d.getFeatures() != null) {
                allKeys.addAll(d.getFeatures().keySet());
            }
        }

        Map<String, Object> fusedFeatures = new HashMap<>();
        for (String key : allKeys) {
            double weightedSum = 0;
            double weightSum = 0;
            boolean allNumeric = true;
            boolean hasValue = false;
            for (UnifiedTrafficData d : windowData) {
                Object val = d.getFeatures() != null ? d.getFeatures().get(key) : null;
                if (val instanceof Number num) {
                    double weight = d.getQuality() != null ? d.getQuality().getConfidence() : 0.5;
                    weightedSum += num.doubleValue() * weight;
                    weightSum += weight;
                    hasValue = true;
                } else if (val != null) {
                    allNumeric = false;
                    break;
                }
            }
            if (allNumeric && weightSum > 0 && hasValue) {
                fusedFeatures.put(key, weightedSum / weightSum);
            } else if (!allNumeric && !windowData.isEmpty()) {
                Object lastVal = windowData.get(windowData.size() - 1).getFeatures().get(key);
                if (lastVal != null) {
                    fusedFeatures.put(key, lastVal);
                }
            }
        }
        fused.setFeatures(fusedFeatures);

        return fused;
    }
}
