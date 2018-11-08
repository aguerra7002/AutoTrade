package traders;

import java.util.Date;

import org.json.JSONArray;

import API.Constants;
import actions.MarketFetchAction;
import logging.Logger;

/* 
 * The plan for this algorithm is still unclear. There are two main ways I see of detecting ridges. 
 * First, one could do some sort of moving average on the last 5 minutes of data and see if the average 
 * is significantly affected by the newest datapoint (if it is, then the price has changed a significant amount.
 * However, we may also be able to use candlestick data to calculate derivatives in some way so that we could see 
 * if the change in price at a certain point is exceptionally high or low (this would indicate that we have a ridge)
 */
public class RidgeDetector extends Trader {
	
	// How often we update. Because ridges happen can happen in a short amount of time, we will update 
	// relatively frequently. Subject to change
	private static final int UPDATE_RATE_SEC = 20;
	
	// This is the threshold for how much deviation we allow for in the price
	private static final double RIDGE_THRESHOLD = 2.1;
	
	// This will be how far back we look in the data. We don't want it to be too far back, so we will only look 
	// at the last 10 minutes of data.
	private static final int NUM_DATA = 20;
	
	// For writing to csv
	private String csvMain = "ridges.csv";
	Logger ridgeLogger;
	
	private static double lastRidge = -1;
	private static boolean isUpRidge = false;

	public RidgeDetector(boolean isTestMode) {
		super(UPDATE_RATE_SEC, isTestMode);
		
		// Logging setup
		ridgeLogger = new Logger();
		ridgeLogger.addFile(csvMain, true);
		// Here we would log the titles of categories and what not. 
		// Because I don't know exactly what we want to log yet I will leave this blank for now.
	}

	@Override
	protected void update() {
		
		/* 
		 * For ridge detection, the first approach I will try is to see if the newest price falls outside 
		 * x standard deviations of the set of the previous prices. If so, we can be relatively confident 
		 * that a ridge is in progress. To start, we will have x = 3 std devs.
		 */
		
		MarketFetchAction mfa = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, NUM_DATA);
		JSONArray result = mfa.getResult();
		JSONArray sub;
		double[] f = new double[NUM_DATA];
		double mean = 0;
		for (int i = 0; i < result.length(); i++) {
			
			sub = result.getJSONArray(i);
			
			f[i] = Double.parseDouble(sub.getString(4));
			mean += f[i];
		}
		mean /= NUM_DATA;
		
		// Calculate the standard deviation.
		double stdDev = 0;
		for (double p : f) {
			stdDev += Math.pow(mean - p, 2);
		}
		stdDev = Math.sqrt(stdDev / (double) (NUM_DATA - 1));
		
		// If the difference is outside of our range.
		if (Math.abs(f[NUM_DATA - 1] - mean) >= RIDGE_THRESHOLD * stdDev) {
			// We have a ridge, do something.
			StringBuilder sb = new StringBuilder();
			sb.append(mean + "," + stdDev + "," +f[NUM_DATA - 1] + "," + (new Date(currentTimestamp)) + ",1");
			ridgeLogger.addLineToFile(sb, csvMain);
			// Because we are in a ridge, the time since the last ridge is 0
			lastRidge = 0; 
			// Next, we will determine if it is an up ridge or a down ridge. Very simple to do.
			isUpRidge = f[NUM_DATA] > mean ? true : false;
			
			
			
		} else {
			// If we have no ridge, we can add to the time since the last ridge (in minutes)
			lastRidge += (double) UPDATE_RATE_SEC / 60;
			StringBuilder sb = new StringBuilder();
			sb.append(mean + "," + stdDev + "," +f[NUM_DATA - 1] + "," + (new Date(currentTimestamp)) + ",0");
			ridgeLogger.addLineToFile(sb, csvMain);
		}
		
	}
	
	// This method should return the number of minutes since the last ridge, or 0 if a ridge is currently happening. 
	public static double getLastRidge() {
		
		return lastRidge; 
		
	}
	
	// Returns the status of the last ridge as being an up or down. Default is down, so model should be careful here
	public static boolean isUpRidge() {
		
		return isUpRidge; 
		
	}
	



}
