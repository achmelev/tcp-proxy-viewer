package com.tcpviewer.lang.wrapper.impl;

import com.tcpviewer.lang.wrapper.ExecutorServiceWrapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of ExecutorServiceWrapper that delegates to java.util.concurrent.ExecutorService.
 * Provides zero-overhead delegation for production use.
 */
public class DefaultExecutorServiceWrapper implements ExecutorServiceWrapper {

    private final ExecutorService delegate;

    /**
     * Creates a new ExecutorServiceWrapper wrapping an ExecutorService.
     *
     * @param delegate the ExecutorService to wrap
     */
    public DefaultExecutorServiceWrapper(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void submit(Runnable task) {
        delegate.submit(task);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }
}
