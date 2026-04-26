package com.njhtr.marltsc.signal.service.api;

import com.njhtr.marltsc.signal.api.dto.request.PhaseAdjustRequest;
import com.njhtr.marltsc.signal.api.dto.response.SignalPlanResponse;

/**
 * Signal control application service interface.
 */
public interface SignalControlAppService {

    SignalPlanResponse getCurrentPlan(String intersectionId);

    void adjustPhase(PhaseAdjustRequest request);
}
