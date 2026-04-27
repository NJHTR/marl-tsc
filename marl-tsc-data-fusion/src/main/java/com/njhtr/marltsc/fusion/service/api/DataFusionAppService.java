package com.njhtr.marltsc.fusion.service.api;

import com.njhtr.marltsc.fusion.api.dto.request.DataIngestRequest;
import com.njhtr.marltsc.fusion.domain.bo.StateVectorBO;
import com.njhtr.marltsc.fusion.domain.bo.TrafficFlowFeature;

public interface DataFusionAppService {
    void ingestRawData(DataIngestRequest request);
    void aggregateFeatures();
    TrafficFlowFeature getLatestFeature(String intersectionId);
    StateVectorBO getLatestStateVector(String intersectionId);
}
