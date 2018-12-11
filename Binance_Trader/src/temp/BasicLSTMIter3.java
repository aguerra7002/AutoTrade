package temp;

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
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.RmsProp;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;


public class BasicLSTMIter3 {
	
	private static final double[] LEARNARR = new double[60];

	// RNN dimensions
	private static final int HIDDEN_LAYER_WIDTH = 50;
	private static final int HIDDEN_LAYER_CONT = 3;
    private static final Random r = new Random(7894);
    // How many inputs we will look at once
    private static final int INPUT_SIZE = LEARNARR.length - 1;
    private static final int NUM_OUTPUT_PREDICTIONS = 1; // Predict for the next 3 timesteps.
    private static final int INPUT_LAYER_SIZE = 1;
    
    
    /* !!!!!
     * 
     * This OFFSET variable is what is causing problems. High values of offset causes no learning 
     * to take place, but learning works very well with low offset. 
     * 
     * !!!!! */
    private static final int OFFSET = 50;
    
	public static void main(String[] args) {
		
		for (int i = 0; i < LEARNARR.length; i++) {
			LEARNARR[i] = Math.sin(Math.PI * (i) / 2) + OFFSET;
		}

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
		RnnOutputLayer.Builder outputLayerBuilder = new RnnOutputLayer.Builder(LossFunction.MSE);

		outputLayerBuilder.activation(Activation.SIGMOID); // Between 0 and 1, as we want.
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
		for (int j = 0; j < INPUT_LAYER_SIZE; j++) {
			for (int i = 0; i < INPUT_SIZE; i++) {
				
				double d = LEARNARR[j + i];
				input.putScalar(new int[] { j, i, 0}, d);
				
			}
			for (int i = 0; i < NUM_OUTPUT_PREDICTIONS; i++) {
				double out = LEARNARR[j + i + INPUT_SIZE];
				labels.putScalar(new int[] { j, i, 0 }, out);
			}
		}
		DataSet trainingData = new DataSet(input, labels);
		NormalizerMinMaxScaler preProc = new NormalizerMinMaxScaler();
		preProc.fit(trainingData);
		preProc.transform(trainingData);
		

		// some epochs
		for (int epoch = 0; epoch < 500; epoch++) {

			System.out.println("Epoch " + epoch);
			//IteratorDataSetIterator iter = new IteratorDataSetIterator(trainingData.iterator(), INPUT_LAYER_SIZE);
			//DataSet train = iter.next();
			//System.out.println(trainingData.toString());
			net.fit(trainingData);
			net.rnnClearPreviousState();
			
			
			INDArray testInit = Nd4j.zeros(1,INPUT_SIZE, 1);
			System.out.print("In: ");
			//int randStart = (int) (Math.random() * 10);
			for (int i = 0; i < INPUT_SIZE; i++) {
				testInit.putScalar(new int[]{0, i, 0}, LEARNARR[0 + i]);
				System.out.print(LEARNARR[0+ i] + " ");
			}
			System.out.println();
			// Have to make sure we scale the test data.
			preProc.transform(testInit);
			//System.out.println(testInit.toString());
			// run one step -> IMPORTANT: rnnTimeStep() must be called, not output()
			// the output shows what the net thinks what should come next
			INDArray output = net.rnnTimeStep(testInit);
			// Revert the prediction to comprehensible data.
			//System.out.println(output.toString());
			preProc.revertLabels(output);
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
