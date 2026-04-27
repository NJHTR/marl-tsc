package com.njhtr.marltsc.stream;

import com.njhtr.marltsc.stream.manager.StreamJobManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class StreamApplication {

    private final StreamJobManager streamJobManager;

    public static void main(String[] args) {
        SpringApplication.run(StreamApplication.class, args);
    }

    @Bean
    public CommandLineRunner startFlinkJobs() {
        return args -> {
            log.info("Starting Flink stream processing jobs...");
            streamJobManager.startJobs();
        };
    }
}
