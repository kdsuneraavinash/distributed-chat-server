package lk.ac.mrt.cse.cs4262.components.raft.timeouts;

import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftController;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ElectionTimeoutInvoker implements AutoCloseable {
    private final ScheduledExecutorService scheduledExecutorService;

    @Nullable
    private RaftController raftController;

    public ElectionTimeoutInvoker() {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    /**
     * @param raftController Raft controller.
     */
    public void initialize(RaftController raftController) {
        this.raftController = raftController;
    }

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
