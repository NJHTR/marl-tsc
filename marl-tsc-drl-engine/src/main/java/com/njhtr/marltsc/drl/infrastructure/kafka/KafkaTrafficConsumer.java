package com.njhtr.marltsc.drl.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTrafficConsumer {

    private final TrafficStateHolder stateHolder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${spring.kafka.topics.traffic-features}")
    public void onTrafficFeature(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String intersectionId = node.get("intersectionId").asText();

            var snapshot = new TrafficStateHolder.TrafficFeatureSnapshot(
                    node.get("flow").asDouble(),
                    node.get("speed").asDouble(),
                    node.get("occupancy").asDouble(),
                    node.get("queueLength").asDouble(),
                    node.get("delay").asDouble(),
                    node.has("timestamp") ? node.get("timestamp").asLong() : System.currentTimeMillis()
            );

            stateHolder.update(intersectionId, snapshot);
            log.debug("Traffic feature updated for {}: occupancy={}", intersectionId,
                    String.format("%.3f", snapshot.getOccupancy()));
        } catch (Exception e) {
            log.warn("Failed to parse traffic-features message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${spring.kafka.topics.traffic-alerts}")
    public void onTrafficAlert(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String intersectionId = node.get("intersectionId").asText();
            String alertType = node.has("alertType") ? node.get("alertType").asText() : "UNKNOWN";
            int severity = node.has("severity") ? node.get("severity").asInt() : 0;
            String msg = node.has("message") ? node.get("message").asText() : "";

            log.warn("Traffic alert: intersection={}, type={}, severity={}, message={}",
                    intersectionId, alertType, severity, msg);
        } catch (Exception e) {
            log.warn("Failed to parse traffic-alerts message: {}", e.getMessage());
        }
    }
}
