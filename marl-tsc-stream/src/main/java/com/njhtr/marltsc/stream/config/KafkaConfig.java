package com.njhtr.marltsc.stream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfig {
    private String bootstrapServers = "localhost:9092";
    private Topics topics = new Topics();

    @Data
    public static class Topics {
        private String input = "traffic-raw-data";
        private String outputFeatures = "traffic-features";
        private String outputAlerts = "traffic-alerts";
    }
}
