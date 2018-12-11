package actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;

/* 
 * The goal of this class is to grab all the available market data at a given instance and 
 * package in such a way that someone can easily use.
 */
public class MarketFetchAction extends BinanceAction {
	
	
	/*
	 * TODO: Would be cool af if you made this such that for a given symbol, only 
	 * one MarketFetchAction would be created and maintained regularly. Holy shit
	 * would be maybe even cooler if we could have this implement Runnable and have
	 * the Binance Actions just being dank as shit chillin and updating regularly.
	 */

	// Points to spot to get market data.
	private static final String endpoint = "api/v1/klines";
	
	// Market symbol
	String currentSymbol;
	// Number of data points we want.
	private int limit;
	
	// For getting data at different timestamps
	private long sampleTimestamp = -1;
	
	// This is what is updated when execute is called.
	JSONArray result;
	
	public MarketFetchAction(String symbol, int limit) {
		super(endpoint);
		this.limit = limit;
		currentSymbol = symbol;
	}

	@Override
	protected void execute() {
		Executor executor = Executor.newInstance();
		//System.out.println(location + " " + currentSymbol);
		try {
			URIBuilder base = new URIBuilder(location)
					.setParameter("symbol", currentSymbol)
					.setParameter("limit", limit + "")
					.setParameter("interval", "1m");
			
			if (sampleTimestamp != -1) {
				System.out.println("Shouldn't be here.");
				base.setParameter("startTime", sampleTimestamp + "");
			}
			// If in test mode we need to get the data from a certain timestamp.
			if (testMode) {
				base.setParameter("startTime", currentTimestamp + "");
			} 
								

			URI baseUri = base.build();
			String baseReq = parseServerResponse(executor.execute(Request.Get(baseUri)));
			//System.out.println(baseReq);
			result = new JSONArray(baseReq);
			
		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected String parseServerResponse(Object response) {

		try {
			return ((Response) response).returnContent().asString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}
	
	public void setSymbol(String s) {
		currentSymbol = s;
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public String getSymbol() {
		return currentSymbol;
	}
	
	public int getLimit() {
		return limit;
	}
	
	public long getSampleTimestamp() {
		return sampleTimestamp;
	}
	
	public void setSampleTimestamp(long t) {
		sampleTimestamp = t;
	}

	public double getCurrentPrice() {
		// To be sure we get the most current price, we do this.
		execute();
		// Get the most recent price (closing price of last index
		return getPriceAtIndex(result.length() - 1);
	}
	
	public double getPriceAtIndex(int i) {
		return Double.parseDouble(result.getJSONArray(i).getString(4));
	}
	
	public JSONArray getResult() {
		execute();
		// TODO Auto-generated method stub
		return result;
	}
	
	
}
