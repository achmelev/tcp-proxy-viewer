package com.tcpviewer.lang.wrapper;

/**
 * Wrapper interface for System class operations.
 * Allows for testing code that uses System.exit() without actually terminating the JVM.
 */
public interface SystemWrapper {

    /**
     * Terminates the currently running Java Virtual Machine.
     *
     * @param status exit status. By convention, a nonzero status code indicates abnormal termination.
     */
    void exit(int status);
}
