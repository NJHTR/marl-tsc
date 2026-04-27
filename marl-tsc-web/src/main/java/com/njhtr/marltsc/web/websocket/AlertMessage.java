package com.njhtr.marltsc.web.websocket;

import lombok.Data;

@Data
public class AlertMessage {
    private String intersectionId;
    private String alertType;
    private Integer severity;
    private String message;
    private Long timestamp;
}
