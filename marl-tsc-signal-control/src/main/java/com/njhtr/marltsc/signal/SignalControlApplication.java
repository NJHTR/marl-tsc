package com.njhtr.marltsc.signal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {"com.njhtr.marltsc.signal", "com.njhtr.marltsc.common"})
public class SignalControlApplication {
    public static void main(String[] args) {
        SpringApplication.run(SignalControlApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
