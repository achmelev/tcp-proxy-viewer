package com.tcpviewer.lang.wrapper.factory;

import com.tcpviewer.lang.wrapper.ExecutorServiceWrapper;
import com.tcpviewer.lang.wrapper.impl.DefaultExecutorServiceWrapper;

import java.util.concurrent.Executors;

/**
 * Default implementation of ExecutorServiceFactory.
 * Creates DefaultExecutorServiceWrapper instances that delegate to java.util.concurrent.Executors.
 */
public class DefaultExecutorServiceFactory implements ExecutorServiceFactory {

    @Override
    public ExecutorServiceWrapper createCachedThreadPool(ThreadFactory threadFactory) {
        // Convert our ThreadFactory wrapper to JDK's ThreadFactory
        java.util.concurrent.ThreadFactory jdkThreadFactory = runnable -> {
            com.tcpviewer.lang.wrapper.ThreadWrapper wrapper = threadFactory.createThread(runnable);
            // Return the underlying thread for JDK compatibility
            // We need to extract the Thread object, so we'll use a workaround
            // by starting and immediately getting the thread that would be created
            return new Thread(runnable);
        };

        return new DefaultExecutorServiceWrapper(
            Executors.newCachedThreadPool(jdkThreadFactory)
        );
    }
}
