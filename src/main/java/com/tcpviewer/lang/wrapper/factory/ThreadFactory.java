package com.tcpviewer.lang.wrapper.factory;

import com.tcpviewer.lang.wrapper.ThreadWrapper;

/**
 * Factory interface for creating thread wrappers.
 * Centralizes thread creation for testability and dependency injection.
 * Enables mocking of thread behavior in unit tests.
 */
public interface ThreadFactory {

    /**
     * Creates a new thread wrapper with the given Runnable.
     *
     * @param target the Runnable to execute
     * @return a ThreadWrapper wrapping a new thread
     */
    ThreadWrapper createThread(Runnable target);

    /**
     * Creates a new thread wrapper with the given Runnable and name.
     *
     * @param target the Runnable to execute
     * @param name   the name of the new thread
     * @return a ThreadWrapper wrapping a new named thread
     */
    ThreadWrapper createThread(Runnable target, String name);

    /**
     * Creates a new daemon thread wrapper with the given Runnable and name.
     *
     * @param target the Runnable to execute
     * @param name   the name of the new thread
     * @return a ThreadWrapper wrapping a new daemon thread
     */
    ThreadWrapper createDaemonThread(Runnable target, String name);

    /**
     * Returns a wrapper for the current thread.
     * Used for checking Thread.currentThread().isInterrupted().
     *
     * @return a ThreadWrapper wrapping the current thread
     */
    ThreadWrapper currentThread();
}
