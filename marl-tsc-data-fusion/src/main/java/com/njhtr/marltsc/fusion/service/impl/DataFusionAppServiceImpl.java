package com.njhtr.marltsc.fusion.service.impl;

import com.njhtr.marltsc.fusion.api.dto.request.DataIngestRequest;
import com.njhtr.marltsc.fusion.domain.bo.StateVectorBO;
import com.njhtr.marltsc.fusion.domain.bo.TrafficFlowFeature;
import com.njhtr.marltsc.fusion.domain.entity.UnifiedTrafficData;
import com.njhtr.marltsc.fusion.domain.service.DataQualityAssessor;
import com.njhtr.marltsc.fusion.domain.service.FeatureEngineeringEngine;
import com.njhtr.marltsc.fusion.domain.service.SpatioTemporalAlignmentEngine;
import com.njhtr.marltsc.fusion.infrastructure.adapter.DataSourceAdapterFactory;
import com.njhtr.marltsc.fusion.infrastructure.publisher.KafkaTrafficDataPublisher;
import com.njhtr.marltsc.fusion.infrastructure.repository.TrafficFlowFeatureRepository;
import com.njhtr.marltsc.fusion.service.api.DataFusionAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataFusionAppServiceImpl implements DataFusionAppService {

    private final DataSourceAdapterFactory adapterFactory;
    private final DataQualityAssessor qualityAssessor;
    private final SpatioTemporalAlignmentEngine alignmentEngine;
    private final FeatureEngineeringEngine featureEngine;
    private final KafkaTrafficDataPublisher publisher;
    private final TrafficFlowFeatureRepository featureRepository;

    private final Map<String, List<UnifiedTrafficData>> buffer = new ConcurrentHashMap<>();
    private final Map<String, TrafficFlowFeature> latestFeatures = new ConcurrentHashMap<>();
    private final Map<String, StateVectorBO> latestStateVectors = new ConcurrentHashMap<>();

    private static final long WINDOW_SIZE_MS = 5000;
    private static final long STEP_SIZE_MS = 2000;

    @Override
    public void ingestRawData(DataIngestRequest request) {
        // 1. Adapt raw data to unified format
        var adapter = adapterFactory.getAdapter(request.getSourceType());
        UnifiedTrafficData data = adapter.adapt(request.getRawData());

        // Override with request-level fields if provided
        if (request.getTimestamp() != null) {
            data.setTimestamp(request.getTimestamp());
        }
        if (request.getIntersectionId() != null) {
            data.setIntersectionId(request.getIntersectionId());
        }

        // 2. Assess quality
        var quality = qualityAssessor.assess(data);
        data.setQuality(quality);

        // 3. Buffer for windowed alignment
        String intersectionId = data.getIntersectionId();
        if (intersectionId != null) {
            buffer.computeIfAbsent(intersectionId, k -> new ArrayList<>()).add(data);
        }

        // 4. Publish raw data to Kafka
        publisher.publishRawData(data);

        log.debug("Ingested data from source: {}, intersection: {}", request.getSourceType(), intersectionId);
    }

    @Override
    public void aggregateFeatures() {
        if (buffer.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<UnifiedTrafficData>> entry : buffer.entrySet()) {
            String intersectionId = entry.getKey();
            List<UnifiedTrafficData> rawData = new ArrayList<>(entry.getValue());
            entry.getValue().clear();

            if (rawData.isEmpty()) continue;

            try {
                // 4. Spatio-temporal alignment
                List<UnifiedTrafficData> aligned = alignmentEngine.align(rawData, WINDOW_SIZE_MS, STEP_SIZE_MS);

                if (aligned.isEmpty()) continue;

                // 5. Convert aligned data to TrafficFlowFeature list
                List<TrafficFlowFeature> features = new ArrayList<>();
                for (UnifiedTrafficData d : aligned) {
                    TrafficFlowFeature feature = new TrafficFlowFeature();
                    feature.setIntersectionId(intersectionId);
                    feature.setTimestamp(d.getTimestamp());
                    feature.setFlow(getDoubleFeature(d, "flow"));
                    feature.setSpeed(getDoubleFeature(d, "speed"));
                    feature.setOccupancy(getDoubleFeature(d, "occupancy"));
                    feature.setQueueLength(getDoubleFeature(d, "queueLength"));
                    feature.setDelay(getDoubleFeature(d, "delay"));
                    features.add(feature);
                }

                // 6. Extract state vector
                StateVectorBO stateVector = featureEngine.extractState(intersectionId, features);

                // 7. Save features and update latest
                for (TrafficFlowFeature f : features) {
                    featureRepository.save(f);
                    latestFeatures.put(intersectionId, f);
                    publisher.publishFeatures(f);
                }

                // 8. Update and publish state vector
                latestStateVectors.put(intersectionId, stateVector);
                publisher.publishStateVector(stateVector);

                log.debug("Aggregated features for intersection: {}, {} windows processed",
                        intersectionId, aligned.size());
            } catch (Exception e) {
                log.error("Error aggregating features for intersection: {}", intersectionId, e);
            }
        }
    }

    @Override
    public TrafficFlowFeature getLatestFeature(String intersectionId) {
        return latestFeatures.get(intersectionId);
    }

    @Override
    public StateVectorBO getLatestStateVector(String intersectionId) {
        return latestStateVectors.get(intersectionId);
    }

    private double getDoubleFeature(UnifiedTrafficData data, String key) {
        Object val = data.getFeatures() != null ? data.getFeatures().get(key) : null;
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }
}
