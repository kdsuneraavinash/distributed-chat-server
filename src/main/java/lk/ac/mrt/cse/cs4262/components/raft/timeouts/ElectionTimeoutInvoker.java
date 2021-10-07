package lk.ac.mrt.cse.cs4262.components.raft.timeouts;

import lk.ac.mrt.cse.cs4262.common.utils.NamedThreadFactory;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftController;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Timeout invoker for election timeouts.
 */
public class ElectionTimeoutInvoker implements AutoCloseable {
    private final ScheduledExecutorService scheduledExecutorService;

    @Nullable
    private RaftController raftController;

    /**
     * See {@link ElectionTimeoutInvoker}.
     */
    public ElectionTimeoutInvoker() {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1,
                new NamedThreadFactory("election-timeout"));
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
    public void setTimeout(int delay) {
        Optional.ofNullable(raftController).ifPresent(controller ->
                scheduledExecutorService.schedule(controller::handleElectionTimeout,
                        delay, TimeUnit.MILLISECONDS));
    }

    @Override
    public void close() throws Exception {
        scheduledExecutorService.shutdownNow();
    }
}
