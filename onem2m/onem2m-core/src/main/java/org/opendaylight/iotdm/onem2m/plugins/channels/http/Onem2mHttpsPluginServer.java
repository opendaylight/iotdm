/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.http;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Implementation of HTTPS server. Uses the same parent class as the HTTP server implementation,
 * but configuration is mandatory.
 */
public class Onem2mHttpsPluginServer extends Onem2mHttpBaseChannel<Onem2mHttpsPluginServer.HttpsServerConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpsPluginServer.class);

    /**
     * Class stores HTTPS server configuration.
     */
    public static class HttpsServerConfiguration {
        private final String keyStoreFile;
        private final String keyStorePassword;
        private final String keyManagerPassword;

        /**
         * Constructor creates instance with the same password usded
         * for keyStore and keyManager.
         * @param keyStoreFile Path to the keyStore file
         * @param keyStorePassword Password common for keyStore and keyManager
         */
        public HttpsServerConfiguration(@Nonnull final String keyStoreFile,
                                        @Nonnull final String keyStorePassword) {
            this.keyStoreFile = keyStoreFile;
            this.keyStorePassword = keyStorePassword;
            this.keyManagerPassword = keyStorePassword;
        }

        /**
         * Constructor creates instance with all configuration data set.
         * @param keyStoreFile Path to the keyStore file
         * @param keyStorePassword Password to the keyStore
         * @param keyManagerPassword Password for keyManager
         */
        public HttpsServerConfiguration(@Nonnull final String keyStoreFile,
                                        @Nonnull final String keyStorePassword,
                                        @Nonnull final String keyManagerPassword) {
            this.keyStoreFile = keyStoreFile;
            this.keyStorePassword = keyStorePassword;
            this.keyManagerPassword = keyManagerPassword;
        }

        public String getKeyStoreFile() {
            return keyStoreFile;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public String getKeyManagerPassword() {
            return keyManagerPassword;
        }
    }

    public Onem2mHttpsPluginServer (String ipAddress, int port,
                                    Onem2mLocalEndpointRegistry registry,
                                    @Nonnull final HttpsServerConfiguration configuration) {
        super(ipAddress, port, registry, configuration);
    }

    @Override
    public boolean validateConfig(HttpsServerConfiguration configuration) {
        if (null == configuration.getKeyStoreFile() || configuration.getKeyStoreFile().isEmpty()) {
            LOG.error("Invalid configuration: no KeyStore file configured");
            return false;
        }

        if (null == configuration.getKeyStorePassword() || configuration.getKeyStorePassword().isEmpty()) {
            LOG.error("Invalid configuration: no KeyStore password configured");
            return false;
        }

        if (null == configuration.getKeyManagerPassword() || configuration.getKeyManagerPassword().isEmpty()) {
            LOG.error("Invalid configuration: no KeyManager password configured");
            return false;
        }

        return true;
    }

    @Override
    public boolean compareConfig(HttpsServerConfiguration configuration) {
        if (this.configuration == null) {
            return false;
        }

        if ((! this.configuration.getKeyStoreFile().equals(configuration.getKeyStoreFile())) ||
            (! this.configuration.getKeyStorePassword().equals(configuration.getKeyStorePassword())) ||
            (! this.configuration.getKeyManagerPassword().equals(configuration.getKeyManagerPassword()))) {
            return false;
        }

        return true;
    }

    @Override
    public boolean init() {
        httpServer = new Server();

        // Prepare the httpServer instance
        this.prepareServer();

        // Configure SSL Context Factory
        SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();
        ssl_connector.setPort(port);
        SslContextFactory cf = ssl_connector.getSslContextFactory();
        cf.setKeyStorePath(this.configuration.getKeyStoreFile());
        cf.setKeyStorePassword(this.configuration.getKeyStorePassword());
        cf.setTrustAll(true);
        cf.setKeyManagerPassword(this.configuration.getKeyManagerPassword());
        httpServer.setConnectors(new Connector[]{ ssl_connector });

        // Start the prepared server
        this.startServer();

        return true;
    }

    @Override
    public String getProtocol() {
        return "https";
    }
}
