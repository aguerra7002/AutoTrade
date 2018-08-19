package traders;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* This class handles all the timing/ update stuff for the trader
 * but still leaves a lot to be done in the individual traders which
 * opens the door for heavy customization.
 */
public abstract class Trader implements Runnable {
	
	// Time between calling update()
	int updateRateSec;
	ScheduledExecutorService executor;
	public Trader(int updateRateSecs) {
		this.updateRateSec = updateRateSecs;
	}
	
	public void begin() {
		// Here, we want the Trader to run concurrently to be efficient/not use a ton of CPU usage
		executor = Executors.newScheduledThreadPool(1);
		// Now schedule this to call update() every updateRateSec
		executor.scheduleAtFixedRate(this, 0, updateRateSec, TimeUnit.SECONDS);
//		long curTime = System.currentTimeMillis();
//		update();
//		// TODO: Have a more sophisticated method than this for 
//		// handling the loop.
//		while (true) {
//			if (System.currentTimeMillis() - curTime > updateRateSec * 1000) {
//				// Reset the time and update.
//				curTime = System.currentTimeMillis();
//				update();
//			}
//		}
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
	
	/* 
	 * Abstract method for the user to implement. Gets called 
	 * once every updateRateSecs
	 */
	protected abstract void update();

}
