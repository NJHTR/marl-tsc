package com.njhtr.marltsc.web.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrafficWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastTrafficUpdate(TrafficUpdateMessage message) {
        messagingTemplate.convertAndSend("/topic/traffic", message);
        log.debug("Broadcast traffic update: intersection={}, flow={}", message.getIntersectionId(), message.getFlow());
    }

    public void broadcastSignalUpdate(SignalUpdateMessage message) {
        messagingTemplate.convertAndSend("/topic/signal", message);
        log.debug("Broadcast signal update: intersection={}, phase={}", message.getIntersectionId(), message.getCurrentPhase());
    }

    public void broadcastAlert(AlertMessage message) {
        messagingTemplate.convertAndSend("/topic/alerts", message);
        log.warn("Broadcast alert: intersection={}, type={}, severity={}",
                message.getIntersectionId(), message.getAlertType(), message.getSeverity());
    }
}
