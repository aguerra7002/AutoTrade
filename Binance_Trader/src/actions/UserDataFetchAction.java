package actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import API.Constants;
import security.SignatureFactory;

public class UserDataFetchAction extends BinanceAction {

	
	private static final String USER_DATA_ENDPOINT = "api/v3/account";

	JSONObject cur = null;
	
	public UserDataFetchAction() {
		super(USER_DATA_ENDPOINT);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void execute() {
		
		try {
			URI sellUri = new URIBuilder(location)
					.setParameter("timestamp", Long.toString(System.currentTimeMillis()))
					.build();

			String queryString1 = sellUri.toString().substring(sellUri.toString().indexOf('?') + 1);

			String signature = SignatureFactory.generateHMACSHA256Signature(Constants.PRIVATE_KEY, queryString1);

			URI sellUriSigned = new URIBuilder(sellUri).setParameter("signature", signature).build();

			//String queryString = sellUriSigned.toString().substring(sellUriSigned.toString().indexOf('?') + 1);

			HttpGet httpget = new HttpGet("https://api.binance.com/api/v3/order/test");
			httpget.setHeader(Constants.KEY_HEADER, Constants.PUBLIC_KEY);
			httpget.setHeader("Content-Type", Constants.CONTENT_TYPE + "; " + Constants.ENCODING);
			//StringEntity se = new StringEntity(queryString);
			
			httpget.setURI(sellUriSigned);

			HttpClient client = HttpClients.createDefault();

			HttpResponse response = client.execute(httpget); // THE "CRUX" STEP
			cur = new JSONObject(parseServerResponse(response));
			

		} catch (URISyntaxException | IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected String parseServerResponse(Object response) {

		HttpEntity entity = ((HttpResponse) response).getEntity();

		// Read the contents of an entity and return it as a String.
		String content = "";
		try {
			content = EntityUtils.toString(entity);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return content;
	}
	
	public double getNewestBal(String symbol) {
		execute();
		double balance = -1;
		try {
			JSONArray bals = cur.getJSONArray("balances");
			for (int i = 0; i < bals.length(); i++) {
				JSONObject bal = bals.getJSONObject(i);
				if (bal.getString("asset").equals(symbol)) {
					balance = Double.parseDouble(bal.getString("free"));
					break;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (balance == -1) System.out.println("No valid balance found, returning...");
		
		return balance;
	}

}
