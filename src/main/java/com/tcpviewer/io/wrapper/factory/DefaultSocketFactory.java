package com.tcpviewer.io.wrapper.factory;

import com.tcpviewer.io.wrapper.SocketWrapper;
import com.tcpviewer.io.wrapper.impl.DefaultSocketWrapper;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * Default implementation of SocketFactory.
 * Creates new sockets and wraps them with DefaultSocketWrapper.
 */
public class DefaultSocketFactory implements SocketFactory {

    private SSLSocketFactory socketFactory;

    public  DefaultSocketFactory() {
        // Trust manager that trusts everything
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Initialize SSL context
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            socketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            throw  new RuntimeException("Error creating SSL Socket factory");
        }
    }

    @Override
    public SocketWrapper createSocket(String host, int port, boolean ssl, String sniHostName) throws IOException {
        if  (ssl) {
            SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, port);

            // --- Enable SNI ---
            SSLParameters sslParameters = socket.getSSLParameters();
            sslParameters.setServerNames(
                    Collections.singletonList(new SNIHostName(sniHostName))
            );
            socket.setSSLParameters(sslParameters);
            // Start handshake explicitly
            socket.startHandshake();
            return new DefaultSocketWrapper(socket);
        } else {
            Socket socket = new Socket(host, port);
            return new DefaultSocketWrapper(socket);
        }

    }

    @Override
    public SocketWrapper wrapSocket(Socket socket) {
        return new DefaultSocketWrapper(socket);
    }
}
