package com.njhtr.marltsc.stream.manager;

import com.njhtr.marltsc.stream.config.FlinkConfig;
import com.njhtr.marltsc.stream.config.KafkaConfig;
import com.njhtr.marltsc.stream.job.TrafficStreamJob;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class StreamJobManager {

    private final FlinkConfig flinkConfig;
    private final KafkaConfig kafkaConfig;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public StreamJobManager(FlinkConfig flinkConfig, KafkaConfig kafkaConfig) {
        this.flinkConfig = flinkConfig;
        this.kafkaConfig = kafkaConfig;
    }

    public synchronized void startJobs() {
        if (running.get()) {
            log.warn("Flink jobs are already running");
            return;
        }

        log.info("Starting Flink job: {}", TrafficStreamJob.JOB_NAME);

        StreamExecutionEnvironment env = TrafficStreamJob.createEnvironment(flinkConfig);
        TrafficStreamJob.buildPipeline(env, kafkaConfig);

        CompletableFuture.runAsync(() -> {
            try {
                running.set(true);
                env.execute(TrafficStreamJob.JOB_NAME);
            } catch (Exception e) {
                log.error("Flink job execution failed", e);
                running.set(false);
            }
        });

        log.info("Flink job submitted: {}", TrafficStreamJob.JOB_NAME);
    }

    public synchronized void stopJobs() {
        if (!running.get()) {
            log.warn("No Flink jobs are running");
            return;
        }
        log.info("Stopping Flink jobs...");
        running.set(false);
    }

    public JobStatus getStatus() {
        return running.get() ? JobStatus.RUNNING : JobStatus.STOPPED;
    }

    public enum JobStatus {
        RUNNING, STOPPED
    }

    @PreDestroy
    public void shutdown() {
        if (running.get()) {
            log.info("Shutting down Flink jobs...");
            stopJobs();
        }
    }
}
