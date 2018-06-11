package com.github.connteam.conn.core.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public final class SSLUtil {
    private SSLUtil() {
    }

    public static void setKeyStore(InputStream input, String password) throws KeyStoreException {
        try {
            char[] pass = (password != null ? password.toCharArray() : null);

            KeyStore store = KeyStore.getInstance("JKS");
            store.load(input, pass);

            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(store, pass);

            TrustManagerFactory trusted = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trusted.init(store);

            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(keys.getKeyManagers(), trusted.getTrustManagers(), null);
            SSLContext.setDefault(ctx);
        } catch (KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException
                | IOException e) {
            throw new KeyStoreException(e);
        }
    }
}
