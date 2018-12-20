package traders;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
import org.json.JSONArray;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.nd4j.linalg.schedule.MapSchedule;
import org.nd4j.linalg.schedule.ScheduleType;

import API.Constants;
import actions.MarketFetchAction;
import actions.OrderAction;
import actions.UserDataFetchAction;
import balance.BalanceHub;

public class LSTMTrader extends Trader {
	
	// Trade at least a dollars worth of stuff, otherwise not worth it.
	private static final double MIN_TRADE_VALUE_THRESHOLD = 1;
	// TODO: Empirically determine this constant. Will affect how much we trade
	private static final double STD_MARKET_DEVIATION = 0.00001;
	
	private static final int AVG_CONSTANT = 8000; // order of mag of price.
	private static final int RANGE_CONSTANT = 100; // order of mag of how much price changes in 1hr.
	
	// Trade fee percentage
	private static final double TRADE_FEE_RATE = 0.00075;
	
	private static final int UPDATE_RATE_SEC = 60;
	
	// RNN dimensions
	private static final int HIDDEN_LAYER_WIDTH = 128;
	private static final int HIDDEN_LAYER_CONT = 4;
    private static final Random r = new Random(7894);
    
    private static final int DATA_FETCH_SIZE = 60; // Look 60 min into the past.
    // How many inputs we will look at once
    private static final int INPUT_SIZE = 59; // TODO: CHANGE THIS and the fetch size as you want.
    private static final int NUM_OUTPUT_PREDICTIONS = 1; // Predict for the next n timesteps.
    private static final int INPUT_LAYER_SIZE = DATA_FETCH_SIZE - INPUT_SIZE - NUM_OUTPUT_PREDICTIONS + 1;
	
	private MultiLayerNetwork net;
	//private NormalizerMinMaxScaler pre;
	
	/*
	 *  fPrev is very important, as it will allow us to provide `feedback' to our LSTM. This is really cool 
	 *  because in a sense it brings together reinforcement learning and LSTM's. This in hope will make our 
	 *  network highly adaptable and powerful.
	 */
	private double[] fPrev = new double[INPUT_SIZE];
	private boolean firstRun = true;
	
	public LSTMTrader() {
		super(UPDATE_RATE_SEC);
		// TODO Auto-generated constructor stub
		initTrainNetwork();
		
		// Here, we probably want to change our NN's hyper-parameters if possible, like lr and such.
		//net.conf.set<hyperparameter>(value);
	}
	
	/*
	 * This method will not only do the trading as with the other traders, but will also update the network with a 
	 * new piece of data, hopefully strengthening the model over time. This should also make the network able to 
	 * change over time as the market changes.
	 */
	@Override
	protected void update() {
		
		// First step: Get the past data (Same as other trader)
		MarketFetchAction mfa = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, DATA_FETCH_SIZE);
		// TODO: Adjust MFA so we can get a certain amount of data as opposed to just being at its mercy.
		JSONArray result = mfa.getResult();
		JSONArray sub;
		double[] f = new double[INPUT_SIZE];
		INDArray toPred = Nd4j.zeros(1,INPUT_SIZE + 2, 1);
		//int test = (int) (Math.random() * 4);
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		double avg = 0;
		for (int i = 0; i < INPUT_SIZE; i++) {
			// This ensures we get the most recent data points.
			sub = result.getJSONArray(result.length() - INPUT_SIZE + i);
			// Uncomment this when ready to test on real data
			double d = Double.parseDouble(sub.getString(4));
			// Comment this out when switching to real data
			//double d = Math.sin(Math.PI * (i + test) / 2) + 50; // Simple oscillating from 0 -> 1 -> 0 -> -1
			f[i] = d;
			if (d > max) {
				max = d;
			}
			if (d < min) {
				min = d;
			}
			avg += d;
		}
		avg /= INPUT_SIZE;
		
		for (int i = 0; i < INPUT_SIZE; i++) {
			// This ensures we get the most recent data points.
			sub = result.getJSONArray(result.length() - INPUT_SIZE + i);
			// Uncomment this when ready to test on real data
			double d = Double.parseDouble(sub.getString(4));
			// Comment this out when switching to real data
			//double d = Math.sin(Math.PI * (i + test) / 2) + 50; // Simple oscillating from 0 -> 1 -> 0 -> -1
			toPred.putScalar(new int[]{0, i, 0}, (d - min) / (max - min));
		}
		toPred.putScalar(new int[] {0, INPUT_SIZE, 0}, avg / AVG_CONSTANT);
		toPred.putScalar(new int[] {0,  INPUT_SIZE + 1, 0}, (max - min) / RANGE_CONSTANT);
		// Transform it before making our prediction
		//pre.transform(toPred);
		
		// Then feed this to the existing model, get a predicted price output.
		// Note that the model is already trained, so this should save a lot of time compared with other methods.
		INDArray output = net.output(toPred);
		//pre.revertLabels(output);
		String s = output.toString();
		System.out.println(s);
		double f_pred = Double.parseDouble(s.substring(1, s.length() - 1));
		// Don't forget to untransform it
		f_pred = f_pred * (max - min) + min;
		System.out.println("Current Price: " + Math.round(f[f.length - 1]) +"    Predicted price: " + f_pred);
		
		// Trade based on the predicted price (Same as other trader)
		// Then find the difference between the new estimate and the last known val
		double difference = f_pred - f[f.length - 1];
		// Now, get the optimal balance given the difference.
		BalanceHub hub = BalanceHub.getInstance();
		double balanceRisk = calcRiskForCrypto(difference);
		// Get the usd value of our balance.
		double usdVal = hub.getUSDValue();
		// And get the crypto Val.
		double cryptoVal = hub.getCryptoValue();
		// Now, we want to find our target valuation so we can calculate the difference and trade.
		double targetCryptoVal = (usdVal + cryptoVal) * balanceRisk;
		// Also want to get target USD Val as it will be useful for fee calculations
		double targetUSDVal = (usdVal + cryptoVal) - targetCryptoVal;
		// Find the difference between our target
		double toTradeVal = targetCryptoVal - cryptoVal;
		// If it tells us to trade an insignificant amount, then just stop.

		if (Math.abs(toTradeVal) < MIN_TRADE_VALUE_THRESHOLD) {
			/* TODO: Add logging to this */
			// We don't want to do an order but we still want to train our network, so do this b4 returning
			//finishTrain(f);
			return;
		}

		// This is an expression that calculates what our predicted profit is without accounting for trading fees.
		double predictedGrossProfit = (targetUSDVal - usdVal) + (f_pred - f[f.length - 1]) * (targetCryptoVal - cryptoVal);
		double fees = toTradeVal * TRADE_FEE_RATE;
		// See if the fees put us in the red, if they do, then don't trade
		if (predictedGrossProfit - fees <= 0) {
			/* TODO: Add logging to this */
			// We don't want to do an order but we still want to train our network, so do this b4 returning
			//finishTrain(f);
			return;
		}
		// If we made it here, then we are going through with the trade...

		// Now we want to carry out the trade. First, get the amount necessary needed to buy/sell.
		double toTradeQty = Math.abs(((double) ((int) (1000000d * toTradeVal / mfa.getCurrentPrice()))) / 1000000d);
		// Determine to buy or sell.
		boolean isBuyOrder = toTradeVal > 0 ? true : false;
		// Create the OrderAction object. Note that we want limit order to avoid bad trading
		OrderAction oa = new OrderAction(Constants.BTC_USDT_MARKET_SYMBOL, isBuyOrder, OrderAction.LIMIT_ORDER,
				toTradeQty, f[f.length - 1]);
		oa.execute();
		System.out.println(
				"Order executed, traded " + toTradeQty + " at " + new Date()/* + " Result: " + oa.getResult() */);
		// Now that the order has executed, update our Vals for use in the next iteration.
		if (testMode) {
			double finUsdVal = usdVal - (toTradeVal * (1 - TRADE_FEE_RATE));
			double finCryptVal = cryptoVal + (toTradeVal * (1 - TRADE_FEE_RATE));
			hub.setValue(finUsdVal, finCryptVal);
		} else { 
			// If not testing, we don't put the theoretical values. Rather, we put whatever we actually have to maintain accuracy.
			UserDataFetchAction udfa = new UserDataFetchAction();
			hub.setValue(udfa.getNewestBal("USDT"), udfa.getNewestBal("BTC"));
		}
		System.out.println("Total Value: " + hub.getValue() + "   USD: " + hub.getUSDValue() + "   Crypto: " + hub.getCryptoQty());
		//finishTrain(f);
		
	}
	
	
	//TODO: Make sure we normalize our data before we add this back in.
	private void finishTrain(double[] f) {
		// Finally, the unique step. Use the current price paired with the previous
		// price to get the error so we can fit and backpropagate
		if (!firstRun) {

			INDArray input = Nd4j.zeros(INPUT_LAYER_SIZE, INPUT_SIZE, 1);
			INDArray labels = Nd4j.zeros(INPUT_LAYER_SIZE, NUM_OUTPUT_PREDICTIONS, 1);
			for (int i = 0; i < INPUT_SIZE; i++) {
				// Put the last runs data as the input
				input.putScalar(new int[] { 0, i, 0 }, fPrev[i]);
			}
			// Put the current label as the ground truth label
			labels.putScalar(new int[] { 0, 0, 0 }, f[f.length - 1]);
			// Then create the dataset
			DataSet trainingData = new DataSet(input, labels);
			// And then train. //TODO: Parallelize?
			net.fit(trainingData);
			
			// Lastly, we don't need to do any more prediction this iteration, so we can
			// clear its current state. (I think)

		} else {
			// Don't do anything on the first run.
			firstRun = false;
		}
		// The just predicted price becomes the old predicted price.
		fPrev = f;
		// TODO: Currently we need to reset the state which kinda sucks and slows the
		// program down (albeit marginally)
		// Would be nice to update code to fully utilize the efficiency of the
		// rnnTimestep() method.
		net.rnnClearPreviousState();
		System.out.println("Trained...");
	}
	
	// Determines risk of trading based on predicted change in price.
	private double calcRiskForCrypto(double difference) {
		double z = difference / STD_MARKET_DEVIATION;
		if (z < -8.0) return 0.0;
        if (z >  8.0) return 1.0;
        double sum = 0.0, term = z;
        for (int i = 3; sum + term != sum; i += 2) {
            sum  = sum + term;
            term = term * z * z / i;
        }
		return 0.5 + sum * Math.exp(-z*z / 2) / Math.sqrt(2 * Math.PI);
	}
	
	/*
	 * This method is intended to train up a net if we have none already trained. This method thus should 
	 * only be called at the beginning of a run, and should only actually train a network once. Every other 
	 * time, it should just read a model from a file saved by the save method.
	 */
	private void initTrainNetwork() {
		// See if an existing model exists
		boolean modelExists = new File("model.zip").exists();
		if (modelExists) {
			
			System.out.println("Existing model found, Restoring...");
			try {
				net = ModelSerializer.restoreMultiLayerNetwork("model.zip", true);
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
			Map<Integer, Double> lrSchedule = new HashMap<>();
			int epochs = 500;
			lrSchedule.put(0, 0.0002); // iteration #, learning rate
		    //lrSchedule.put(40, 0.1);
//		    lrSchedule.put(6, 2d);
//		    lrSchedule.put(10, .1);
//		    lrSchedule.put(15, .01);
		    lrSchedule.put(epochs, 0.00002); // For post training, keep lr very low
			builder.updater(new Adam(new MapSchedule(ScheduleType.ITERATION, lrSchedule)));
			builder.weightInit(WeightInit.XAVIER);

			ListBuilder listBuilder = builder.list();

			// first difference, for rnns we need to use LSTM.Builder
			for (int i = 0; i < HIDDEN_LAYER_CONT; i++) {
				LSTM.Builder hiddenLayerBuilder = new LSTM.Builder();
				hiddenLayerBuilder.nIn(i == 0 ? INPUT_SIZE + 2 : HIDDEN_LAYER_WIDTH);
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
			
			/* 
			 * Here we get a bunch of data to initially train our network. 
			 */
			MarketFetchAction mfa = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, INPUT_SIZE + NUM_OUTPUT_PREDICTIONS);
			long oneMonthMillis = (long) 2.6e9; // 
			long endRange = System.currentTimeMillis();
			long beginRange = endRange - oneMonthMillis;
			int trainDataPoints = 50; // Train initially on this many data points
			System.out.println("Training started...");
			
			for (int k = 0; k < epochs; k++) {
				INDArray input = Nd4j.zeros(trainDataPoints, INPUT_SIZE + 2, 1);
				INDArray labels = Nd4j.zeros(trainDataPoints, NUM_OUTPUT_PREDICTIONS, 1);
				for (int i = 0; i < trainDataPoints; i++) {
					long randTime = (long) (r.nextDouble() * oneMonthMillis) + beginRange;
					//System.out.println(randTime);
					mfa.setSampleTimestamp(randTime);
					JSONArray result = mfa.getResult();
					JSONArray sub;
					//int off = (int) (Math.random() * 4);
					
					// First handle labels
					// Put the current label as the ground truth label
					sub = result.getJSONArray(result.length() - NUM_OUTPUT_PREDICTIONS);
					// Uncomment this when ready to test on real data
					double dLab = Double.parseDouble(sub.getString(4));
					//double dLab = Math.sin(Math.PI * (INPUT_SIZE+off) / 2) + 50;
					
					
					// Then handle inputs
					double avg = 0;
					double max = Double.MIN_VALUE;
					double min = Double.MAX_VALUE;
					for (int j = 0; j < INPUT_SIZE; j++) {
						sub = result.getJSONArray(result.length() - INPUT_SIZE + j);
						// Uncomment this when ready to test on real data
						double d = Double.parseDouble(sub.getString(4));
						//double d = Math.sin(Math.PI * (j + off) / 2) + 50;
						if (d > max) {
							max = d;
						} 
						if (d < min) {
							min = d;
						}
						avg += d;
						
						
					}
					avg /= INPUT_SIZE; // +1 is dangerous.
					
					// Now actually put the label
					labels.putScalar(new int[] { i, 0, 0 }, (dLab - min) / (max - min));
					
					// Here we scale the data accordingly and add it to inputs.
					for (int j = 0; j < INPUT_SIZE; j++) {
						
						sub = result.getJSONArray(result.length() - INPUT_SIZE + j);
						double d = Double.parseDouble(sub.getString(4));
						//double d = Math.sin(Math.PI * (j + off) / 2) + 50;
						// Put the last runs data as the input
						input.putScalar(new int[] { i, j, 0 }, (d - min) / (max - min) );
					}
					// Because we lost some info with scaling, we add it back with the avg.
					input.putScalar(new int[] {i,  INPUT_SIZE, 0}, avg / AVG_CONSTANT);
					input.putScalar(new int[] {i, INPUT_SIZE + 1, 0}, (max - min) / RANGE_CONSTANT);
		

				}
				// Then create the dataset
				
				DataSet trainingData = new DataSet(input, labels);
				//pre = new NormalizerMinMaxScaler();
				//pre.fitLabel(true);
				//pre.fit(trainingData);
				//pre.transform(trainingData);
				
				//TODO: save the preprocessor -> pre.save(...);
				// And then train.
				//System.out.println(trainingData.exampleMeans().getDouble(0));
				net.fit(trainingData);
				// I don't know if this has any meaning but we'll see I guess.
				double score = net.gradientAndScore().getRight();
				System.out.println("Epoch " + k + " of initial training complete. Score: " + score);
			}
			System.out.println("Initial training ended.");
			
			// We will save once before we return.
			saveNet();
		}
	}
	
	
	/* 
	 * This method should be as simple as saving the model to a file, nothing more. We do this by utilizing Dl4J's 
	 * Model Serialization capability. 
	 */
	public void saveNet() {
		try {
			ModelSerializer.writeModel(net, "model.zip", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
