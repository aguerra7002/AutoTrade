package traders;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.json.JSONArray;

import API.Constants;
import actions.MarketFetchAction;
import actions.OrderAction;
import balance.BalanceHub;

/* This class is a trader that aims to beat the market by 
 * looking at the previous several data points of the market and 
 * then use these to predict the next data using concavity 
 * Essentially just a 2nd derivative approximation.
 */
public class MLSinewaveFitRidgeDetectorTrader extends Trader {
	
	// Trade at least a dollars worth of stuff, otherwise not worth it.
	private static final double MIN_TRADE_VALUE_THRESHOLD = 1;
	// TODO: Empirically determine this constant. Will affect how much we trade
	private static final double STD_MARKET_DEVIATION = 0.00001;
	
	//Sine wave is of the form f(x) = a sin(bx + c) + d. These are the constants we will try and learn.
	private double a = 0;
	private double b = 0;
	private double c = 0;
	private double d = 0;
	private double e = 0; //***
	/* 
	 * This is the tolerance to which we minimize our error, in other words we will stop training when 
	 * | oldError - newError | < this value.  
	 * TODO: Empirically find a good value for this.
	 */ 
	private static double DONE_LEARNING = .001;
	
	/* 
	 * This number has to be wisely chosen. Too big and we will be using expired/obsolete data to predict, 
	 * not to mention the extra calculations necessary. However, if we make it too small, our estimate will 
	 * be too prone to outlier observations or slightly abnormal behavior.  
	 */
	private static final int NUM_DATA = 60;
	/*
	 * This number also needs to be carefully chosen, it will be the minimum number of data points/minutes after a
	 * ridge that we will start trading again. We want to wait in case another ridge happens right after as the 
	 * market is probably unstable, but we also need enough data points to produce a good periodic model.
	 */
	private static final int MIN_DATA_TO_TRADE = 10;
	/* 
	 * Not declaring as final because I may want to add something that adjusts the learning rate
	 * for better optimization.
	 */
	private double learningRate = .00001;
	private boolean shouldWriteToCSV;
	private boolean firstRun;
	//TODO: Implement this to write trade history to CSV
	private File csv;
	
	// Update once a minute;
	private static final int UPDATE_RATE = 60;
	int count = 0;
	public MLSinewaveFitRidgeDetectorTrader(boolean writeToCSV) {
		super(UPDATE_RATE);
		shouldWriteToCSV = writeToCSV;
		firstRun = true;
		csv = new File("out.csv");
		try {
			FileWriter fw = new FileWriter(csv, true);
			PrintWriter pw = new PrintWriter(fw);
			StringBuilder sb = new StringBuilder();
			sb.append("MarketPrice,Total Value,USD Value,Price Error,Traded?");
			pw.println(sb.toString());
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void update() {
		// TODO Auto-generated method stub
		
		/*
		 *  One thing that could potentially be really cool to add would be a 
		 *  MAX_VALID_RESULT_TIME_MS variable that would cause the result of 
		 *  mfa to get a new result 
		 */
		
		MarketFetchAction mfa = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, NUM_DATA);
		JSONArray result = mfa.getResult();
		JSONArray sub;
		double min = -1;
		double max = -1;
		double sum = 0;
		double[] f = new double[NUM_DATA];
		for (int i = 0; i < result.length(); i++) {
			sub = result.getJSONArray(i);
			//TODO: Uncomment line below to get real data
			f[i] = Double.parseDouble(sub.getString(4));
			/* The following code keeps track of the minimum, max, and rolling sum of the data (to be averaged
			 * later). The purpose of this is to be used to set a,b,c and d to initial values in the correct 
			 * 'convex pool' that will (hopefully) yield a global minimum. Note that we need only do this one 
			 * time, as once a, b, c, and d are set, they will not change significantly with the loss and gain 
			 * of one data point (in theory).
			 */
			if (firstRun) {
				sum += f[i];
				if (min < 0 || f[i] < min) {
					min = f[i];
				} 
				if (f[i] > max) {
					max = f[i];
				}
			}
			
			// This is just test data. Further data might include adding random jitter.
			// If our algorithm can't fit this, it is wrong.
			//f[i] = 40 * Math.sin(.1 * i + 1) + 7000 + ((Math.random() * 20) - 10);
		}
		// `diff` is just gonna be used to calculate std deviation.
		//double diff = f[f.length - 1] - f[f.length - 2];
		// This function will return -1 if it predicts a ridge will occur. 
		// Otherwise, it will return the number of minutes (x vals) since last ridge 
		int lastRidge = getRidgeIfOneExists(f);
		
		if (lastRidge == -1) {
			// Trade like a ridge
		} else if (lastRidge >= MIN_DATA_TO_TRADE){
			// Indicates we have a cosine like response.
			
			// This will update a, b, c, d
			//trainModel(f, lastRidge);
			//learningRate = 1;
			//DONE_LEARNING = .001;
			//if (firstRun) {
				a = .5 * (max - min); // good heuristic formula for amplitude.
				b = .1; // This is known to be a pretty good starting point for b
			    c = 1; // TODO: Maybe in the future, implement a way to guess this value;
				d = sum / NUM_DATA; // Average price across our 60 observations. Should key us in as to the correct d value.
				e = (f[NUM_DATA - 1] - f[0]) / NUM_DATA; //*** Linear slope term
				//System.out.println(a + " " + b + " " + c + " " + d);
			//}
			
			trainModel(f, lastRidge);
			
			// Now plug into function with x + 1 timestamp to predict next value
			int x_new = lastRidge + 1;
			// Predict the value according to our periodic model.
			double f_pred = h(x_new);
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
			// Find the difference between our target 
			double toTradeVal = targetCryptoVal - cryptoVal;
			// If it tells us to trade an insignificant amount, then just stop.
			if (Math.abs(toTradeVal) < MIN_TRADE_VALUE_THRESHOLD) {
				System.out.println(toTradeVal); 
				if (shouldWriteToCSV) addCSVEntry(mfa.getCurrentPrice(), hub.getValue(), hub.getUSDValue(), difference, "N", f);
				return;
			}
			// Now we want to carry out the trade. First, get the amount necessary needed to buy/sell.
			double toTradeQty = Math.abs(((double)((int) (1000000d * toTradeVal / mfa.getCurrentPrice()))) / 1000000d);
			// Determine to buy or sell.
			boolean isBuyOrder = toTradeVal > 0 ? true : false;
			// Create the OrderAction object.
			OrderAction oa = new OrderAction(Constants.BTC_USDT_MARKET_SYMBOL, isBuyOrder, toTradeQty);
			oa.execute();
			System.out.println("Order executed, traded " + toTradeQty  + " at " + new Date()/*+ " Result: " + oa.getResult()*/);
			// Now that the order has executed, update our Vals for use in the next iteration.
			hub.setValue(usdVal - toTradeVal, targetCryptoVal);
			System.out.println("Total Value: " + hub.getValue() + "   USD: " + hub.getUSDValue() + "   Crypto: " + hub.getCryptoQty());
			// CSV writing stuff
			if (shouldWriteToCSV) addCSVEntry(mfa.getCurrentPrice(), hub.getValue(), hub.getUSDValue(), difference, "Y", f);
		} else {
			/* 
			 * If we are here, then we have just come out of a ridge, and do not have enough 
			 * data to predict a periodic function, so for now (TODO: Maybe have something 
			 * happen here if there is margin to reap) we will do nothing. This sort of 
			 * represents an unstable point in the market. It could suddenly get stable, or it 
			 * could go immediately go back into a ridge, we don't really know.
			 */
		}
		
	}
	

	
	
	/*
	 *  Idk what to expect really from this function, but ideally it will output a number 
	 *  0-1 that will say what percent of our balance should be in crypto, and then 1-x 
	 *  will tell us our percent balance for USD (Tether essentially). Then, we will have 
	 *  another function that will calculate what this translates to in terms of how much 
	 *  to buy/sell. Idk its gonna be a complicated mess, but hopefully it will work 
	 *  positively long term.
	 */
	private double calcRiskForCrypto(double difference) {
		/* 
		 * Idea: Empirically determine standard deviation of f_pred - f_prev.
		 * Then assume normal distribution. Then, for a given f_pred - f_prev, 
		 * we can calculate the probability that the difference is positive or 
		 * negative for a given probability. For example, if the difference is
		 * predicted to be .05, with a standard deviation of .3 (these are 
		 * completely made up figures), then the probability would be (in equiv.
		 * TI-84 terms, normalcdf( 0, \infty, 0.05, 0.3 ) = .566, which is what
		 * this is what we would return. Then, the program will go on to try and 
		 * get our balance to this desired amount. Hopefully, this will work lol.
		 * 
		 * P.S. Perhaps Standard deviation changes according some variables? So 
		 * much later this could be a learned/calculated figure (Also may change 
		 * for each market).
		 */
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
	
	/**
	 * This method will adjust a, b, c, and d to fit a periodic function defined by 
	 * f(x) = a * sin (b * x + c) + d using a simple gradient descent method. See the 
	 * paper for details as to how the formulas were derived. 
	 * @param f - Specifies the double array with the last NUM_DATA price vals for the 
	 * market. 
	 * @param pts - Specifies the number of minutes/points since the last ridge. We will 
	 * only use the last [pts] points of f[].
	 */
	private void trainModel(double[] f, int pts) {
		long startTime = System.nanoTime();
		// Before we do our fancy GD, we need to do our less fancy `find b by guess and check`
//		double MIN = -.3;
//		double MAX = .3;
//		int NUM_DATA = 200;
//		double INCREMENT = (MAX - MIN) / (NUM_DATA - 1);
//		double lowestError = -1;
//		double lowestYieldingB = -1;
//		for (double currentB = MIN; currentB <= MAX; currentB += INCREMENT) {
//			// Technically this won't calculate error, but will be a linear multiple of it (assuming pts is constant, which for testing it is.
//			double raw = 0; 
//			for (int i = f.length - pts; i < f.length; i++) {
//				raw += Math.pow((Math.sin(currentB * i) - f[i]), 2);
//			}
//			if (lowestError < 0 || raw < lowestError) {
//				lowestError = raw;
//				lowestYieldingB = currentB;
//			}
//
//		} 
//		// Finally, we have the b that is "close enough". So we set it to this.
//		b = lowestYieldingB;
		/*
		 * The general form for a parameter in gradient descent is
		 * a_{j+1} = a_j - r * D_j J(a) 
		 * where a_j is the j-th generation of an arbitrary parameter,
		 * r is the learning rate, D_j is the partial derivative with 
		 * respect to the j-th parameter (a_j), and J is the error function
		 */
		
		// The x value we use is the most recent price val.
		//double x = f[f.length - 1];
		boolean learning = true;
		// Initially give error an invalid value.
		double prevError = -1;
		double error = -1;
		while (learning) {
			if ((System.nanoTime() - startTime) / 1e9 > 30) {
				System.out.println("UH-OH " + a + "sin( " + b + "x + " + c + " ) + " + d + " + " + e + "x");//***
			}
			double sum_a = 0;
			double sum_b = 0;
			double sum_c = 0;
			double sum_d = 0;
			double sum_e = 0;//***
			double error_sum = 0;
			for (int i = f.length - pts; i < f.length; i++) {
				// We sum all the previous errors.
				int x_i = pts - f.length + i;
				double diff = h(x_i) - f[i];
				error_sum += diff * diff;
				sum_d += diff; 
				sum_a += diff * Math.sin(b * x_i);
				sum_b += diff * a * x_i * Math.cos(b * x_i + c);
				sum_c += diff * a * Math.cos(b * x_i + c);
				sum_e += diff * x_i;//***
			}
			// Don't forget to divide by pts at the end.
			sum_a /= (double) pts;
			sum_b /= (double) pts;
			sum_c /= (double) pts;
			sum_d /= (double) pts;
			sum_e /= (double) pts;//***
			// This is our error function (J(x))
			error = error_sum * 1/2 * pts ; 
			//addCSVEntry(error, 0d, 0d, 0d, "", f);
			/*if (error == 0) {
				System.out.println("No Error");
				break; // If there is no error, then the values are perfect, and we may exit.
			} else*/ if (Math.abs(error - prevError) < DONE_LEARNING && prevError >= 0) {
				learning = false;
			} else {
				prevError = error;
			}
			
			/*
		 	*  Note that we can't yet assign these values to a, b, c, and d, as 
		 	*  this would screw up the other calculations, so we put them in 
		 	*  temporary variables for the time being.
		 	*/
			double temp_a = a - learningRate * 100 * sum_a;
			double temp_b = b - learningRate * .1 * sum_b; // Learning rate on this guy has to be very delicate as highly non-convex
			double temp_c = c - learningRate * 100 * sum_c;
			double temp_d = d - learningRate * 100 * sum_d;
			double temp_e = e - learningRate * 100 * sum_e;//***
		
			// Now we can set these.
			a = temp_a;
			b = temp_b;
			c = temp_c;
		    d = temp_d;
			e = temp_e;//***
			
			
		}
		double time = (double)(System.nanoTime() - startTime) / 1e9;
		System.out.println(a + "sin( " + b + "x + " + c + " ) + " + d + " + " + e + "x   Error: " + error + "   Time: " + time + " seconds");//***
		//addCSVEntry(0d, 0d, 0d, 0d, "", f);
	}
	
	/**
	 * Hypothesis function, which is just a periodic function that 
	 * takes in an x, and returns a y, based on the current parameter.
	 * @param x - x value passed into function. To make numbers a little 
	 * nicer, we will make all the x's ints, and fit our curve to that.
	 */
	private double h (int x) { 
		return a * Math.sin(b * x + c) + d + e * x; //***
	}
	
	/**
	 * This method will attempt to detect if a ridge is imminent or, more easily, 
	 * if one has recently happened.
	 * @param f - double array of previous data points.
	 * @return - returns -1 if a ridge is presently occuring, otherwise it will 
	 * return the number of minutes (max is NUM_DATA) since the last ridge.
	 */
	private int getRidgeIfOneExists(double[] f) {
		
		// TODO: Implement this method (May need to add params).
		
		return NUM_DATA;
	}
	
	private void addCSVEntry(double mp, double v, double usd, double df, String traded, double[] f) {
		PrintWriter pw = null;
		try {
			FileWriter fw = new FileWriter(csv, true);
			pw = new PrintWriter(fw);
			StringBuilder sb = new StringBuilder();
			sb.append(mp + "," + v + "," + usd + "," + df + "," + traded);
//			for (int i = 0; i < f.length; i++) {
//				sb.append(i + "," + f[i] + "," + h(i) + "\n");
//			}
			//sb.append(mp); //except do error lol
			if (pw != null) {
				pw.println(sb.toString());
				count++;
				//System.out.println(count);
			}
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		if (count == 2) {
//			pw.close();
//			System.exit(1);
//		}
	}

}
