package com.njhtr.marltsc.fusion.domain.service;

import com.njhtr.marltsc.fusion.domain.bo.StateVectorBO;
import com.njhtr.marltsc.fusion.domain.bo.TrafficFlowFeature;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeatureEngineeringEngine {

    public StateVectorBO extractState(String intersectionId, List<TrafficFlowFeature> windowData) {
        if (windowData == null || windowData.isEmpty()) {
            return new StateVectorBO(intersectionId, new double[0], System.currentTimeMillis());
        }

        double avgFlow = windowData.stream().mapToDouble(TrafficFlowFeature::getFlow).average().orElse(0);
        double avgSpeed = windowData.stream().mapToDouble(TrafficFlowFeature::getSpeed).average().orElse(0);
        double avgOccupancy = windowData.stream().mapToDouble(TrafficFlowFeature::getOccupancy).average().orElse(0);
        double avgQueueLength = windowData.stream().mapToDouble(TrafficFlowFeature::getQueueLength).average().orElse(0);
        double avgDelay = windowData.stream().mapToDouble(TrafficFlowFeature::getDelay).average().orElse(0);

        return new StateVectorBO(intersectionId,
                new double[]{avgFlow, avgSpeed, avgOccupancy, avgQueueLength, avgDelay},
                System.currentTimeMillis());
    }
}
