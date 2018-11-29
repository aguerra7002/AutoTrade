package temp;
/*
 * IMPORTANT: This is just a test file, all the dependencies are missing from this project as it was originally
 * from another project. This file is included because it gives the barebones shell to create a RNN, which an LSTM 
 * network falls under the category of. This class is really simplified, and real LSTM's should fat outperform this 
 * type of learning. This is iteration 2 and is starting to resemble a network more reflective of a time series predictor 
 * model.
 */
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.indexaccum.IMax;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;


public class BasicLSTMIter2 {

	// define a sentence to learn.
    // Add a special character at the beginning so the RNN learns the complete string and ends with the marker.
	private static final int[] LEARNARR_HARD = {0,1,2,3,4,5,4,3,2,1,2,3,4,5,4,3,2,1,2,3};
	private static final int[] LEARNARR_MED = {0,2,4,5,3,1,2,4,5,3,1,2,4,5,3,1,2,4,5,3};
	private static final int[] LEARNARR_EASY = {0,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1};
	
	private static final int[] LEARNARR = LEARNARR_MED;

	// a list of all possible characters
	private static final List<Integer> LEARNSTRING_INTS_LIST = new ArrayList<>();

	// RNN dimensions
	private static final int HIDDEN_LAYER_WIDTH = 50;
	private static final int HIDDEN_LAYER_CONT = 2;
    private static final Random r = new Random(7894);

	public static void main(String[] args) {

		// create a dedicated list of possible ints
		// TODO: We don't want this in our final model, as we would like there 
		// to be a continuous space where are output can map to, not just a couple 
		// Values. Find a way to deal with this.
		LinkedHashSet<Integer> LEARNSTRING_CHARS = new LinkedHashSet<>();
		for (int c : LEARNARR)
			LEARNSTRING_CHARS.add(c);
		LEARNSTRING_INTS_LIST.addAll(LEARNSTRING_CHARS);

		// some common parameters
		NeuralNetConfiguration.Builder builder = new NeuralNetConfiguration.Builder();
		builder.seed(123);
		builder.biasInit(0);
		builder.miniBatch(false);
		builder.updater(new RmsProp(0.001));
		builder.weightInit(WeightInit.XAVIER);

		ListBuilder listBuilder = builder.list();

		// first difference, for rnns we need to use LSTM.Builder
		for (int i = 0; i < HIDDEN_LAYER_CONT; i++) {
			LSTM.Builder hiddenLayerBuilder = new LSTM.Builder();
			hiddenLayerBuilder.nIn(i == 0 ? LEARNSTRING_CHARS.size() : HIDDEN_LAYER_WIDTH);
			hiddenLayerBuilder.nOut(HIDDEN_LAYER_WIDTH);
			// adopted activation function from LSTMCharModellingExample
			// seems to work well with RNNs
			hiddenLayerBuilder.activation(Activation.TANH);
			listBuilder.layer(i, hiddenLayerBuilder.build());
		}

		// we need to use RnnOutputLayer for our RNN
		// TODO: Explore different Loss functions and analyze which would be most useful for this 
		RnnOutputLayer.Builder outputLayerBuilder = new RnnOutputLayer.Builder(LossFunction.MCXENT);
		// softmax normalizes the output neurons, the sum of all outputs is 1
		// this is required for our sampleFromDistribution-function
		outputLayerBuilder.activation(Activation.SOFTMAX);
		outputLayerBuilder.nIn(HIDDEN_LAYER_WIDTH);
		// To make predictions, we only would want a hidden layer size of 1.
		outputLayerBuilder.nOut(LEARNSTRING_CHARS.size()); 
		listBuilder.layer(HIDDEN_LAYER_CONT, outputLayerBuilder.build());

		// create network
		MultiLayerConfiguration conf = listBuilder.build();
		MultiLayerNetwork net = new MultiLayerNetwork(conf);
		net.init();
		net.setListeners(new ScoreIterationListener(1));

		// Here we create the train data. 
		// TODO: Increase the size of the input vectors for each iteration of the RNN so that we can look 
		// farther back to see how  
		INDArray input = Nd4j.zeros(1, LEARNSTRING_INTS_LIST.size(), LEARNARR.length);
		INDArray labels = Nd4j.zeros(1, LEARNSTRING_INTS_LIST.size(), LEARNARR.length);
		// loop through our sample-sentence
		for (int i = 0; i < LEARNARR.length - 1; i++) {
			int currentInt = LEARNARR[i];
			// Get next char as label for input. Note that this is very basic and only considers past characters, but should 
			// Really be more than a scalar. This gives the network more of a "memory" in that sense.
			int nextInt = LEARNARR[(i + 1) % (LEARNARR.length)];
			// input neuron for current-char is 1 at "samplePos"
			input.putScalar(new int[] { 0, LEARNSTRING_INTS_LIST.indexOf(currentInt), i }, 1);
			// output neuron for next-char is 1 at "samplePos"
			labels.putScalar(new int[] { 0, LEARNSTRING_INTS_LIST.indexOf(nextInt), i }, 1);
		}
		DataSet trainingData = new DataSet(input, labels);

		// some epochs
		for (int epoch = 0; epoch < 100; epoch++) {

			System.out.println("Epoch " + epoch);

			// train the data
			net.fit(trainingData);
			System.out.println(trainingData.toString());

			// clear current stance from the last example
			net.rnnClearPreviousState();

			// put the last character into the rrn as an initialisation
			// This will predict the next output in the array, which is what we 
			// want for time series data
			INDArray testInit = Nd4j.zeros(1,LEARNSTRING_INTS_LIST.size(), 1);
			testInit.putScalar(LEARNSTRING_INTS_LIST.indexOf(LEARNARR[LEARNARR.length - 1]), 1);

			// run one step -> IMPORTANT: rnnTimeStep() must be called, not
			// output()
			// the output shows what the net thinks what should come next
			//INDArray output = net.rnnTimeStep(testInit);
			INDArray output = net.output(testInit);
			
			int sampledCharacterIdx = Nd4j.getExecutioner().exec(new IMax(output), 1).getInt(0);

            // print the output
            System.out.print(sampledCharacterIdx + " " + LEARNSTRING_INTS_LIST.get(sampledCharacterIdx));


			// now the net should guess LEARNSTRING.length more characters
//            for (int dummy : LEARNARR) {
//
//                // first process the last output of the network to a concrete
//                // neuron, the neuron with the highest output has the highest
//                // chance to get chosen
//            	int sampledCharacterIdx = Nd4j.getExecutioner().exec(new IMax(output), 1).getInt(0);
//
//                // print the chosen output
//                System.out.print(LEARNSTRING_INTS_LIST.get(sampledCharacterIdx));
//
//                
//                // use the last output as input
//                INDArray nextInput = Nd4j.zeros(1, LEARNSTRING_INTS_LIST.size(), 1);
//                nextInput.putScalar(sampledCharacterIdx, 1);
//                output = net.rnnTimeStep(nextInput);
//
//            }
			System.out.print("\n");
		}
	}
}
