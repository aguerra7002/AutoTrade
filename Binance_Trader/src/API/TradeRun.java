package API;

import actions.MarketFetchAction;
import actions.SetupAction;
import balance.BalanceHub;
import server.WebServer;
import traders.MLSinewaveFitRidgeDetectorTrader;



/**
 * Main Class where we run the Trader from. Note that we can test in the Test class.
 * @author alexg
 */
public class TradeRun {


	public static void main(String[] args) {
		
		SetupAction sa = new SetupAction(Constants.BTC_USDT_MARKET_SYMBOL);
		sa.getMinQty();
		
		MarketFetchAction mfa = new
		MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, 1);
		
		System.out.println("Starting trading... Start Price: " + mfa.getCurrentPrice());
		
		BalanceHub hub = BalanceHub.getInstance();
		hub.setValue(1000d, 0d);
		
		WebServer server = new WebServer();
		server.startServer();
		
		MLSinewaveFitRidgeDetectorTrader trader = new MLSinewaveFitRidgeDetectorTrader(true);

		trader.begin();

	}

}
