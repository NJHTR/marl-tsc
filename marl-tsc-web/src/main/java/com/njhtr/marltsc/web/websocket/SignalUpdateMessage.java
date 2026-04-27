package com.njhtr.marltsc.web.websocket;

import lombok.Data;

@Data
public class SignalUpdateMessage {
    private String intersectionId;
    private Integer currentPhase;
    private Integer greenTime;
    private String status;
}
