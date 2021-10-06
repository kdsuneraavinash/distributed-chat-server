package lk.ac.mrt.cse.cs4262.components.raft.timeouts;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftController;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcTimeoutInvoker implements AutoCloseable {
    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<ServerId, Future<?>> scheduledTasks;

    @Nullable
    private RaftController raftController;

    public RpcTimeoutInvoker() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        this.scheduledTasks = new HashMap<>();
        this.scheduledExecutorService = scheduledThreadPoolExecutor;
    }

    /**
     * @param raftController Raft controller.
     */
    public void initialize(RaftController raftController) {
        this.raftController = raftController;
    }

    public void setTimeout(ServerId serverId, int delay) {
        Optional.ofNullable(raftController).ifPresent(controller -> {
            Future<?> future = scheduledExecutorService
                    .schedule(() -> controller.handleRpcTimeout(serverId),
                            delay, TimeUnit.MILLISECONDS);
            scheduledTasks.put(serverId, future);
        });
    }

    public void cancelTimeout(ServerId serverId) {
        Optional.ofNullable(scheduledTasks.remove(serverId))
                .ifPresent(future -> future.cancel(true));
    }

    @Override
    public void close() throws Exception {
        scheduledExecutorService.shutdownNow();
    }
}
