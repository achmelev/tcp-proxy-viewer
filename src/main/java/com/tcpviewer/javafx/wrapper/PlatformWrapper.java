package com.tcpviewer.javafx.wrapper;

/**
 * Mockable wrapper interface for javafx.application.Platform.
 * Provides abstraction over Platform for testability without JavaFX runtime.
 * Enables testing of UI synchronization code without actual JavaFX thread.
 */
public interface PlatformWrapper {

    /**
     * Run the specified Runnable on the JavaFX Application Thread at some unspecified time in the future.
     * This method can be called whether or not the application has been launched.
     *
     * @param runnable the Runnable whose run method will be executed on the JavaFX Application Thread
     */
    void runLater(Runnable runnable);
}
