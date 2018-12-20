package actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
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

	String orderID = null;
	String orderSymbol;
	double orderQty;
	double orderPrice;
	String side;
	String orderType;
	// This is updated when execute is called.
	String result;
	
	// Static variable to keep track of all orders over all OrderActions
	private static HashSet<String> activeOrders = new HashSet<String>();
	private static long orderNum = 0;
	
	private static final String ENDPOINT = "v3/api/order";
	private static final String ENDPOINT_TEST = "v3/api/order/test";
	
	public OrderAction(String symbol, boolean isBuyOrder, String type, double qty, double price) {
		super(testMode ? ENDPOINT_TEST : ENDPOINT);
		orderType = type;
		side = isBuyOrder ? "BUY" : "SELL";
		orderQty = qty;
		orderPrice = price;
		orderSymbol = symbol;
	}

	public void specifyOrderID(String id) {
		orderID = id;
	}

	public void execute() {

		try {
			URIBuilder sellUriBuilder = new URIBuilder(location)
					.setParameter("symbol", orderSymbol)
					.setParameter("side", side)
					.setParameter("type", orderType)
					.setParameter("timeInForce", "GTC")
					.setParameter("quantity", orderQty + "");
			
			if (orderType.equals(LIMIT_ORDER)) {
				sellUriBuilder = sellUriBuilder.setParameter("price", orderPrice + "");
			}
			
			if (orderID != null) {
				sellUriBuilder = sellUriBuilder.setParameter("newClientOrderId", orderID);
			} else { //TODO: Get rid of this else clause, its not really necessary at all.
				orderID = orderNum + "";
				sellUriBuilder = sellUriBuilder.setParameter("newClientOrderId", orderID);
				orderID = null;
			}
			
			URI sellUri = sellUriBuilder.setParameter("newOrderRespType", "RESULT")
					.setParameter("recvWindow", "5000")
					.setParameter("timestamp", Long.toString(System.currentTimeMillis()))
					.build();
			
			

			String queryString1 = sellUri.toString().substring(sellUri.toString().indexOf('?') + 1);

			String signature = SignatureFactory.generateHMACSHA256Signature(Constants.PRIVATE_KEY, queryString1);

			URI sellUriSigned = new URIBuilder(sellUri).setParameter("signature", signature).build();

			String queryString = sellUriSigned.toString().substring(sellUriSigned.toString().indexOf('?') + 1);
			
			String finalEndpoint = BASE_ENDPOINT + (testMode ? ENDPOINT_TEST : ENDPOINT);
			HttpPost httppost = new HttpPost(finalEndpoint);
			httppost.setHeader(Constants.KEY_HEADER, Constants.PUBLIC_KEY);
			httppost.setHeader("Content-Type", Constants.CONTENT_TYPE + "; " + Constants.ENCODING);
			StringEntity se = new StringEntity(queryString);
			httppost.setEntity(se);

			HttpClient client = HttpClients.createDefault();

			HttpResponse response = client.execute(httppost); // THE "CRUX" STEP
			result = parseServerResponse(response);
			
			System.out.println(result);
			
			orderNum++;

		} catch (URISyntaxException e) {

			e.printStackTrace();

		} catch (ClientProtocolException e) {

			e.printStackTrace();

		} catch (IOException e) {

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
	
	public static void orderCancelled(String orderID) {
		activeOrders.remove(orderID);
	}
	
	public String getResult() {
		return result;
	}

}
