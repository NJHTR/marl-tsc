package com.njhtr.marltsc.drl.domain.agent;

import com.njhtr.marltsc.drl.domain.config.DrlConfig;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentManager {

    private final DrlConfig config;
    private final Map<String, TrafficSignalAgent> agents = new ConcurrentHashMap<>();

    public AgentManager(DrlConfig config) {
        this.config = config;
    }

    public TrafficSignalAgent getOrCreateAgent(String intersectionId) {
        return agents.computeIfAbsent(intersectionId, id -> new TrafficSignalAgent(id, config));
    }

    public TrafficSignalAgent getAgent(String intersectionId) {
        return agents.get(intersectionId);
    }

    public Collection<TrafficSignalAgent> getAllAgents() {
        return agents.values();
    }

    public TrafficSignalAgent removeAgent(String intersectionId) {
        return agents.remove(intersectionId);
    }

    public int getAgentCount() {
        return agents.size();
    }
}
