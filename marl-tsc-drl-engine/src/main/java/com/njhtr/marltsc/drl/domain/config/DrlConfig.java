package com.njhtr.marltsc.drl.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "drl")
public class DrlConfig {
    private int stateSize = 8;
    private int actionSize = 4;
    private int[] hiddenLayers = {64, 32};
    private double learningRate = 0.001;
    private double gamma = 0.95;
    private double epsilonInit = 1.0;
    private double epsilonMin = 0.01;
    private double epsilonDecay = 0.995;
    private int replayBufferCapacity = 10000;
    private int batchSize = 32;
    private int targetUpdateFreq = 100;
    private int trainFreq = 4;
    private int greenTimeStep = 5;
}
