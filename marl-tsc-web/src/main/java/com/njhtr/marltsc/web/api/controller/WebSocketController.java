package com.njhtr.marltsc.web.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class WebSocketController {

    @MessageMapping("/traffic/subscribe")
    public void handleSubscribe(Map<String, Object> payload) {
        log.info("WebSocket subscription request: {}", payload);
    }
}
