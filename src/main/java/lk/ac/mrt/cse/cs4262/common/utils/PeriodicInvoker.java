package lk.ac.mrt.cse.cs4262.common.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An invoker class that invokes a method in a specified period.
 */
public class PeriodicInvoker implements AutoCloseable {
    private static final int TERMINATION_WAIT_S = 10;

    private final ScheduledExecutorService executorService;

    /**
     * Create a timed invoker. See {@link PeriodicInvoker}.
     */
    public PeriodicInvoker() {
        this.executorService = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start the execution.
     *
     * @param eventHandler Event handler for timed events
     * @param initialDelay the time to delay first execution (milliseconds)
     * @param period       the period between successive executions (milliseconds)
     */
    public void startExecution(EventHandler eventHandler, int initialDelay, int period) {
        executorService.scheduleAtFixedRate(eventHandler::handleTimedEvent,
                initialDelay, period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
        executorService.shutdownNow();
        boolean terminated = executorService.awaitTermination(TERMINATION_WAIT_S, TimeUnit.SECONDS);
        if (!terminated) {
            throw new TimeoutException("timeout waiting for executor shutdown");
        }
    }

    public interface EventHandler {
        /**
         * Event handler for the timed event.
         */
        void handleTimedEvent();
    }
}
