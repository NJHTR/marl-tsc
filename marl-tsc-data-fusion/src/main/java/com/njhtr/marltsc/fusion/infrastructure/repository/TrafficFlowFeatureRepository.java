package com.njhtr.marltsc.fusion.infrastructure.repository;

import com.njhtr.marltsc.fusion.domain.bo.TrafficFlowFeature;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Repository
public class TrafficFlowFeatureRepository {

    private final List<TrafficFlowFeature> storage = new CopyOnWriteArrayList<>();

    public void save(TrafficFlowFeature feature) {
        storage.add(feature);
    }

    public List<TrafficFlowFeature> queryRecent(String intersectionId, int limit) {
        return storage.stream()
                .filter(f -> intersectionId.equals(f.getIntersectionId()))
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
