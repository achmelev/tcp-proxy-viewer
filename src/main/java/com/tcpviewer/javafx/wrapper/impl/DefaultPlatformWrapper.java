package com.tcpviewer.javafx.wrapper.impl;

import com.tcpviewer.javafx.wrapper.PlatformWrapper;
import javafx.application.Platform;

/**
 * Default implementation of PlatformWrapper that delegates to javafx.application.Platform.
 * Provides zero-overhead delegation for production use.
 */
public class DefaultPlatformWrapper implements PlatformWrapper {

    @Override
    public void runLater(Runnable runnable) {
        Platform.runLater(runnable);
    }
}
