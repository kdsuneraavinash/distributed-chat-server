package lk.ac.mrt.cse.cs4262.common.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An invoker class that invokes a method in a specified period.
 */
public class PeriodicInvoker implements AutoCloseable {
    private final ScheduledExecutorService executorService;

    /**
     * Create a timed invoker. See {@link PeriodicInvoker}.
     *
     * @param name Name of invoker created.
     */
    public PeriodicInvoker(String name) {
        this.executorService = Executors.newScheduledThreadPool(1,
                new NamedThreadFactory(name));
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
    }

    public interface EventHandler {
        /**
         * Event handler for the timed event.
         */
        void handleTimedEvent();
    }
}
