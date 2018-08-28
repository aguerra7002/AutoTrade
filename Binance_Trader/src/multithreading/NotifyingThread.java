package multithreading;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * This class is for creating Threads in classes where we can then notify the class upon the completion of the 
 * Thread
 * @author alexg
 *
 */
public abstract class NotifyingThread extends Thread {
	private final Set<ThreadCompleteListener> listeners = new CopyOnWriteArraySet<ThreadCompleteListener>();

	public final void addListener(final ThreadCompleteListener listener) {
		listeners.add(listener);
	}

	public final void removeListener(final ThreadCompleteListener listener) {
		listeners.remove(listener);
	}

	private final void notifyListeners() {
		for (ThreadCompleteListener listener : listeners) {
			listener.notifyOfThreadComplete(this);
		}
	}

	@Override
	public final void run() {
		try {
			doRun();
		} finally {
			notifyListeners();
		}
	}

	public abstract void doRun();
}