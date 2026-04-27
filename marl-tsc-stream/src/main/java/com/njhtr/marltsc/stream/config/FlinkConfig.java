package com.njhtr.marltsc.stream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flink")
public class FlinkConfig {
    private int parallelism = 2;
    private long checkpointInterval = 5000;
}
