package com.tcpviewer.lang.wrapper.impl;

import com.tcpviewer.lang.wrapper.SystemWrapper;
import org.springframework.stereotype.Component;

/**
 * Default implementation of SystemWrapper that delegates to System class.
 */
@Component
public class SystemWrapperImpl implements SystemWrapper {

    @Override
    public void exit(int status) {
        System.exit(status);
    }
}
