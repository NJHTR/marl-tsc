package com.njhtr.marltsc.coopt.service.api;

import com.njhtr.marltsc.coopt.api.dto.response.OptimizationResultResponse;

public interface CoOptimizationAppService {
    
    void triggerOptimization(String intersectionId);
    
    OptimizationResultResponse getLatestResult(String intersectionId);
}
