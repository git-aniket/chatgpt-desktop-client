package nl.vu.psy.ams.suite.tools;

import java.util.LinkedList;

/*
 * Class that can be used to remember started threads.
 * Used to close resources when exiting the app. Before exiting,
 * all threads are stopped first.
 */
public class ThreadServer {
	private static LinkedList<Thread> activeThreads = new LinkedList<Thread>();

	public static void addNewThread(Thread thread) {
		activeThreads.add(thread);
	}

	public static Thread getNewThread(Runnable run) {
		Thread newThread = new Thread(run);
		activeThreads.add(newThread);
		return newThread;
	}

	public static void removeThread(Thread thread) {
		activeThreads.remove(thread);
	}

	public static void stopAllThreads() {
		for (Thread t : activeThreads) {
			// t.interrupt();
			try {
				t.join(1);
			} catch (InterruptedException e) {
			}
		}
		activeThreads.clear();
	}
}
