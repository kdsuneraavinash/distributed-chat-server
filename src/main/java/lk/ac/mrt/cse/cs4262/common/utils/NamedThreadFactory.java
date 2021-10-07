package lk.ac.mrt.cse.cs4262.common.utils;

import lombok.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory to provide custom thread pool name.
 */
public class NamedThreadFactory implements ThreadFactory {
    private final AtomicInteger threadIndex;
    private final String threadPrefix;

    /**
     * Creates {@link NamedThreadFactory}.
     *
     * @param threadPrefix Name of thread pool.
     */
    public NamedThreadFactory(String threadPrefix) {
        this.threadIndex = new AtomicInteger(1);
        this.threadPrefix = threadPrefix;
    }

    @Override
    public Thread newThread(@NonNull Runnable runnable) {
        String threadName = String.format("%s-%d", threadPrefix, threadIndex.getAndIncrement());
        return new Thread(runnable, threadName);
    }
}
