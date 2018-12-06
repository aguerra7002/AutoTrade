package server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import API.Constants;
import balance.BalanceHub;
import traders.Trader;

// The tutorial can be found just here on the SSaurel's Blog : 
// https://www.ssaurel.com/blog/create-a-simple-http-web-server-in-java
// Each Client Connection will be managed in a dedicated Thread
public class WebServer {
	static String dataToDisplay;
	HttpServer server;
	public WebServer() {
		dataToDisplay = "No Data Set";
		
		
		// This assigns another thread to update the value on the web. 
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(new Thread() {
			@Override
			public void run() {
				BalanceHub hub = BalanceHub.getInstance();
				double value = hub.getValue();
				double price = hub.getCurrentPrice(Constants.BTC_USDT_MARKET_SYMBOL);
				double startPrice = hub.getStartPrice();
				double startValue = hub.getStartValue();
				double valueChange = 100 * (value - startValue) / startValue;
				double priceChange = 100 * (price - startPrice) / startPrice;
				setDisplayData(new Date(Trader.getCurrentTimestamp()) + "\n" +
						"USD Value: $" + value + "\n" +
						"Value % Change: " + valueChange + "%\n" +
						"Price % Change: " + priceChange + "%");
			}
		}, 0, 10, TimeUnit.SECONDS); // Update the value every 10 seconds.
	}

	public void startServer() {
		server = null;
		try {
			
			server = HttpServer.create(new InetSocketAddress(8000), 0);
			server.createContext("/test", new MyHandler());
			server.setExecutor(null); // creates a default executor
			
			server.start();
			System.out.println("Successfully Started server.");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Very important method to call to ensure we actually get rid of the server.
	 */
	public void stopServer() {
		System.out.println("Shutting down server...");
		server.stop(0); // Exit gracefully
	}

	class MyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			String response = dataToDisplay;
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}
	
	private void setDisplayData(String dataAsString) {
		dataToDisplay = dataAsString;
		updateContext();
	}
	
	private void updateContext() {
		server.removeContext("/test");
		server.createContext("/test", new MyHandler());
	}
}
