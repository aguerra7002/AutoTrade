package traders;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import actions.BinanceAction;

/* This class handles all the timing/ update stuff for the trader
 * but still leaves a lot to be done in the individual traders which
 * opens the door for heavy customization.
 */
public abstract class Trader implements Runnable {
	
	// This is June 1, 2018. Probably fine.
	protected static final long DEFAULT_STARTING_TIMESTAMP_MS = 1527811200000l;
	// This is June 18, 2018. Allows us to test for 18 days, which is fine for our purposes.
	protected static final long OPTIONAL_END_TIMESTAMP_MS = 1529280000000l;
	
	// This will be the updated currentTimestamp. 
	protected static long currentTimestamp;
	
	// This will determine if we are in testMode
	protected static boolean testMode;
	
	
	// Time between calling update()
	int updateRateSec;
	
	// To execute all our tasks.
	ScheduledExecutorService executor;
	
	public Trader(int updateRateSecs, boolean isTestMode) {
		this.updateRateSec = updateRateSecs;
		testMode = isTestMode;
		BinanceAction.setTestMode(testMode);
		if (testMode) {
			// Set default timestamp
			currentTimestamp = DEFAULT_STARTING_TIMESTAMP_MS;
			BinanceAction.setTimestamp(currentTimestamp);
		}
	}
	
	public void begin() {
		if (testMode) {
			// In test mode, what we want to do is to run our process, advancing the timestamp ahead by 
			//updateRate as soon as one loop is done so we can test our algorithm efficiently and effectively.
			while(true) {
				// For logging time.
				long start = System.currentTimeMillis();
				// Don't want to put this on another Thread as this would cause many to be spawned at once, this is bad.
				update(); 
				// Then advance the time to the next update point and go again.
				currentTimestamp += (updateRateSec * 1000);
				BinanceAction.setTimestamp(currentTimestamp);
				System.out.println("Cycle Time: " + ((double) (System.currentTimeMillis() - start) / 1000d) + " seconds");
			}
			
		} else {
			// Indicates we are runnning in real time, not on test data.
			
			// Here, we want the Trader to run concurrently to be efficient/not use a ton of CPU usage
			executor = Executors.newScheduledThreadPool(1);
			// Now schedule this to call update() every updateRateSec
			executor.scheduleAtFixedRate(this, 0, updateRateSec, TimeUnit.SECONDS);
		}
	}
	
	public void stop() {
		// Here we shut down the thread. We can do this gracefully with the shutdown() method 
		// and nothing else.
		executor.shutdown();
	}
	
	
	@Override
	public void run() {
		update();
	}
	
	public int getUpdateRate() {
		return updateRateSec;
	}
	
	public static long getCurrentTimestamp() {
		return currentTimestamp;
	}
	
	public void setTimeStamp(long timestamp) {
		currentTimestamp = timestamp;
	}
	
	/* 
	 * Abstract method for the user to implement. Gets called 
	 * once every updateRateSecs
	 */
	protected abstract void update();

}
