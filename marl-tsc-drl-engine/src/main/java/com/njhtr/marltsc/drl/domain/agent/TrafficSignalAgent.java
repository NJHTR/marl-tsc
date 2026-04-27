package com.njhtr.marltsc.drl.domain.agent;

import com.njhtr.marltsc.drl.domain.config.DrlConfig;
import com.njhtr.marltsc.drl.domain.experience.Experience;
import com.njhtr.marltsc.drl.domain.experience.ReplayBuffer;
import com.njhtr.marltsc.drl.domain.model.DQNModel;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TrafficSignalAgent {

    private final String intersectionId;
    private final DQNModel policyNetwork;
    private final DQNModel targetNetwork;
    private final ReplayBuffer replayBuffer;
    private final DrlConfig config;
    private double epsilon;
    private int totalSteps;
    private int trainSteps;
    private boolean trainingMode;

    public TrafficSignalAgent(String intersectionId, DrlConfig config) {
        this.intersectionId = intersectionId;
        this.config = config;
        this.policyNetwork = new DQNModel(
                config.getStateSize(), config.getActionSize(),
                config.getHiddenLayers(), config.getLearningRate());
        this.targetNetwork = policyNetwork.copy();
        this.replayBuffer = new ReplayBuffer(config.getReplayBufferCapacity());
        this.epsilon = config.getEpsilonInit();
        this.trainingMode = true;
    }

    public TrafficSignalAgent(String intersectionId, DrlConfig config, DQNModel pretrained) {
        this.intersectionId = intersectionId;
        this.config = config;
        this.policyNetwork = pretrained;
        this.targetNetwork = pretrained.copy();
        this.replayBuffer = new ReplayBuffer(config.getReplayBufferCapacity());
        this.epsilon = config.getEpsilonMin();
        this.trainingMode = false;
    }

    public int selectAction(double[] state) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (trainingMode && random.nextDouble() < epsilon) {
            return random.nextInt(config.getActionSize());
        }
        INDArray input = Nd4j.create(new double[][]{state});
        INDArray output = policyNetwork.predict(input);
        return output.argMax(1).getInt(0);
    }

    public double[] predictQValues(double[] state) {
        INDArray input = Nd4j.create(new double[][]{state});
        INDArray output = policyNetwork.predict(input);
        double[] values = new double[config.getActionSize()];
        for (int i = 0; i < config.getActionSize(); i++) {
            values[i] = output.getDouble(0, i);
        }
        return values;
    }

    public void train(Experience experience) {
        if (!trainingMode) return;

        replayBuffer.add(experience);
        totalSteps++;

        if (totalSteps % config.getTrainFreq() != 0) return;
        if (replayBuffer.size() < config.getBatchSize()) return;

        List<Experience> batch = replayBuffer.sample(config.getBatchSize());
        int batchSize = batch.size();
        int stateSize = config.getStateSize();
        int actionSize = config.getActionSize();

        INDArray states = Nd4j.create(batchSize, stateSize);
        INDArray nextStates = Nd4j.create(batchSize, stateSize);

        for (int i = 0; i < batchSize; i++) {
            Experience e = batch.get(i);
            for (int j = 0; j < stateSize; j++) {
                states.putScalar(i, j, e.getState()[j]);
                if (e.getNextState() != null) {
                    nextStates.putScalar(i, j, e.getNextState()[j]);
                }
            }
        }

        INDArray currentQs = policyNetwork.predict(states);
        INDArray nextQs = targetNetwork.predict(nextStates);

        INDArray targets = currentQs.dup();
        for (int i = 0; i < batchSize; i++) {
            Experience e = batch.get(i);
            double targetQ;
            if (e.isDone()) {
                targetQ = e.getReward();
            } else {
                double maxNextQ = nextQs.getRow(i).max().getDouble();
                targetQ = e.getReward() + config.getGamma() * maxNextQ;
            }
            targets.putScalar(i, e.getAction(), targetQ);
        }

        policyNetwork.fit(states, targets);
        trainSteps++;

        if (trainSteps % config.getTargetUpdateFreq() == 0) {
            targetNetwork.syncParamsFrom(policyNetwork);
        }

        if (epsilon > config.getEpsilonMin()) {
            epsilon = Math.max(config.getEpsilonMin(), epsilon * config.getEpsilonDecay());
        }
    }

    public String getIntersectionId() { return intersectionId; }
    public double getEpsilon() { return epsilon; }
    public int getTotalSteps() { return totalSteps; }
    public int getReplayBufferSize() { return replayBuffer.size(); }
    public boolean isTrainingMode() { return trainingMode; }
    public void setTrainingMode(boolean trainingMode) { this.trainingMode = trainingMode; }
    public DQNModel getPolicyNetwork() { return policyNetwork; }

    public void reset() {
        this.epsilon = config.getEpsilonInit();
        this.totalSteps = 0;
        this.trainSteps = 0;
        this.replayBuffer.clear();
    }
}
