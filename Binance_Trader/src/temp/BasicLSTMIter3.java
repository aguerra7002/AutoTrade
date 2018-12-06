package temp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.deeplearning4j.datasets.iterator.IteratorDataSetIterator;
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


public class BasicLSTMIter3 {

	// define a sentence to learn.
    // Add a special character at the beginning so the RNN learns the complete string and ends with the marker.
	private static final int[] LEARNARR_HARD = {1,2,3,4,5,4,3,2,1,2,3,4,5,4,3,2,1,2,3,4,5,4,3,2,1,2,3,4,5,4,3,2,1,2};
	private static final int[] LEARNARR_MED = {0,2,4,5,3,1,2,4,5,3,1,2,4,5,3,1,2,4,5,3};
	private static final int[] LEARNARR_EASY = {0,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1,2,1};
	
	private static final int[] LEARNARR = LEARNARR_HARD;

	// a list of all possible characters
	private static final List<Integer> LEARNSTRING_INTS_LIST = new ArrayList<>();

	// RNN dimensions
	private static final int HIDDEN_LAYER_WIDTH = 50;
	private static final int HIDDEN_LAYER_CONT = 3;
    private static final Random r = new Random(7894);
    // How many inputs we will look at once
    private static final int INPUT_SIZE = 3;
    private static final int NUM_OUTPUT_PREDICTIONS = 1; // Predict for the next 3 timesteps.
    private static final int INPUT_LAYER_SIZE = LEARNARR.length - INPUT_SIZE - NUM_OUTPUT_PREDICTIONS + 1;
    
	public static void main(String[] args) {

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
		// TODO: Explore different Loss functions and analyze which would be most useful for this 
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
		MultiLayerNetwork net = new MultiLayerNetwork(conf);
		net.init();
		net.setListeners(new ScoreIterationListener(1));
		// Here we create the train data. 
		INDArray input = Nd4j.zeros(INPUT_LAYER_SIZE, INPUT_SIZE, 1);
		INDArray labels = Nd4j.zeros(INPUT_LAYER_SIZE, NUM_OUTPUT_PREDICTIONS, 1);
		// loop through our sample-sentence
		for (int j = 0; j < LEARNARR.length - NUM_OUTPUT_PREDICTIONS - INPUT_SIZE; j++) {
			for (int i = 0; i < INPUT_SIZE; i++) {
				
				int currentInt = LEARNARR[j + i];
				input.putScalar(new int[] { j, i, 0}, currentInt);
				
				
			}
			for (int i = 0; i < NUM_OUTPUT_PREDICTIONS; i++) {
				int outInt = LEARNARR[j + i + INPUT_SIZE];
				labels.putScalar(new int[] { j, i, 0 }, outInt);
			}
		}
		DataSet trainingData = new DataSet(input, labels);

		// some epochs
		for (int epoch = 0; epoch < 500; epoch++) {

			System.out.println("Epoch " + epoch);
			IteratorDataSetIterator iter = new IteratorDataSetIterator(trainingData.iterator(), INPUT_LAYER_SIZE);
			for (int i = 0; i < 1; i++) { // TODO: Make this better at batching properly.
				// train the data
				net.fit(iter.next());
				//net.computeGradientAndScore();
				//System.out.println(trainingData.toString());

				// clear current stance from the last example
				net.rnnClearPreviousState();
			}

			// put the last character into the rnn as an initialisation
			// This will predict the next output in the array, which is what we 
			// want for time series data
			
			INDArray testInit = Nd4j.zeros(1,INPUT_SIZE, 1);
			System.out.print("In: ");
			int randStart = (int) (Math.random() * 10);
			for (int i = 0; i < INPUT_SIZE; i++) {
				testInit.putScalar(new int[]{0, i, 0}, LEARNARR[randStart + i]);
				System.out.print(LEARNARR[randStart + i] + " ");
			}
			System.out.println();
			// run one step -> IMPORTANT: rnnTimeStep() must be called, not
			// output()
			// the output shows what the net thinks what should come next
			INDArray output = net.rnnTimeStep(testInit);
			String s = output.toString();
			long out = Math.round(Double.parseDouble(s.substring(1, s.length() - 1)));
			System.out.println("Out: " + out);
			System.out.println("Raw: " + s);
			//INDArray output = net.output(testInit);
			
			//double out = Nd4j.getExecutioner().exec(new IMax(output), 1).getDouble();
			//System.out.println("Out: " + out);
            // print the output
            //System.out.print(sampledCharacterIdx + " " + LEARNSTRING_INTS_LIST.get(sampledCharacterIdx));


			// now the net should guess LEARNSTRING.length more characters
//            for (int dummy : LEARNARR) {
//                // first process the last output of the network to a concrete
//                // neuron, the neuron with the highest output has the highest
//                // chance to get chosen
//            	int sampledCharacterIdx = Nd4j.getExecutioner().exec(new IMax(output), 1).getInt(0);
//
//                // print the chosen output
//                System.out.print(LEARNSTRING_INTS_LIST.get(sampledCharacterIdx) + " ");
//
//                
//                // use the last output as input
//                INDArray nextInput = Nd4j.zeros(1, LEARNSTRING_INTS_LIST.size(), 1);
//                nextInput.putScalar(sampledCharacterIdx, 1);
//                output = net.rnnTimeStep(nextInput);
//
//            }
//			System.out.print("\n");
		}
	}
}
