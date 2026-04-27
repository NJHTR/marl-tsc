package com.njhtr.marltsc.coopt.domain.bo;

import lombok.Data;
import java.util.Map;

@Data
public class PredictedNetworkStateBO {
    private Map<String, Double> segmentTravelTimes;
    private Map<String, Double> segmentCongestionProbs;
}
