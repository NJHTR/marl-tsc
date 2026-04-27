package com.njhtr.marltsc.fusion.infrastructure.publisher;

import com.njhtr.marltsc.fusion.domain.bo.StateVectorBO;
import com.njhtr.marltsc.fusion.domain.bo.TrafficFlowFeature;
import com.njhtr.marltsc.fusion.domain.entity.UnifiedTrafficData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTrafficDataPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRawData(UnifiedTrafficData data) {
        kafkaTemplate.send("traffic-raw-data", data.getDataId(), data);
        log.debug("Published raw data to traffic-raw-data: {}", data.getDataId());
    }

    public void publishFeatures(TrafficFlowFeature feature) {
        kafkaTemplate.send("traffic-features", feature.getIntersectionId(), feature);
        log.debug("Published features to traffic-features for intersection: {}", feature.getIntersectionId());
    }

    public void publishStateVector(StateVectorBO stateVector) {
        kafkaTemplate.send("traffic-state-vector", stateVector.getIntersectionId(), stateVector);
        log.debug("Published state vector to traffic-state-vector for intersection: {}", stateVector.getIntersectionId());
    }
}
