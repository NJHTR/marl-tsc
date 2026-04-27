package com.njhtr.marltsc.drl.domain.model;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.*;

public class DQNModel {

    private final int stateSize;
    private final int actionSize;
    private final int[] hiddenLayers;
    private transient MultiLayerNetwork network;

    public DQNModel(int stateSize, int actionSize, int[] hiddenLayers, double learningRate) {
        this.stateSize = stateSize;
        this.actionSize = actionSize;
        this.hiddenLayers = hiddenLayers.clone();
        this.network = buildNetwork(stateSize, actionSize, hiddenLayers, learningRate);
    }

    private DQNModel(int stateSize, int actionSize, int[] hiddenLayers, MultiLayerNetwork network) {
        this.stateSize = stateSize;
        this.actionSize = actionSize;
        this.hiddenLayers = hiddenLayers.clone();
        this.network = network;
    }

    private static MultiLayerNetwork buildNetwork(int stateSize, int actionSize, int[] hiddenLayers, double learningRate) {
        NeuralNetConfiguration.ListBuilder builder = new NeuralNetConfiguration.Builder()
                .seed(System.currentTimeMillis())
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.ADAM)
                .updater(new Adam(learningRate))
                .list();

        int prevSize = stateSize;
        for (int i = 0; i < hiddenLayers.length; i++) {
            builder.layer(i, new DenseLayer.Builder()
                    .nIn(prevSize)
                    .nOut(hiddenLayers[i])
                    .activation(Activation.RELU)
                    .build());
            prevSize = hiddenLayers[i];
        }

        builder.layer(hiddenLayers.length, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(prevSize)
                .nOut(actionSize)
                .activation(Activation.IDENTITY)
                .build());

        MultiLayerConfiguration config = builder.build();
        MultiLayerNetwork network = new MultiLayerNetwork(config);
        network.init();
        return network;
    }

    public INDArray predict(INDArray input) {
        return network.output(input, false);
    }

    public void fit(INDArray input, INDArray target) {
        network.fit(input, target);
    }

    public DQNModel copy() {
        MultiLayerNetwork copy = new MultiLayerNetwork(network.getLayerWiseConfigurations().clone());
        copy.init();
        copy.setParams(network.params().dup());
        return new DQNModel(stateSize, actionSize, hiddenLayers, copy);
    }

    public void syncParamsFrom(DQNModel other) {
        this.network.setParams(other.network.params().dup());
    }

    public byte[] toBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ModelSerializer.writeModel(network, baos, false);
        return baos.toByteArray();
    }

    public static DQNModel fromBytes(byte[] data, int stateSize, int actionSize, int[] hiddenLayers, double learningRate) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork(bais, true);
        return new DQNModel(stateSize, actionSize, hiddenLayers, net);
    }

    public MultiLayerNetwork getNetwork() {
        return network;
    }

    public int getStateSize() {
        return stateSize;
    }

    public int getActionSize() {
        return actionSize;
    }
}
