package API;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.json.JSONArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import actions.MarketFetchAction;
import logging.Logger;



/**
 * 
 * Simple test class where we can test code from. Same as TradeRun structurally. 
 */
public class Test {
	
	
	public static void main(String[] args) throws ClientProtocolException, IOException, URISyntaxException {
		//SetupAction sa = new SetupAction(Constants.BTC_USDT_MARKET_SYMBOL);
		//sa.getMinQty();
		//MarketFetchAction mfa = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, 1);
		//System.out.println("Starting trading... Start Price: " + mfa.getCurrentPrice());
		//BalanceHub hub = BalanceHub.getInstance();
		//hub.setValue(1000d, 0d);
		//WebServer server = new WebServer();
		//server.startServer();
		//MLSinewaveFitRidgeDetectorTrader trader = new MLSinewaveFitRidgeDetectorTrader(true);

		//trader.begin();
		
//		TradeFetchAction tfa = new TradeFetchAction(Constants.BTC_USDT_MARKET_SYMBOL);
//		double density = tfa.getTradeDensity();
//		System.out.println(density);
		//RidgeDetector trader = new RidgeDetector();
		//trader.begin();
		int INPUT_SIZE = 59;
		Logger log = new Logger();
		log.addFile("rnnout", true);
		MarketFetchAction mfa = new MarketFetchAction(Constants.BTC_USDT_MARKET_SYMBOL, 60);
		// TODO: Adjust MFA so we can get a certain amount of data as opposed to just being at its mercy.
		JSONArray result = mfa.getResult();
		JSONArray sub;
		double[] f = new double[INPUT_SIZE];
		INDArray toPred = Nd4j.zeros(1,INPUT_SIZE + 1, 1);
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		double avg = 0;
		for (int i = 0; i < INPUT_SIZE; i++) {
			// This ensures we get the most recent data points.
			sub = result.getJSONArray(result.length() - INPUT_SIZE + i);
			// Uncomment this when ready to test on real data
			double d = Double.parseDouble(sub.getString(4));
			log.addLineToFile(new StringBuilder("" + d), "rnnout");
			// Comment this out when switching to real data
			//double d = Math.sin(Math.PI * (i + test) / 2) + 50; // Simple oscillating from 0 -> 1 -> 0 -> -1
			f[i] = d;
			if (d > max) {
				max = d;
			}
			if (d < min) {
				min = d;
			}
			avg += d;
		}
		avg /= INPUT_SIZE;
		
		for (int i = 0; i < INPUT_SIZE; i++) {
			// This ensures we get the most recent data points.
			sub = result.getJSONArray(result.length() - INPUT_SIZE + i);
			// Uncomment this when ready to test on real data
			double d = Double.parseDouble(sub.getString(4));
			// Comment this out when switching to real data
			//double d = Math.sin(Math.PI * (i + test) / 2) + 50; // Simple oscillating from 0 -> 1 -> 0 -> -1
			d = (d - min) / (max - min);
			f[i] = d;
			toPred.putScalar(new int[]{0, i, 0}, d);
		}
		//toPred.putScalar(new int[] {0, INPUT_SIZE, 0}, avg / 10000);
		toPred.putScalar(new int[] {0,  INPUT_SIZE , 0}, (max - min) / 100);
		// Transform it before making our prediction
		//pre.transform(toPred);
		
		// Then feed this to the existing model, get a predicted price output.
		// Note that the model is already trained, so this should save a lot of time compared with other methods.
		MultiLayerNetwork net = ModelSerializer.restoreMultiLayerNetwork("model.zip", true);
		net.rnnClearPreviousState();
		
		for (int i = 0; i < 100; i++) {
			//INDArray output = net.output(toPred);
			INDArray output = net.rnnTimeStep(toPred);
			//pre.revertLabels(output);
			String s = output.toString();
			//System.out.println(s);
			double f_pred = Double.parseDouble(s.substring(1, s.length() - 1));
			
			// Update the array we predict with
			for (int j = 0; j < INPUT_SIZE - 1; j++) {
				f[j] = f[j+1];
				toPred.putScalar(new int[] {0, j, 0} , f[j]);
			}
			f[INPUT_SIZE - 1] = f_pred;
			toPred.putScalar(new int[] {0,  INPUT_SIZE - 1, 0}, f_pred);
			
			// Don't forget to untransform it for logging...
			f_pred = f_pred * (max - min) + min;
			log.addLineToFile(new StringBuilder("" + f_pred), "rnnout");
		}
		

		
		// ****************** BELOW IS OLD STUFF ***************************
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
