/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.annotation.Nonnull;

public class Onem2mHttpsPluginServer extends Onem2mHTTPProvider {

    private final HttpsServerConfiguration config;

    public static class HttpsServerConfiguration {
        private final String keyStoreFile;
        private final String keyStorePassword;
        private final String keyManagerPassword;

        public HttpsServerConfiguration(@Nonnull final String keyStoreFile,
                                        @Nonnull final String keyStorePassword) {
            this.keyStoreFile = keyStoreFile;
            this.keyStorePassword = keyStorePassword;
            this.keyManagerPassword = keyStorePassword;
        }

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

    public Onem2mHttpsPluginServer (@Nonnull final HttpsServerConfiguration configuration) {
        super();
        this.config = configuration;
    }

    @Override
    public int init(int port, Onem2mPluginManager.Mode mode) {
        this.__port = port;
        this.instanceMode = mode;
        httpServer = new Server();

        // Prepare the httpServer instance
        this.prepareServer();

        // Configure SSL Context Factory
        SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();
        ssl_connector.setPort(port);
        SslContextFactory cf = ssl_connector.getSslContextFactory();
        cf.setKeyStorePath(this.config.getKeyStoreFile());
        cf.setKeyStorePassword(this.config.getKeyStorePassword());
        cf.setTrustAll(true);
        cf.setKeyManagerPassword(this.config.getKeyManagerPassword());
        httpServer.setConnectors(new Connector[]{ ssl_connector });

        // Start the prepared server
        this.startServer();

        return 0;
    }

    @Override
    public String getProtocol() {
        return "https";
    }
}
