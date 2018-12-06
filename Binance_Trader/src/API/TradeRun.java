package API;

import actions.MarketFetchAction;
import actions.SetupAction;
import balance.BalanceHub;
import server.WebServer;
import traders.MLSinewaveFitRidgeDetectorTrader;
import traders.RidgeDetector;


/**
 * Main Class where we run the Trader from. Note that we can test in the Test class.
 * @author alexg
 */
public class TradeRun {


	public static void main(String[] args) {
		
		MLSinewaveFitRidgeDetectorTrader trader = new MLSinewaveFitRidgeDetectorTrader(true);
		RidgeDetector trader1 = new RidgeDetector(true);
		
		SetupAction sa = new SetupAction(Constants.BTC_USDT_MARKET_SYMBOL);
		//sa.getMinQty();
		
		MarketFetchAction mfa = new
				MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, 1);
		
		System.out.println("Starting trading... Start Price: " + mfa.getCurrentPrice());
		
		// Stores balances.
		BalanceHub hub = BalanceHub.getInstance();
		hub.setValue(1000d, 0d);
		
		// Starts the web server so we can see our results online.
		WebServer server = new WebServer();
		server.startServer();
		
		// Starts the trader. Very complex
		trader.begin();
		trader1.begin();
	}

}
