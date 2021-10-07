package lk.ac.mrt.cse.cs4262.components.raft.timeouts;

import lk.ac.mrt.cse.cs4262.common.symbols.ServerId;
import lk.ac.mrt.cse.cs4262.common.utils.NamedThreadFactory;
import lk.ac.mrt.cse.cs4262.components.raft.controller.RaftController;
import lombok.Synchronized;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Timeout invoker for RPC timeouts.
 */
public class RpcTimeoutInvoker implements AutoCloseable {
    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<ServerId, Future<?>> scheduledTasks;

    @Nullable
    private RaftController raftController;

    /**
     * See {@link RpcTimeoutInvoker}.
     */
    public RpcTimeoutInvoker() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2,
                        new NamedThreadFactory("rpc-timeout"));
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        this.scheduledTasks = new HashMap<>();
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
     * @param serverId Server ID that is related to the RPC timeout.
     * @param delay    Delay in milliseconds.
     */
    @Synchronized
    public void setTimeout(ServerId serverId, int delay) {
        Optional.ofNullable(raftController).ifPresent(controller -> {
            Optional.ofNullable(scheduledTasks.remove(serverId))
                    .ifPresent(future -> future.cancel(true));

            Future<?> future = scheduledExecutorService
                    .schedule(() -> controller.handleRpcTimeout(serverId),
                            delay, TimeUnit.MILLISECONDS);
            scheduledTasks.put(serverId, future);
        });
    }

    /**
     * Cancel any scheduled timeouts.
     * Timeouts are not guaranteed to exit upon calling this.
     *
     * @param serverId Server ID that is related to the RPC timeout.
     */
    public void cancelTimeout(ServerId serverId) {
        Optional.ofNullable(scheduledTasks.remove(serverId))
                .ifPresent(future -> future.cancel(true));
    }

    @Override
    public void close() throws Exception {
        scheduledExecutorService.shutdownNow();
    }
}
