package com.njhtr.marltsc.route.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "signal-control-service", url = "http://localhost:8081")
public interface SignalControlClient {

    @GetMapping("/api/v1/signal/status")
    String getSignalStatus();
}
