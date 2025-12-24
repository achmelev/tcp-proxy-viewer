package com.tcpviewer.lang.wrapper.factory;

import com.tcpviewer.lang.wrapper.ThreadWrapper;
import com.tcpviewer.lang.wrapper.impl.DefaultThreadWrapper;

/**
 * Default implementation of ThreadFactory.
 * Creates DefaultThreadWrapper instances that delegate to java.lang.Thread.
 */
public class DefaultThreadFactory implements ThreadFactory {

    @Override
    public ThreadWrapper createThread(Runnable target) {
        return new DefaultThreadWrapper(target);
    }

    @Override
    public ThreadWrapper createThread(Runnable target, String name) {
        return new DefaultThreadWrapper(target, name);
    }

    @Override
    public ThreadWrapper createDaemonThread(Runnable target, String name) {
        DefaultThreadWrapper wrapper = new DefaultThreadWrapper(target, name);
        wrapper.setDaemon(true);
        return wrapper;
    }

    @Override
    public ThreadWrapper currentThread() {
        return new DefaultThreadWrapper(Thread.currentThread());
    }
}
