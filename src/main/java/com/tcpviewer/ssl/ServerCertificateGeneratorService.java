package com.tcpviewer.ssl;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.*;
import org.bouncycastle.operator.jcajce.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;

@Service
public class ServerCertificateGeneratorService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final X509Certificate caCertificate;
    private final PrivateKey caPrivateKey;

    public ServerCertificateGeneratorService() {
        try {
            this.caCertificate = loadCertificate();
            this.caPrivateKey = loadPrivateKey();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to initialize CA material", e);
        }
    }

    public GeneratedCertificate generateServerCertificate(String serverName, int validityDays)
            throws Exception {

        // Generate server keypair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair serverKeyPair = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=" + serverName);
        X500Name issuer = X500Name.getInstance(caCertificate.getSubjectX500Principal().getEncoded());

        Instant now = Instant.now();
        Date notBefore = Date.from(now.minusSeconds(60));
        Date notAfter = Date.from(now.plusSeconds(validityDays * 86400L));

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        JcaX509v3CertificateBuilder certBuilder =
                new JcaX509v3CertificateBuilder(
                        issuer,
                        serial,
                        notBefore,
                        notAfter,
                        subject,
                        serverKeyPair.getPublic()
                );

        // Extensions
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(false)
        );

        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        );

        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
        );

        certBuilder.addExtension(
                Extension.subjectAlternativeName,
                false,
                new GeneralNames(
                        new GeneralName(GeneralName.dNSName, serverName)
                )
        );

        certBuilder.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(caCertificate.getPublicKey())
        );

        certBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(serverKeyPair.getPublic())
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(caPrivateKey);

        X509CertificateHolder holder = certBuilder.build(signer);
        X509Certificate serverCert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);

        serverCert.verify(caCertificate.getPublicKey());

        // 1. Create an in-memory KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        // 2. Store private key and certificate
        char[] keyPassword = "changeit".toCharArray(); // required, even for in-memory keystore
        Certificate[] chain = new Certificate[]{serverCert};
        keyStore.setKeyEntry("server", serverKeyPair.getPrivate(), keyPassword, chain);


        return new GeneratedCertificate(serverCert, serverKeyPair.getPrivate(), keyStore);
    }

    public X509Certificate getCaCertificate() {
        return caCertificate;
    }

    private static X509Certificate loadCertificate() throws Exception {
        byte[] bytes = loadFromClasspath("ssl/cacert.pem");
        try (PEMParser parser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            X509CertificateHolder holder = (X509CertificateHolder) parser.readObject();
            return new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(holder);
        }
    }

    private static PrivateKey loadPrivateKey() throws Exception {
        byte[] bytes = loadFromClasspath("ssl/cakey.pem");
        try (PEMParser parser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (obj instanceof org.bouncycastle.openssl.PEMKeyPair) {
                return converter.getKeyPair((org.bouncycastle.openssl.PEMKeyPair) obj).getPrivate();
            }
            return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) obj);
        }
    }

    private static byte[] loadFromClasspath(String resourcePath) throws IOException {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (is == null) {
                throw new IOException("Resource not found on classpath: " + resourcePath);
            }
            return is.readAllBytes();
        }
    }


    public record GeneratedCertificate(
            X509Certificate certificate,
            PrivateKey privateKey,
            KeyStore keyStore
    ) {}
}
