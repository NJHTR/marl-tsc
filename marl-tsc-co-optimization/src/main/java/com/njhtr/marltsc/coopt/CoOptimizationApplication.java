package com.njhtr.marltsc.coopt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.njhtr.marltsc.coopt.infrastructure.client")
@EnableScheduling
public class CoOptimizationApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoOptimizationApplication.class, args);
    }
}
