package nl.vu.psy.ams.suite.tools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
/*
 * Helper to provide a threadpool that is shared for
 * the entire app.
 */
public class CachedThreadPool {

	private static ExecutorService	exService	= Executors.newCachedThreadPool();

	public static void execute(Runnable runnable) {
		exService.execute(runnable);
	}

	public static Future<?> submit(Runnable runnable) {
		return exService.submit(runnable);
	}

	private CachedThreadPool() {

	}

}
