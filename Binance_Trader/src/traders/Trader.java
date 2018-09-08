package traders;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* This class handles all the timing/ update stuff for the trader
 * but still leaves a lot to be done in the individual traders which
 * opens the door for heavy customization.
 */
public abstract class Trader implements Runnable {
	
	// TODO: Find out a good value for this number. (Probably
	protected static final long DEFAULT_STARTING_TIMESTAMP_MS =  1527811200; // This is June 1, 2018. Probably fine.
	// This will only be used if going in test mode 
	protected static double currentTimestamp;
	
	// boolean to determine if we are in test mode.
	protected boolean testMode;
	
	// Time between calling update()
	int updateRateSec;
	
	// To execute all our tasks.
	ScheduledExecutorService executor;
	
	public Trader(int updateRateSecs, boolean isTestMode) {
		this.updateRateSec = updateRateSecs;
		this.testMode = isTestMode;
		if (this.testMode) {
			// Set default timestamp
			currentTimestamp = DEFAULT_STARTING_TIMESTAMP_MS;
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
	
	public void setTimeStamp(long timestamp) {
		currentTimestamp = timestamp;
	}
	
	/* 
	 * Abstract method for the user to implement. Gets called 
	 * once every updateRateSecs
	 */
	protected abstract void update();

}
