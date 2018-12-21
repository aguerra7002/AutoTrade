package actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import API.Constants;
import security.SignatureFactory;

public class CancelAction extends BinanceAction {

	String orderID;
	String orderSymbol;
	String result;
	
	public CancelAction(String symbol, String orderID) {
		super("api/v3/order");
		orderSymbol = symbol;
		this.orderID = orderID;
	}

	public void specifyOrderID(String id) {
		orderID = id;
	}

	public void execute() {

		try {
			URI cancelUri = new URIBuilder(location)
					.setParameter("symbol", orderSymbol)
					.setParameter("origClientOrderId", orderID)
					.setParameter("timestamp", Long.toString(System.currentTimeMillis()))
					.build();

			String queryString1 = cancelUri.toString().substring(cancelUri.toString().indexOf('?') + 1);

			String signature = SignatureFactory.generateHMACSHA256Signature(Constants.PRIVATE_KEY, queryString1);

			URI cancelUriSigned = new URIBuilder(cancelUri).setParameter("signature", signature).build();

			HttpDelete httpdel = new HttpDelete("https://api.binance.com/api/v3/order");
			httpdel.setHeader(Constants.KEY_HEADER, Constants.PUBLIC_KEY);
			httpdel.setHeader("Content-Type", Constants.CONTENT_TYPE + "; " + Constants.ENCODING);
			
			httpdel.setURI(cancelUriSigned);

			HttpClient client = HttpClients.createDefault();

			HttpResponse response = client.execute(httpdel); // THE "CRUX" STEP
			result = parseServerResponse(response);
			// TODO: Remove the order from the hashset of orders in OrderAction
			System.out.println(result);
			OrderAction.orderCancelled(orderID);

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
