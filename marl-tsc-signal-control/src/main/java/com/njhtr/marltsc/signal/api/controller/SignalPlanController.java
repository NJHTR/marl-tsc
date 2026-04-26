package com.njhtr.marltsc.signal.api.controller;

import com.njhtr.marltsc.common.result.ApiResult;
import com.njhtr.marltsc.signal.api.dto.request.PhaseAdjustRequest;
import com.njhtr.marltsc.signal.api.dto.response.SignalPlanResponse;
import com.njhtr.marltsc.signal.service.api.SignalControlAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signal plan REST controller.
 *
 * <p>Handles HTTP requests, parameter validation, delegates to AppService, returns ApiResult.</p>
 */
@RestController
@RequestMapping("/api/v1/signal")
@RequiredArgsConstructor
public class SignalPlanController {

    private final SignalControlAppService signalService;

    @GetMapping("/plans/{intersectionId}")
    public ApiResult<SignalPlanResponse> getPlan(@PathVariable String intersectionId) {
        Assert.hasText(intersectionId, "路口ID不能为空");

        SignalPlanResponse response = signalService.getCurrentPlan(intersectionId);
        return ApiResult.ok(response);
    }

    @PostMapping("/plans/adjust")
    public ApiResult<Void> adjustPhase(@RequestBody PhaseAdjustRequest request) {
        signalService.adjustPhase(request);
        return ApiResult.ok();
    }
}
