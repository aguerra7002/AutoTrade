package traders;

import multithreading.NotifyingThread;


/* 
 * The plan for this algorithm is still unclear. There are two main ways I see of detecting ridges. 
 * First, one could do some sort of moving average on the last 5 minutes of data and see if the average 
 * is significantly affected by the newest datapoint (if it is, then the price has changed a significant amount.
 * However, we may also be able to use candlestick data to calculate derivatives in some way so that we could see 
 * if the change in price at a certain point is exceptionally high or low (this would indicate that we have a ridge)
 */
public class RidgeDetector extends NotifyingThread {
	
	

	@Override
	public void doRun() {
		// TODO Auto-generated method stub
		
	}

}
