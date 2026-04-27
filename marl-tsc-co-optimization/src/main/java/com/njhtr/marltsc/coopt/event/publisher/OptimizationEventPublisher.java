package com.njhtr.marltsc.coopt.event.publisher;

import com.njhtr.marltsc.coopt.api.dto.response.OptimizationResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OptimizationEventPublisher {

    /**
     * Placeholder method that will publish Kafka events later.
     * For now, just logs the event.
     */
    public void publishOptimizationCompleted(OptimizationResultResponse result) {
        log.info("Optimization completed - Intersection: {}, Reward: {}, Iterations: {}", 
                result.getIntersectionId(), 
                result.getJointReward(), 
                result.getConvergenceIterations());
        // TODO: Implement Kafka event publishing
    }
}
