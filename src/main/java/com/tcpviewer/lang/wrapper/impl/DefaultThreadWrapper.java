package com.tcpviewer.lang.wrapper.impl;

import com.tcpviewer.lang.wrapper.ThreadWrapper;

/**
 * Default implementation of ThreadWrapper that delegates to java.lang.Thread.
 * Provides zero-overhead delegation for production use.
 */
public class DefaultThreadWrapper implements ThreadWrapper {

    private final Thread delegate;

    /**
     * Creates a new ThreadWrapper wrapping a Thread with the given Runnable.
     *
     * @param target the Runnable to execute
     */
    public DefaultThreadWrapper(Runnable target) {
        this.delegate = new Thread(target);
    }

    /**
     * Creates a new ThreadWrapper wrapping a Thread with the given Runnable and name.
     *
     * @param target the Runnable to execute
     * @param name   the name of the new thread
     */
    public DefaultThreadWrapper(Runnable target, String name) {
        this.delegate = new Thread(target, name);
    }

    /**
     * Creates a new ThreadWrapper wrapping an existing Thread.
     * This is used for wrapping Thread.currentThread().
     *
     * @param thread the thread to wrap
     */
    public DefaultThreadWrapper(Thread thread) {
        this.delegate = thread;
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void join() throws InterruptedException {
        delegate.join();
    }

    @Override
    public void join(long millis) throws InterruptedException {
        delegate.join(millis);
    }

    @Override
    public void interrupt() {
        delegate.interrupt();
    }

    @Override
    public boolean isAlive() {
        return delegate.isAlive();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setDaemon(boolean on) {
        delegate.setDaemon(on);
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public boolean isInterrupted() {
        return delegate.isInterrupted();
    }

    @Override
    public void run() {
        delegate.run();
    }
}
