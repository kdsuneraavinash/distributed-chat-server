package lk.ac.mrt.cse.cs4262.components.raft.timeouts;

import lk.ac.mrt.cse.cs4262.common.utils.NamedThreadFactory;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftController;
import lombok.Synchronized;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Timeout invoker for election timeouts.
 */
public class ElectionTimeoutInvoker implements AutoCloseable {
    private final ScheduledExecutorService scheduledExecutorService;

    @Nullable
    private Future<?> currentTimeout;

    @Nullable
    private RaftController raftController;

    /**
     * See {@link ElectionTimeoutInvoker}.
     */
    public ElectionTimeoutInvoker() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
                        new NamedThreadFactory("election-timeout"));
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        this.scheduledExecutorService = scheduledThreadPoolExecutor;
    }

    /**
     * @param newRaftController Raft controller.
     */
    public void attachController(RaftController newRaftController) {
        this.raftController = newRaftController;
    }

    /**
     * Schedule to run in a specified delay.
     *
     * @param delay Delay in milliseconds.
     */
    @Synchronized
    public void setTimeout(int delay) {
        Optional.ofNullable(raftController).ifPresent(controller -> {
            Optional.ofNullable(currentTimeout).ifPresent(future -> future.cancel(true));
            currentTimeout = scheduledExecutorService.schedule(controller::handleElectionTimeout,
                    delay, TimeUnit.MILLISECONDS);
        });
    }

    @Override
    public void close() throws Exception {
        scheduledExecutorService.shutdownNow();
    }
}
