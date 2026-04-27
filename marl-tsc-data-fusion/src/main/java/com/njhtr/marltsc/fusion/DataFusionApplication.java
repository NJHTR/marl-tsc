package com.njhtr.marltsc.fusion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataFusionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataFusionApplication.class, args);
    }
}
