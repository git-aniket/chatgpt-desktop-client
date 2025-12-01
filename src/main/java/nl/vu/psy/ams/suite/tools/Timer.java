package nl.vu.psy.ams.suite.tools;

public class Timer {
	// A simple "stopwatch" class with millisecond accuracy
	private long	startTime, endTime;
	public long getTime() {
		return endTime - startTime;
	}
	public void start() {
		startTime = System.currentTimeMillis();
	}
	public void stop() {
		endTime = System.currentTimeMillis();
	}
}