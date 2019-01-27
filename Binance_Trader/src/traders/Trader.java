package traders;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import API.Constants;
import actions.BinanceAction;
import actions.CancelAction;
import actions.MarketFetchAction;
import actions.OrderAction;
import actions.UserDataFetchAction;
import balance.BalanceHub;

/* This class handles all the timing/ update stuff for the trader
 * but still leaves a lot to be done in the individual traders which
 * opens the door for heavy customization.
 */
public abstract class Trader implements Runnable {
	
	// This is June 1, 2018. Probably fine.
	//protected static final long DEFAULT_STARTING_TIMESTAMP_MS = 1527811200000l;
	protected static final long DEFAULT_STARTING_TIMESTAMP_MS = 1541030400000l;
	// This is June 18, 2018. Allows us to test for 18 days, which is fine for our purposes.
	protected static final long OPTIONAL_END_TIMESTAMP_MS = 1529280000000l;
	
	// This will be the updated currentTimestamp. 
	protected static long currentTimestamp;
	
	// This will determine if we are in testMode
	protected static boolean testMode;
	
	
	// Time between calling update()
	int updateRateSec;
	
	// To execute all our tasks.
	ScheduledExecutorService executor;
	
	public Trader(int updateRateSecs) {
		this.updateRateSec = updateRateSecs;
		testMode = BinanceAction.getTestMode();
		BinanceAction.setTestMode(testMode);
		if (testMode) {
			// Set default timestamp
			currentTimestamp = DEFAULT_STARTING_TIMESTAMP_MS;
			BinanceAction.setTimestamp(currentTimestamp);
		}
	}
	
	public void begin() {
		if (testMode) {
			// In test mode, what we want to do is to run our process, advancing the timestamp ahead by 
			//updateRate as soon as one loop is done so we can test our algorithm efficiently and effectively.
			while(true) {
				// For logging time.
				long start = System.currentTimeMillis();
				// Don't want to put this on another Thread as this would cause many to be spawned at once, this is bad.
				update(); 
				// Then advance the time to the next update point and go again.
				currentTimestamp += (updateRateSec * 1000);
				BinanceAction.setTimestamp(currentTimestamp);
				System.out.println("Cycle Time: " + ((double) (System.currentTimeMillis() - start) / 1000d) + " seconds");
			}
			
		} else {
			// Indicates we are runnning in real time, not on test data.
			
			// Here, we want the Trader to run concurrently to be efficient/not use a ton of CPU usage
			executor = Executors.newScheduledThreadPool(1);
			// Now schedule this to call update() every updateRateSec
			executor.scheduleAtFixedRate(this, 0, updateRateSec, TimeUnit.SECONDS);
		}
	}
	
	public void stop() {
		// Here we shut down the thread. We can do this gracefully with the shutdown() method 
		// and nothing else.
		executor.shutdown();
	}
	
	
	@Override
	public void run() {
		 // This run() method is only called when we are not testing. 
		 // It is important that here, we cancel all orders before trying to create a new one
		// TODO: Maybe put this in OrderAction?
		Iterator<String> orders = OrderAction.getOrders();
		while (orders.hasNext()) {
			CancelAction ca = new CancelAction(Constants.BTC_USDT_MARKET_SYMBOL, orders.next());
			ca.execute();
		}
		
		// Now we can go ahead and potentially make a new order
		update();
	}
	
	public int getUpdateRate() {
		return updateRateSec;
	}
	
	public static long getCurrentTimestamp() {
		return currentTimestamp;
	}
	
	public void setTimeStamp(long timestamp) {
		currentTimestamp = timestamp;
	}
	
	/* 
	 * Abstract method for the user to implement. Gets called 
	 * once every updateRateSecs
	 */
	protected abstract void update();
	
	/*
	 * **************** GENERIC TRADING METHODS BELOW.
	 */
	
	// Trade at least a dollars worth of stuff, otherwise not worth it.
	private static final double MIN_TRADE_VALUE_THRESHOLD = 1;
	// TODO: Empirically determine this constant. Will affect how much we trade
	private static final double STD_MARKET_DEVIATION = 0.00001;
	
	// Trade fee percentage
	private static final double TRADE_FEE_RATE = 0.00075;
	
	/*
	 * Method called by children to trade given what the children predict the price will be. 
	 * While not necessarily used by traders if they trade not by predicting future price, it 
	 * is helpful for many cases.
	 * 
	 * Returns 1 if trade attempted, 0 if not worth trading bc not enough price change, -1 if fees too high.
	 */
	protected int tradeGivenPredictedPrice(double f_pred, double currentPrice, MarketFetchAction mfa) {
		// Then find the difference between the new estimate and the last known val
		double difference = f_pred - currentPrice;
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
			//System.out.println(toTradeVal); 
			return 0;
		}

		// This is an expression that calculates what our predicted profit is without accounting for trading fees.
		double predictedGrossProfit = (targetUSDVal - usdVal) + (difference) * (targetCryptoVal - cryptoVal);
		double fees = toTradeVal * TRADE_FEE_RATE;
		// See if the fees put us in the red, if they do, then don't trade
		if (predictedGrossProfit - fees <= 0) {
			return -1;
		}
		// If we made it here, then we are going through with the trade...

		// Now we want to carry out the trade. First, get the amount necessary needed to buy/sell.
		double toTradeQty = Math.abs(((double)((int) (1000000d * toTradeVal / mfa.getCurrentPrice()))) / 1000000d);
		// Determine to buy or sell.
		boolean isBuyOrder = toTradeVal > 0 ? true : false;
		// Create the OrderAction object. Note that we want limit order to avoid bad trading
		OrderAction oa = new OrderAction(Constants.BTC_USDT_MARKET_SYMBOL, isBuyOrder, OrderAction.LIMIT_ORDER, toTradeQty, currentPrice);
		oa.execute();
		System.out.println("Order executed, traded " + toTradeQty  + " at " + new Date()/*+ " Result: " + oa.getResult()*/);
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
		// We attempted a trade, so return 1
		return 1;
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

}
