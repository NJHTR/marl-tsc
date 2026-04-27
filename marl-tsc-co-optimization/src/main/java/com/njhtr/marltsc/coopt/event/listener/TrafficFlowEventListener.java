package com.njhtr.marltsc.coopt.event.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Placeholder listener for traffic flow events.
 * Will be activated when Kafka integration is ready.
 */
@Slf4j
@Component
public class TrafficFlowEventListener {

    // @KafkaListener(topics = "traffic-flow-updates", groupId = "co-optimization-service")
    // public void handleTrafficFlowUpdate(String message) {
    //     log.info("Received traffic flow update: {}", message);
    //     // Process the traffic flow update
    // }

    public void onTrafficFlowUpdate(String message) {
        log.debug("Traffic flow update received (stub): {}", message);
    }
}
