package com.tcpviewer.lang.wrapper.factory;

import com.tcpviewer.lang.wrapper.ExecutorServiceWrapper;

/**
 * Factory interface for creating executor service wrappers.
 * Centralizes executor service creation for testability and dependency injection.
 * Enables mocking of thread pool behavior in unit tests.
 */
public interface ExecutorServiceFactory {

    /**
     * Creates a cached thread pool executor service that creates new threads as needed.
     * Threads that have been idle for sixty seconds are terminated and removed from the cache.
     *
     * @param threadFactory the thread factory to use for creating new threads
     * @return an ExecutorServiceWrapper wrapping a cached thread pool
     */
    ExecutorServiceWrapper createCachedThreadPool(ThreadFactory threadFactory);
}
