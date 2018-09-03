package actions;

public class TradeFetchAction extends BinanceAction {
	
	private static final String endpoint = "api/v1/aggTrades";
	
	
	public TradeFetchAction() {
		super(endpoint);
		
		
	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String parseServerResponse(Object response) {
		// TODO Auto-generated method stub
		return null;
	}
}
