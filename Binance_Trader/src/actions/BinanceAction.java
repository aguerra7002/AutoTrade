package actions;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class BinanceAction {
	
	protected static final String BASE_ENDPOINT = "https://api.binance.com/";
	protected URI location;
	
//	// This is June 1, 2018. Probably fine.
//	protected static final long DEFAULT_STARTING_TIMESTAMP_MS =  1527811200; 
//	// This will only be used if going in test mode 
	protected static long currentTimestamp;
//	
//	// boolean to determine if we are in test mode.
	protected static boolean testMode;
	
	
	public BinanceAction(String endpoint) {

		try {
			location = new URI(BASE_ENDPOINT + endpoint);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * This method will update some data in the class.
	 */
	protected abstract void execute();
	
	// Return something meaningful about what the action did
	protected abstract String parseServerResponse(Object response);
	
	public static void setTimestamp(long timestamp) {
		currentTimestamp = timestamp;
	}
	
	public static void setTestMode(boolean isTestMode) {
		testMode = isTestMode;
	}
	
	public static boolean getTestMode() {
		return testMode;
	}
	
}
