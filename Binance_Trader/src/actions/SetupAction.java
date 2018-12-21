package actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import API.Constants;

public class SetupAction extends BinanceAction {
	// Will store all the important info.
	static JSONObject cur = null;
	static String marketSymbol;
	/*
	 *  A lot of Market properties are constant, and can be 
	 *  initialized once at the beginning of the run. This class 
	 *  aims to store all this info.
	 */
	public SetupAction(String symbol) {
		super("api/v1/exchangeInfo");
		marketSymbol = symbol;
	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		Executor executor = Executor.newInstance();
		//System.out.println(location + " " + marketSymbol);
		try {

			URI baseUri = new URIBuilder(location)
					//.setParameter("symbol", marketSymbol)
					.build();

			String baseReq = parseServerResponse(executor.execute(Request.Get(baseUri)));
			
			//System.out.println(baseReq);
			cur = new JSONObject(baseReq);
			

		} catch (URISyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double getMinQty() {
		execute();
		JSONArray arr = cur.getJSONArray("symbols");
		JSONObject temp;
		double minQty = -1;
		for (int i = 0; i < arr.length(); i++) {
			temp = arr.getJSONObject(i);
			if (temp.get("symbol").equals(Constants.BTC_USDT_MARKET_SYMBOL)) {
				JSONArray filters = temp.getJSONArray("filters");
				//System.out.println(filters);
				//minQty = Double.parseDouble(filters.getJSONObject(1).getString("minQty"));
			}
		}
		//System.out.println(minQty);
		return minQty;
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

}
