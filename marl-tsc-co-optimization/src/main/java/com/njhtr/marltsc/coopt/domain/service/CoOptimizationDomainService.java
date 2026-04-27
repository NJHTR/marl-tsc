package com.njhtr.marltsc.coopt.domain.service;

import com.njhtr.marltsc.coopt.domain.bo.PredictedNetworkStateBO;
import com.njhtr.marltsc.coopt.domain.bo.RouteChangeBO;
import com.njhtr.marltsc.coopt.domain.bo.SignalActionBO;

import java.util.Map;

/**
 * Pure logic service for co-optimization.
 * No Spring dependencies, no database access, no Feign clients.
 */
public class CoOptimizationDomainService {

    /**
     * Computes joint reward: R = -0.6 * totalDelay + 0.3 * throughput - 0.1 * fairness
     */
    public double evaluateJointPolicy(Map<String, SignalActionBO> signalActions, 
                                      Map<String, RouteChangeBO> routeChanges) {
        // Simplified calculation based on number of actions
        double totalDelay = signalActions.size() * 10.0; // Simulated delay
        double throughput = routeChanges.size() * 5.0;   // Simulated throughput
        double fairness = Math.abs(signalActions.size() - routeChanges.size()) * 2.0; // Simulated fairness metric
        
        return -0.6 * totalDelay + 0.3 * throughput - 0.1 * fairness;
    }

    /**
     * Simple prediction logic: add delay reduction to travel times based on signal actions
     */
    public PredictedNetworkStateBO predictNetworkState(PredictedNetworkStateBO currentState, 
                                                       Map<String, SignalActionBO> actions) {
        if (currentState == null || currentState.getSegmentTravelTimes() == null) {
            return new PredictedNetworkStateBO();
        }

        PredictedNetworkStateBO predicted = new PredictedNetworkStateBO();
        
        // Copy and adjust travel times based on signal actions
        Map<String, Double> predictedTravelTimes = new java.util.HashMap<>(currentState.getSegmentTravelTimes());
        for (Map.Entry<String, Double> entry : predictedTravelTimes.entrySet()) {
            // Reduce travel time by 5% for each signal action (simplified logic)
            double reductionFactor = 1.0 - (actions.size() * 0.05);
            entry.setValue(entry.getValue() * reductionFactor);
        }
        predicted.setSegmentTravelTimes(predictedTravelTimes);
        
        // Copy congestion probabilities
        if (currentState.getSegmentCongestionProbs() != null) {
            predicted.setSegmentCongestionProbs(new java.util.HashMap<>(currentState.getSegmentCongestionProbs()));
        }
        
        return predicted;
    }

    /**
     * Check if optimization converged by comparing travel times
     */
    public boolean isConverged(PredictedNetworkStateBO previous, PredictedNetworkStateBO current) {
        if (previous == null || current == null) {
            return false;
        }
        
        if (previous.getSegmentTravelTimes() == null || current.getSegmentTravelTimes() == null) {
            return false;
        }
        
        // Check if travel times have stabilized (difference < 1%)
        for (String segment : previous.getSegmentTravelTimes().keySet()) {
            if (!current.getSegmentTravelTimes().containsKey(segment)) {
                continue;
            }
            
            double prevTime = previous.getSegmentTravelTimes().get(segment);
            double currTime = current.getSegmentTravelTimes().get(segment);
            
            if (prevTime > 0) {
                double change = Math.abs(currTime - prevTime) / prevTime;
                if (change > 0.01) { // 1% threshold
                    return false;
                }
            }
        }
        
        return true;
    }
}
