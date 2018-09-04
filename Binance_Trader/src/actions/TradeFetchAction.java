package actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;

public class TradeFetchAction extends BinanceAction {
	
	private static final String endpoint = "api/v1/aggTrades";
	
	// Stores the current raw json of the result.
	JSONArray cur = null;
	String marketSymbol;
	
	public TradeFetchAction(String symbol) {
		super(endpoint);
		marketSymbol = symbol;
	}

	@Override
	protected void execute() {

		Executor executor = Executor.newInstance();

		try {

			URI baseUri = new URIBuilder(location)
					.setParameter("symbol", marketSymbol)
					.build();

			String baseReq = parseServerResponse(executor.execute(Request.Get(baseUri)));
			
			//System.out.println(baseReq);
			cur = new JSONArray(baseReq);
			

		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public double getTradeDensity() {
		execute();
		if (cur == null) 
			return -1;
		long start = (long) cur.getJSONObject(0).get("T");
		long end = (long) cur.getJSONObject(499).get("T");
		return (double) (end - start) / (500d * 1000d);
	}

	@Override
	protected String parseServerResponse(Object response) {
		try {
			return ((Response) response).returnContent().asString();
		} catch (IOException e) {
			
			e.printStackTrace();
		}

		return "";
	}
}
