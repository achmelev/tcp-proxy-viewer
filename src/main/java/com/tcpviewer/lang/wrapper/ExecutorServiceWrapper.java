package com.tcpviewer.lang.wrapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mockable wrapper interface for java.util.concurrent.ExecutorService.
 * Provides abstraction over ExecutorService for testability without JDK 25 Mockito limitations.
 * Enables testing of thread pool management without actual thread execution.
 */
public interface ExecutorServiceWrapper {

    /**
     * Submits a Runnable task for execution.
     *
     * @param task the task to submit
     */
    void submit(Runnable task);

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed,
     * but no new tasks will be accepted.
     */
    void shutdown();

    /**
     * Attempts to stop all actively executing tasks, halts the processing of waiting tasks,
     * and returns a list of the tasks that were awaiting execution.
     *
     * @return list of tasks that never commenced execution
     */
    List<Runnable> shutdownNow();

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return true if this executor terminated and false if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Returns true if this executor has been shut down.
     *
     * @return true if this executor has been shut down
     */
    boolean isShutdown();

    /**
     * Returns true if all tasks have completed following shut down.
     *
     * @return true if all tasks have completed following shut down
     */
    boolean isTerminated();
}
