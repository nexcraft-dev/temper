package dev.nexcraft.temper.core;

import java.util.concurrent.Callable;

/**
 * Vendor-neutral Temper fault-tolerance execution contract.
 */
public interface FaultTolerance {
    /**
     * Executes the supplied task.
     *
     * @param task the task to execute
     * @param <T> the task result type
     * @return the task result
     * @throws Exception if task execution fails
     */
    <T> T execute(Callable<T> task) throws Exception;
}
