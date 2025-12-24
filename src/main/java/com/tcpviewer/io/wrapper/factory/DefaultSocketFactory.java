package com.tcpviewer.io.wrapper.factory;

import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.io.wrapper.impl.DefaultSocketWrapper;

import java.io.IOException;
import java.net.Socket;

/**
 * Default implementation of SocketFactory.
 * Creates new sockets and wraps them with DefaultSocketWrapper.
 */
public class DefaultSocketFactory implements SocketFactory {

    @Override
    public SocketWrapper createSocket(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        return new DefaultSocketWrapper(socket);
    }

    @Override
    public SocketWrapper wrapSocket(Socket socket) {
        return new DefaultSocketWrapper(socket);
    }
}
