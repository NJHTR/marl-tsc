package com.njhtr.marltsc.drl;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DrlEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(DrlEngineApplication.class, args);
    }
}
