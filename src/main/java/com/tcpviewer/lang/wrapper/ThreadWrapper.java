package com.tcpviewer.lang.wrapper;

/**
 * Mockable wrapper interface for java.lang.Thread.
 * Provides abstraction over Thread for testability without JDK 25 Mockito limitations.
 * Enables testing of concurrent code without actual thread execution.
 */
public interface ThreadWrapper extends Runnable {

    /**
     * Causes this thread to begin execution.
     * Delegates to the underlying Thread.start() method.
     */
    void start();

    /**
     * Waits for this thread to die.
     *
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    void join() throws InterruptedException;

    /**
     * Waits at most {@code millis} milliseconds for this thread to die.
     *
     * @param millis the time to wait in milliseconds
     * @throws InterruptedException if any thread has interrupted the current thread
     */
    void join(long millis) throws InterruptedException;

    /**
     * Interrupts this thread.
     */
    void interrupt();

    /**
     * Tests whether this thread is alive.
     *
     * @return true if this thread is alive; false otherwise
     */
    boolean isAlive();

    /**
     * Returns this thread's name.
     *
     * @return this thread's name
     */
    String getName();

    /**
     * Marks this thread as either a daemon thread or a user thread.
     *
     * @param on if true, marks this thread as a daemon thread
     */
    void setDaemon(boolean on);

    /**
     * Changes the name of this thread to be equal to the argument name.
     *
     * @param name the new name for this thread
     */
    void setName(String name);

    /**
     * Tests whether this thread has been interrupted.
     *
     * @return true if this thread has been interrupted; false otherwise
     */
    boolean isInterrupted();
}
