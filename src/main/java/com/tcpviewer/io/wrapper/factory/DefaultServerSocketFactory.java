package com.tcpviewer.io.wrapper.factory;

import com.tcpviewer.io.wrapper.ServerSocketWrapper;
import com.tcpviewer.io.wrapper.impl.DefaultServerSocketWrapper;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

/**
 * Default implementation of ServerSocketFactory.
 * Creates DefaultServerSocketWrapper instances that delegate to java.net.ServerSocket.
 */
public class DefaultServerSocketFactory implements ServerSocketFactory {

    private final SocketFactory socketFactory;

    /**
     * Creates a new DefaultServerSocketFactory.
     *
     * @param socketFactory factory for wrapping accepted sockets
     */
    public DefaultServerSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    @Override
    public ServerSocketWrapper createServerSocket(KeyStore keyStore) throws IOException {
        if (keyStore == null) {
            return new DefaultServerSocketWrapper(new ServerSocket(), socketFactory);
        } else  {
            try {
                // 3. Initialize KeyManagerFactory with the KeyStore
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm()
                );
                kmf.init(keyStore, "changeit".toCharArray());
                // 4. Initialize SSLContext
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), null, null);

                // 5. Create SSLServerSocket
                SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
                return new DefaultServerSocketWrapper(sslServerSocketFactory.createServerSocket(), socketFactory);
            } catch (Exception e) {
                throw  new RuntimeException("Error creating SSL Server Socker");
            }
        }
    }
}
