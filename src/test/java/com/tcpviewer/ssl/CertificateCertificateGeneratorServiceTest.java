package com.tcpviewer.ssl;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CertificateCertificateGeneratorServiceTest {

    @BeforeAll
    static void setupProvider() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void generatesValidServerCertificateSignedByCA() throws Exception {


        ServerCertificateGeneratorService service =
                new ServerCertificateGeneratorService();

        String serverName = "test.example.com";

        // Act
        ServerCertificateGeneratorService.GeneratedCertificate result =
                service.generateServerCertificate(serverName, 365);

        X509Certificate serverCert = result.certificate();
        PrivateKey serverKey = result.privateKey();
        KeyStore keyStore = result.keyStore();

        // Assert: basic properties
        assertNotNull(serverCert);
        assertNotNull(serverKey);
        assertNotNull(keyStore);

        //Verify KeyStore
        Certificate cert = keyStore.getCertificate("server");
        Key key = keyStore.getKey("server", "changeit".toCharArray());
        assertNotNull(cert);
        assertNotNull(key);

        assertEquals(serverCert.getIssuerX500Principal().getName(), service.getCaCertificate().getSubjectX500Principal().getName());

        assertTrue(
                serverCert.getSubjectX500Principal().getName().contains("CN=" + serverName)
        );

        // Assert: certificate verifies with CA public key
        serverCert.verify(service.getCaCertificate().getPublicKey());

        // Assert: SAN contains DNS name
        var sans = serverCert.getSubjectAlternativeNames();
        assertNotNull(sans);
        assertTrue(
                sans.stream().anyMatch(
                        san -> san.get(0).equals(GeneralName.dNSName)
                                && san.get(1).equals(serverName)
                )
        );

        // Assert: key usage
        boolean[] keyUsage = serverCert.getKeyUsage();
        assertNotNull(keyUsage);
        assertTrue(keyUsage[0]); //digital signature
        assertTrue(keyUsage[2]);

        // Assert: extended key usage includes serverAuth
        var eku = serverCert.getExtendedKeyUsage();
        assertNotNull(eku);
        assertTrue(eku.contains(KeyPurposeId.id_kp_serverAuth.getId()));
    }

}

