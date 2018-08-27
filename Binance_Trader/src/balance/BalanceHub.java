package balance;

import API.Constants;
import actions.MarketFetchAction;

public class BalanceHub {
	
	// Singleton class as every user has a single Balance.
	private static BalanceHub bh = null;
	
	// Variables for knowing what Value/Price we started at so we can calculate relative gains
	private static double startValue;
	private static double startPrice;
	
	private static MarketFetchAction priceGrabber = null;
	
	// !!!!!!!! TODO: Probably best to create a Balance Class to manage balances for individual markets.
	//private static double value; // TODO: This variable will be useful later.
	private static double usdValue;
	private static double cryptoQty; 

	public static BalanceHub getInstance() {
		
		if (bh == null) {
			bh = new BalanceHub();
			cryptoQty = 0.0;
			usdValue = 0.0;
			startValue = -1.0d;
			startPrice = -1.0d;
			// Don't specify a symbol yet
			priceGrabber = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, 1);
			return bh;
		} 
		// If we already have one, just return that.
		return bh;
	}
	
	public void setValue(double usdVal, double cryptoVal) {
		usdValue = usdVal;
		cryptoQty = cryptoVal / getCurrentPrice(Constants.BTC_USDT_MARKET_SYMBOL);
		if (startValue == -1d) {
			startValue = usdVal + cryptoVal;
		}
	}

	public double getUSDValue() {
		return (double) Math.round(usdValue * 100) / 100;
	}
	
	public double getCryptoValue() {
		// Note that here, when getting the crypto value, we call getCurrentPrice. 
		// Never in BalanceHub do we manually update the price, that is handled 
	    // exclusively in MarketFetchAction.
		return (double) Math.round(cryptoQty * 
						getCurrentPrice(Constants.BTC_USDT_MARKET_SYMBOL) * 100) / 100d;
	}
	public double getValue() {
		return (double) Math.round(100 * (usdValue + getCryptoValue())) / 100d;
	}
	public double getCryptoQty() {
		return cryptoQty;
	}
	
	/**
	 * Method adjusts the usd value based on what crypto is added
	 * Symbol must have one side be usd, otherwise it will not work. 
	 * This method can also be used to subtract value if a negative qty 
	 * is passed in.
	 * @param symbol - Market symbol to use.
	 * @param qty - qty of crypto to add
	 * @return - returns 0 if succesful, -1 otherwise.
	 */
	public int addCryptoValue(String symbol, double qty) {
		// Fail if not a usdt market
		if (!symbol.endsWith("USDT")) return -1;
		
		usdValue += getCurrentPrice(symbol) * qty;
		return 0;
	}
	
	
	public double getCurrentPrice(String symbol) {
		priceGrabber.setSymbol(symbol);
		double price = priceGrabber.getCurrentPrice();
		if (startPrice == -1) {
			startPrice = price;
		}
		return price;
	}
	
	public double getStartValue() {
		return startValue;
	}
	
	public double getStartPrice() {
		return startPrice;
	}
	
}
