package actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import API.Constants;
import security.SignatureFactory;

public class OrderAction extends BinanceAction {
	
	public static final String MARKET_ORDER = "MARKET";
	public static final String LIMIT_ORDER = "LIMIT";

	String orderID;
	String orderSymbol;
	double orderQty;
	String side;
	String orderType;
	// This is updated when execute is called.
	String result;
	
	public OrderAction(String symbol, boolean isBuyOrder, String type, double qty) {
		super(testMode ? "v3/api/order/test" : "v3/api/order");
		orderType = type;
		side = isBuyOrder ? "BUY" : "SELL";
		orderQty = qty;
		orderSymbol = symbol;
	}

	public void specifyOrderID(String id) {
		orderID = id;
	}

	public void execute() {

		try {
			URI sellUri = new URIBuilder(location)
					.setParameter("symbol", orderSymbol)
					.setParameter("side", side)
					.setParameter("type", orderType)
				//	.setParameter("timeInForce", "GTC")
					.setParameter("quantity", orderQty + "")
					.setParameter("newOrderRespType", "RESULT")
					.setParameter("recvWindow", "5000")
					.setParameter("timestamp", Long.toString(System.currentTimeMillis()))
					.build();

			String queryString1 = sellUri.toString().substring(sellUri.toString().indexOf('?') + 1);

			String signature = SignatureFactory.generateHMACSHA256Signature(Constants.PRIVATE_KEY, queryString1);

			URI sellUriSigned = new URIBuilder(sellUri).setParameter("signature", signature).build();

			String queryString = sellUriSigned.toString().substring(sellUriSigned.toString().indexOf('?') + 1);

			HttpPost httppost = new HttpPost("https://api.binance.com/api/v3/order/test");
			httppost.setHeader(Constants.KEY_HEADER, Constants.PUBLIC_KEY);
			httppost.setHeader("Content-Type", Constants.CONTENT_TYPE + "; " + Constants.ENCODING);
			StringEntity se = new StringEntity(queryString);
			httppost.setEntity(se);

			HttpClient client = HttpClients.createDefault();

			HttpResponse response = client.execute(httppost); // THE "CRUX" STEP
			result = parseServerResponse(response);

		} catch (URISyntaxException e) {

			e.printStackTrace();

		} catch (ClientProtocolException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();
		}
		// If something goes terribly wrong
		//return null;
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
	
	public String getResult() {
		return result;
	}

}
