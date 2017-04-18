/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx;

import java.io.FileInputStream;
import java.security.KeyStore;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.DefaultHttpsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Common HTTP and HTTPS client base class for HTTP(s) notifier and routing plugins.
 */
public abstract class Onem2mHttpClient implements Onem2mProtocolTxChannel {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpClient.class);
    private final Onem2mHttpClientConfiguration configuration;

    protected HttpClient client = null;
    protected String pluginName = "http"; // http by default

    protected KeyStore truststore = null;
    protected FileInputStream truststoreStream = null;

    public Onem2mHttpClient(Onem2mHttpClientConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws RuntimeException {

        SslContextFactory sslContextFactory = null;

        try {
            if (null != configuration &&
                configuration.isSecureConnection() &&
                null != configuration.getSecureConnectionConfig() &&
                null != configuration.getSecureConnectionConfig().getTrustStoreConfig()) {

                sslContextFactory = new SslContextFactory(false);
                truststore = KeyStore.getInstance("JKS");
                try {
                    truststoreStream = new FileInputStream(
                        configuration.getSecureConnectionConfig()
                                     .getTrustStoreConfig()
                                     .getTrustStoreFile());
                    truststore.load(truststoreStream,
                                    configuration.getSecureConnectionConfig().getTrustStoreConfig().
                                        getTrustStorePassword().toCharArray());
                } finally {
                    if (truststoreStream != null) {
                        truststoreStream.close();
                        truststoreStream = null;
                    }
                }

                sslContextFactory.setTrustStore(truststore);
                sslContextFactory.setTrustStorePassword(
                        configuration.getSecureConnectionConfig().getTrustStoreConfig().getTrustStorePassword());

                // TODO: Jetty 8 requires also keystore to be set, can be removed for upper versions
                if (null != configuration.getSecureConnectionConfig().getKeyStoreConfig()) {
                    sslContextFactory.setKeyStorePath(
                            configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyStoreFile());
                    sslContextFactory.setKeyStorePassword(
                            configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyStorePassword());
                    if (null != configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyManagerPassword()) {
                        sslContextFactory.setKeyManagerPassword(
                                configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyManagerPassword());
                    } else {
                        sslContextFactory.setKeyManagerPassword(
                                configuration.getSecureConnectionConfig().getKeyStoreConfig().getKeyStorePassword());
                    }
                } else if (null != Onem2mCoreProvider.getInstance().getDefaultHttpsConfig()) {
                    DefaultHttpsConfig keyStore = Onem2mCoreProvider.getInstance().getDefaultHttpsConfig();
                    sslContextFactory.setKeyStorePath(keyStore.getKeyStoreFile());
                    sslContextFactory.setKeyStorePassword(keyStore.getKeyStorePassword());
                    if (null != keyStore.getKeyManagerPassword()) {
                        sslContextFactory.setKeyManagerPassword(keyStore.getKeyManagerPassword());
                    } else {
                        sslContextFactory.setKeyManagerPassword(keyStore.getKeyStorePassword());
                    }
                } else {
                    LOG.error("Keystore not configured, required by jetty 8");
                }

                client = new HttpClient(sslContextFactory);
                this.pluginName = "https";
                LOG.info("Starting HTTPS client.");
            } else {
                client = new HttpClient();
                LOG.info("Starting HTTP client.");
            }

            client.start();
        } catch (Exception e) {
            LOG.error("Failed to start client:: {}", e);
            truststore = null;
        }
    }

    @Override
    public void close() {
        try {
            client.stop();
        } catch (Exception e) {
            LOG.error("Failed to close client: {}", e);
        }
        truststore = null;
    }

    /**
     * Sends request passed in the content exchange.
     * @param ex The content exchange including all data to be sent.
     * @throws IOException
     */
    public void send(ContentExchange ex) throws IOException {
        this.client.send(ex);
    }
}
