package com.njhtr.marltsc.drl.domain.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.njhtr.marltsc.drl.domain.agent.AgentManager;
import com.njhtr.marltsc.drl.domain.agent.TrafficSignalAgent;
import com.njhtr.marltsc.drl.domain.config.DrlConfig;
import com.njhtr.marltsc.drl.domain.experience.Experience;
import com.njhtr.marltsc.drl.domain.experience.ReplayBuffer;
import com.njhtr.marltsc.drl.domain.model.DQNModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelPersistenceService {

    private final AgentManager agentManager;
    private final DrlConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${drl.model.dir:./drl-models}")
    private String modelDir;

    @PostConstruct
    public void loadAll() {
        Path dir = Path.of(modelDir);
        if (!Files.isDirectory(dir)) {
            log.info("Model directory not found, starting fresh: {}", dir.toAbsolutePath());
            return;
        }
        try {
            List<Path> modelFiles = Files.list(dir)
                    .filter(p -> p.toString().endsWith("_model.zip"))
                    .toList();
            for (Path modelFile : modelFiles) {
                String filename = modelFile.getFileName().toString();
                String intersectionId = filename.replace("_model.zip", "");
                try {
                    loadAgent(intersectionId, dir);
                } catch (Exception e) {
                    log.warn("Failed to load model for {}, starting fresh: {}", intersectionId, e.getMessage());
                }
            }
            log.info("Loaded {} agents from {}", agentManager.getAgentCount(), dir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to list model directory", e);
        }
    }

    private void loadAgent(String intersectionId, Path dir) throws IOException {
        byte[] modelBytes = Files.readAllBytes(dir.resolve(intersectionId + "_model.zip"));
        DQNModel model = DQNModel.fromBytes(modelBytes, config.getStateSize(),
                config.getActionSize(), config.getHiddenLayers(), config.getLearningRate());

        Meta meta = objectMapper.readValue(
                dir.resolve(intersectionId + "_meta.json").toFile(), Meta.class);

        ReplayBuffer buffer = new ReplayBuffer(config.getReplayBufferCapacity());
        Path bufferFile = dir.resolve(intersectionId + "_buffer.json");
        if (Files.exists(bufferFile)) {
            List<Experience> experiences = objectMapper.readValue(
                    bufferFile.toFile(), new TypeReference<List<Experience>>() {});
            buffer.addAll(experiences);
        }

        TrafficSignalAgent agent = TrafficSignalAgent.restore(intersectionId, config,
                model, buffer, meta.epsilon, meta.totalSteps, meta.trainSteps, meta.trainingMode);
        agentManager.registerAgent(agent);
        log.info("Restored agent {}: epsilon={}, steps={}, buffer={}",
                intersectionId, String.format("%.3f", meta.epsilon), meta.totalSteps, buffer.size());
    }

    @Scheduled(fixedRate = 30_000)
    public void saveAll() {
        if (agentManager.getAgentCount() == 0) return;
        try {
            Path dir = Path.of(modelDir);
            Files.createDirectories(dir);
            for (TrafficSignalAgent agent : agentManager.getAllAgents()) {
                saveAgent(agent, dir);
            }
            log.debug("Saved {} agents", agentManager.getAgentCount());
        } catch (Exception e) {
            log.error("Failed to save models: {}", e.getMessage());
        }
    }

    private void saveAgent(TrafficSignalAgent agent, Path dir) throws IOException {
        String id = agent.getIntersectionId();

        Files.write(dir.resolve(id + "_model.zip"), agent.getPolicyNetwork().toBytes());

        Meta meta = new Meta(agent.getEpsilon(), agent.getTotalSteps(),
                agent.getTrainSteps(), agent.isTrainingMode());
        objectMapper.writeValue(dir.resolve(id + "_meta.json").toFile(), meta);

        List<Experience> experiences = agent.getReplayBuffer().getAll();
        if (!experiences.isEmpty()) {
            objectMapper.writeValue(dir.resolve(id + "_buffer.json").toFile(), experiences);
        }
    }

    @SuppressWarnings("unused")
    private static class Meta {
        public double epsilon;
        public int totalSteps;
        public int trainSteps;
        public boolean trainingMode;

        Meta() {}

        Meta(double epsilon, int totalSteps, int trainSteps, boolean trainingMode) {
            this.epsilon = epsilon;
            this.totalSteps = totalSteps;
            this.trainSteps = trainSteps;
            this.trainingMode = trainingMode;
        }
    }
}
