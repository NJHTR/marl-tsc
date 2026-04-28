package com.njhtr.marltsc.signal.service.api;

import com.njhtr.marltsc.common.dto.PhaseAdjustRequest;
import com.njhtr.marltsc.common.dto.SignalPlanResponse;

/**
 * Signal control application service interface.
 */
public interface SignalControlAppService {

    SignalPlanResponse getCurrentPlan(String intersectionId);

    void adjustPhase(PhaseAdjustRequest request);
}
