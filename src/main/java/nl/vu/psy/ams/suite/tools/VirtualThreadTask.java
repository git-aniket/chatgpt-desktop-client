package nl.vu.psy.ams.suite.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class VirtualThreadTask {

    private VirtualThreadTask() {
    }

    /**
     * Runs all tasks in parallel using virtual threads.
     * Waits for all tasks to complete.
     * Throws the first exception encountered.
     */
    public static void runInParallel(Runnable... tasks)
            throws InterruptedException, ExecutionException {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (Runnable task : tasks) {
                futures.add(executor.submit(task));
            }

            ExecutionException firstEx = null;

            for (Future<?> future : futures) {
                try {
                    future.get(); // wait + throw if exception occurred
                } catch (ExecutionException e) {
                    if (firstEx == null) {
                        firstEx = e;
                    }
                }
            }

            if (firstEx != null) {
                throw firstEx;
            }
        }
    }

    /**
     * Runs all tasks in parallel.
     * If any task fails, the rest are cancelled (fail-fast).
     * Equivalent behavior to StructuredTaskScope.ShutdownOnFailure.
     */
    public static void runInParallelFailFast(Runnable... tasks)
            throws InterruptedException, ExecutionException {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (Runnable task : tasks) {
                futures.add(executor.submit(task));
            }

            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    // cancel all other tasks
                    for (Future<?> c : futures) {
                        c.cancel(true);
                    }
                    throw e; // rethrow
                }
            }
        }
    }
}
