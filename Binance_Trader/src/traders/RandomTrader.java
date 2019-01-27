package traders;

import java.util.Random;

import API.Constants;
import actions.MarketFetchAction;

public class RandomTrader extends Trader {
	
	
	private static int UPDATE_RATE = 60;
	
	public RandomTrader() {
		super(60);
		
	}

	@Override
	protected void update() {
		// TODO Auto-generated method stub
		MarketFetchAction mfa = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, 60);
		
		double currentPrice = mfa.getCurrentPrice();
		
		Random r = new Random(System.currentTimeMillis());
		
		// Have the random price fluctuate
		double diff = r.nextGaussian() * 7;
		
		double predictedPrice = currentPrice + diff;
		
		int res = tradeGivenPredictedPrice(predictedPrice, currentPrice, mfa);
		
		// Parse result here.
		
		
		System.out.println("Random Trade.");
		
	}

}
