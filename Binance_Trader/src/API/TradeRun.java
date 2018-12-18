package API;

import actions.BinanceAction;
import actions.MarketFetchAction;
import actions.SetupAction;
import actions.UserDataFetchAction;
import balance.BalanceHub;
import server.WebServer;
import traders.LSTMTrader;
import traders.MLSinewaveFitRidgeDetectorTrader;
import traders.RidgeDetector;


/**
 * Main Class where we run the Trader from. Note that we can test in the Test class.
 * @author alexg
 */
public class TradeRun {

	private static boolean testing = true;
	
	public static void main(String[] args) {
		
		// Important: first specify to the program whether testing or not. 
		// This is currently done thru BinanceAction.
		BinanceAction.setTestMode(testing);
		
		//MLSinewaveFitRidgeDetectorTrader trader = new MLSinewaveFitRidgeDetectorTrader(true);
		//RidgeDetector trader1 = new RidgeDetector();
		
		// LSTM Trader
		LSTMTrader lstm = new LSTMTrader();
		
		SetupAction sa = new SetupAction(Constants.BTC_USDT_MARKET_SYMBOL);
		//sa.getMinQty();
		
		MarketFetchAction mfa = new
				MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, 1);
		
		System.out.println("Starting trading... Start Price: " + mfa.getCurrentPrice());
		
		// Stores balances.
		BalanceHub hub = BalanceHub.getInstance();
		//TODO: Get the real balance from binance.
		if (testing) {
			hub.setValue(1000d, 0d);
		} else {
			UserDataFetchAction udfa = new UserDataFetchAction();
			hub.setValue(udfa.getNewestBal("USDT"), udfa.getNewestBal("BTC"));
		}
		
		
		// Starts the web server so we can see our results online.
		//WebServer server = new WebServer();
		//server.startServer();
		
		// Get the program okay to shutdown gracefully
		setupProgramClose(null, lstm);
		
		UserDataFetchAction udfa = new UserDataFetchAction();
		System.out.println(udfa.getNewestBal("BTC"));
		
		// Starts the trader. Very complex
		//trader.begin();
		//trader1.begin();
		lstm.begin();
	}
	
	private static void setupProgramClose(WebServer ws, LSTMTrader lstm) {
		/* 
		 * Important cleanup things we need to do before our program exits are put here. 
		 * Depending how the trader is set up, some params may be null, so we need to 
		 * check for this.
		 */
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				//System.out.println("Shutting Down...");
				if (lstm != null) 
					lstm.saveNet(); // Save the current model for later use.
				if (ws != null)	
					ws.stopServer(); // Shutdown the server properly
				// TODO: Clean up the other stuff here.
			}
		});
	}

}
