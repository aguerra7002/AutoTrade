package actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;

public abstract class BinanceAction {
	
	private static final String BASE_ENDPOINT = "https://api.binance.com/";
	protected URI location;
	
	// 
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
	public abstract void execute();
	
	// Return something meaningful about what the action did
	public abstract String parseServerResponse(Object response);
	
}
