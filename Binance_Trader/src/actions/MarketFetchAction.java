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
	int limit;
	
	// This is what is updated when execute is called.
	JSONArray result;
	
	public MarketFetchAction(String symbol, int limit) {
		super(endpoint);
		this.limit = limit;
		currentSymbol = symbol;
	}

	@Override
	public void execute() {
		Executor executor = Executor.newInstance();
		//System.out.println(location + " " + currentSymbol);
		try {

			URI baseUri = new URIBuilder(location)
					.setParameter("symbol", currentSymbol)
					.setParameter("interval", "1m")
					.setParameter("limit", limit + "")
					.build();

			String baseReq = parseServerResponse(executor.execute(Request.Get(baseUri)));
			
			//System.out.println(baseReq);
			result = new JSONArray(baseReq);
			

		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String parseServerResponse(Object response) {

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

	public double getCurrentPrice() {
		// Get the most recent price (closing price of last index
		return getPriceAtIndex(result.length() - 1);
	}
	
	public double getPriceAtIndex(int i) {
		return Double.parseDouble(result.getJSONArray(i).getString(4));
	}
	
	public JSONArray getResult() {
		// TODO Auto-generated method stub
		return result;
	}
	
	
}
