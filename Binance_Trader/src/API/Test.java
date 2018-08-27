package API;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import actions.MarketFetchAction;
import actions.SetupAction;
import balance.BalanceHub;
import server.WebServer;
import traders.MLSinewaveFitRidgeDetectorTrader;

public class Test {
	
	public static void main(String[] args) throws ClientProtocolException, IOException, URISyntaxException {
		SetupAction sa = new SetupAction(Constants.BTC_USDT_MARKET_SYMBOL);
		sa.getMinQty();
		MarketFetchAction mfa = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, 1);
		System.out.println("Starting trading... Start Price: " + mfa.getCurrentPrice());
		BalanceHub hub = BalanceHub.getInstance();
		hub.setValue(1000d, 0d);
		WebServer server = new WebServer();
		server.startServer();
		MLSinewaveFitRidgeDetectorTrader trader = new MLSinewaveFitRidgeDetectorTrader(true);

		trader.begin();
		
//		Executor executor = Executor.newInstance();
//		
//		URI baseUri = new URIBuilder()
//		        .setScheme("http")
//		        .setHost("www.api.binance.com")
//		        .setPath("/api/v1/exchangeInfo")
//		        //.setParameter("symbol", "ADABTC")
//		        .build();
//		
//		
//		String baseReq = executor.execute(Request.Get(baseUri))
//		        .returnContent().asString();
//		
//		JSONObject json_1 = new JSONObject(baseReq);
//		
//		//System.out.println(json_1.toString());
//		
//		URI priceUri = new URIBuilder()
//		        .setScheme("http")
//		        .setHost("www.api.binance.com")
//		        .setPath("/api/v3/ticker/price")
//		        .setParameter("symbol", "ADABTC")
//		        .build();
//		
//		
//		String priceReq = executor.execute(Request.Get(priceUri))
//		        .returnContent().asString();
//		
//		JSONObject json = new JSONObject(priceReq);
//		String price = json.getString("price");
//		
//		//System.out.println(price);
//		
//		URI sellUri = new URIBuilder()
//		        .setScheme("http")
//		        .setHost("www.api.binance.com")
//		        .setPath("/api/v3/order")
//		        .setParameter("symbol", "ADABTC")
//		        .setParameter("side", "SELL")
//		        .setParameter("type", "LIMIT")
//		        .setParameter("timeInForce", "GTC")
//		        .setParameter("quantity", "50")
//		        .setParameter("price", price)
//		        .setParameter("newOrderRespType", "RESULT")
//		        .setParameter("recvWindow", "5000")
//		        .setParameter("timestamp", Long.toString(System.currentTimeMillis()))
//		        .build();
//		
//		
//		
//		String queryString1 = sellUri.toString().substring(sellUri.toString().indexOf('?') + 1);
//		
//		String signature = generateHMACSHA256Signature(Constants.PRIVATE_KEY, queryString1);
//		
//		URI sellUriSigned = new URIBuilder(sellUri)
//				.setParameter("signature", signature)
//				.build();
//		
//		String queryString = sellUriSigned.toString().substring(sellUriSigned.toString().indexOf('?') + 1);
//		
//		//System.out.println(queryString);
//		
////		URL toSend = new URL(sellUri.toString().substring(0, sellUri.toString().indexOf('?')));
////		
////		HttpsURLConnection con = (HttpsURLConnection) toSend.openConnection();
////		con.setRequestMethod("POST");
////
////		//con.setRequestProperty("Content-length", String.valueOf(queryString.length())); 
////		con.setRequestProperty("Content-Type", Keys.CONTENT_TYPE); 
////		con.setDoOutput(true); 
////		con.setDoInput(true); 
////
////		DataOutputStream output = new DataOutputStream(con.getOutputStream());  
////
////		String finalS = queryString + "&signature=" + signature;
////		
////		output.writeBytes(queryString + "&signature=" + signature);
////
////		output.close();
////
////		DataInputStream input = new DataInputStream( con.getInputStream() ); 
////
////
////
////		for( int c = input.read(); c != -1; c = input.read() ) 
////		System.out.print( (char)c ); 
////		input.close(); 
////
////		System.out.println("Resp Code:"+con .getResponseCode()); 
////		System.out.println("Resp Message:"+ con .getResponseMessage());
//		
//		HttpPost httppost = new HttpPost("https://api.binance.com/api/v3/order/test");
//		httppost.setHeader(Constants.KEY_HEADER, Constants.PUBLIC_KEY);
//		httppost.setHeader("Content-Type", Constants.CONTENT_TYPE + "; " + Constants.ENCODING);
//		StringEntity se = new StringEntity(queryString);      
//		httppost.setEntity(se);
//		
//		HttpClient client = HttpClients.createDefault();
//		
//		HttpResponse response = client.execute(httppost); // THE "CRUX" STEP
//		HttpEntity entity = response.getEntity();
//
//        // Read the contents of an entity and return it as a String.
//        String content = EntityUtils.toString(entity);
//        //System.out.println(content);
//        
//        
//        MarketFetchAction mfa = new MarketFetchAction("ADABTC", 500);
//        mfa.setSymbol("ADABTC");
//        JSONArray result = (JSONArray) mfa.execute();
//        System.out.println(result.length());
//        for (int i = 0; i < result.length(); i++) {
//        	JSONArray sub = result.getJSONArray(i);
//        	for (int j = 0; j < sub.length(); j++) {
//        		switch (j) {
//        		case 0:
//        			System.out.println("Start Time : " + sub.getLong(j));
//        			break;
//        		case 1: 
//        			System.out.println("Start Price: " + sub.getString(j));
//        			break;
//        		case 2: 
//        			System.out.println("High Price : " + sub.getString(j));
//        			break;
//        		case 3: 
//        			System.out.println("Low  Price : " + sub.getString(j));
//        			break;
//        		case 4: 
//        			System.out.println("Close Price: " + sub.getString(j));
//        			break;
//        		case 5: 
//        			System.out.println("Volume   :   " + sub.getString(j));
//        			break;
//        		case 6: 
//        			System.out.println("Close Time : " + sub.getLong(j));
//        			break;
//        		case 8: 
//        			System.out.println("Num Trades : " + sub.getLong(j));
//        			break;
//        		default: 
//        			
//        			/* 
//        			 * NOTE: There are other pieces of data that I have omitted from
//        			 * the printing but they may still be useful. Be aware of this.
//        			 */
//        			
//        			break;
//        		}
//        	}
//        	System.out.println("**********************");
//        }
	}
	
}
