package nl.vu.psy.ams.suite.tools;
/*
 * Simple helper class that defines a Runnable that
 * can be stopped without using 'interrupt' like methods.
 * This can be used to let a thread stop gracefully instead of
 * being interrupted.
 */
public abstract class StoppableRunnable implements Runnable {

	private boolean	stopped	= false;

	public synchronized boolean isStopped() {
		return stopped;
	}

	public synchronized void startThread() {
		stopped = false;
	}

	public synchronized void stopThread() {
		stopped = true;
	}
}
