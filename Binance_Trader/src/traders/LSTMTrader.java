package traders;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

public class LSTMTrader extends Trader {
	
	private static final int UPDATE_RATE_SEC = 60;
	
	// RNN dimensions
	private static final int HIDDEN_LAYER_WIDTH = 50;
	private static final int HIDDEN_LAYER_CONT = 3;
    private static final Random r = new Random(7894);
    
    private static final int DATA_FETCH_SIZE = 60; // Look 60 min into the past.
    // How many inputs we will look at once
    private static final int INPUT_SIZE = 3;
    private static final int NUM_OUTPUT_PREDICTIONS = 1; // Predict for the next 3 timesteps.
    private static final int INPUT_LAYER_SIZE = DATA_FETCH_SIZE - INPUT_SIZE - NUM_OUTPUT_PREDICTIONS + 1;
	
	private MultiLayerNetwork net;
	
	public LSTMTrader(boolean isTestMode) {
		super(UPDATE_RATE_SEC, isTestMode);
		// TODO Auto-generated constructor stub
		initTrainNetwork();
	}
	
	
	/*
	 * This method is intended to train up a net if we have none already trained. This method thus should 
	 * only be called at the beginning of a run, and should only actually train a network once. Every other 
	 * time, it should just read a model from a file saved by the save method.
	 */
	private void initTrainNetwork() {
		
		boolean modelExists = new File("model").exists();
		if (modelExists) {
			
			System.out.println("Existing model found, Restoring...");
			try {
				net = ModelSerializer.restoreMultiLayerNetwork("model", true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			
			System.out.println("No model found, initializing new one.");
			// some common parameters
			NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder();
			builder.seed(123);
			builder.biasInit(0);
			builder.miniBatch(false);
			builder.updater(new RmsProp(0.01));
			builder.weightInit(WeightInit.XAVIER);

			ListBuilder listBuilder = builder.list();

			// first difference, for rnns we need to use LSTM.Builder
			for (int i = 0; i < HIDDEN_LAYER_CONT; i++) {
				LSTM.Builder hiddenLayerBuilder = new LSTM.Builder();
				hiddenLayerBuilder.nIn(i == 0 ? INPUT_SIZE : HIDDEN_LAYER_WIDTH);
				hiddenLayerBuilder.nOut(HIDDEN_LAYER_WIDTH);
				// adopted activation function from LSTMCharModellingExample
				// seems to work well with RNNs
				hiddenLayerBuilder.activation(Activation.TANH);
				listBuilder.layer(i, hiddenLayerBuilder.build());
			}

			// we need to use RnnOutputLayer for our RNN
			// TODO: Explore different Loss functions and analyze which would be most useful
			// for this
			RnnOutputLayer.Builder outputLayerBuilder = new RnnOutputLayer.Builder(LossFunction.MSE);
			// softmax normalizes the output neurons, the sum of all outputs is 1
			// this is required for our sampleFromDistribution-function
			outputLayerBuilder.activation(Activation.IDENTITY);
			outputLayerBuilder.nIn(HIDDEN_LAYER_WIDTH);
			// To make predictions, we only would want a hidden layer size of 1.
			outputLayerBuilder.nOut(NUM_OUTPUT_PREDICTIONS);
			listBuilder.layer(HIDDEN_LAYER_CONT, outputLayerBuilder.build());

			// create network
			MultiLayerConfiguration conf = listBuilder.build();
			conf.setBackprop(true);
			net = new MultiLayerNetwork(conf);
			net.init();
			net.setListeners(new ScoreIterationListener(1));
			
			/* TODO: Add code here to actually train the network with something... 
			 * I'm not sure if this will end up being necessary, but it would be 
			 * nice to have.
			 */
		}
	}

	/*
	 * This method will not only do the trading as with the other traders, but will also update the network with a 
	 * new piece of data, hopefully strengthening the model over time. This should also make the network able to 
	 * change over time as the market changes.
	 */
	@Override
	protected void update() {
		// TODO: Implement
	}
	
	
	/* 
	 * This method should be as simple as saving the model to a file, nothing more. We do this by utilizing Dl4J's 
	 * Model Serialization capability.
	 */
	private void saveNet() {
		try {
			ModelSerializer.writeModel(net, "model", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
