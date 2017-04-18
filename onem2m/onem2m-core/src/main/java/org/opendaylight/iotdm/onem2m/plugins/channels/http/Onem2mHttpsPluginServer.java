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
import org.opendaylight.iotdm.onem2m.plugins.channels.common.Onem2mKeyStoreFileConfig;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.communication.channel.data.definition.channel.data.ChannelConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.communication.channel.data.definition.channel.data.channel.configuration.channel.specific.configuration.HttpsChannelConfigBuilder;
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
    public static class HttpsServerConfiguration extends Onem2mKeyStoreFileConfig {
        protected boolean compareConfig(HttpsServerConfiguration configuration) {
            if (configuration == null) {
                return false;
            }

            return super.compareConfig(configuration);
        }
    }

    public Onem2mHttpsPluginServer(String ipAddress, int port,
                                   Onem2mLocalEndpointRegistry registry,
                                   @Nonnull final HttpsServerConfiguration configuration,
                                   boolean usesDefaultCfg) {
        super(ipAddress, port, registry, configuration, usesDefaultCfg);
    }

    @Override
    public boolean compareConfig(HttpsServerConfiguration configuration) {
        return this.configuration.compareConfig(configuration);
    }

    @Override
    public boolean init() {
        httpServer = new Server(port);

        // Prepare the httpServer instance
        this.prepareServer();

        // Configure SSL Context Factory
        SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();
//        ssl_connector.setPort(port);
        SslContextFactory cf = ssl_connector.getSslContextFactory();

        cf.setKeyStorePath(this.configuration.getKeyStoreFile());
        cf.setKeyStorePassword(this.configuration.getKeyStorePassword());
        cf.setTrustAll(true);
        cf.setKeyManagerPassword(this.configuration.getKeyManagerPassword());
        httpServer.setConnectors(new Connector[]{ ssl_connector });

        // Start the prepared server
        if (! this.startServer()) {
            this.setState(ChannelState.INITFAILED);
        } else {
            if (this.configuration.getUsesDefaultConfig()) {
                this.setState(ChannelState.RUNNINGDEFAULT);
            } else {
                this.setState(ChannelState.RUNNING);
            }
        }
        return true;
    }

    @Override
    public String getProtocol() {
        return "https";
    }

    @Override
    public ChannelConfiguration getChannelConfig() {
        HttpsChannelConfigBuilder builder = new HttpsChannelConfigBuilder()
                .setKeyManagerPassword(this.configuration.getKeyManagerPassword()) // TODO exposure
                .setKeyStoreFile(this.configuration.getKeyStoreFile())
                .setKeyManagerPassword(this.configuration.getKeyManagerPassword()); // TODO exposure

        return this.getChannelConfigBuilder().setChannelSpecificConfiguration(builder.build()).build();
    }
}
