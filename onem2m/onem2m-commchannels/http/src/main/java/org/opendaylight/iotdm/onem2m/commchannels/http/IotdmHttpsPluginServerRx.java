/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http;

import javax.annotation.Nonnull;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mKeyStoreFileConfig;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.commchannels.http.rev170519.iotdm.plugin.manager.communication.channels.output.iotdm.communication.channel.protocols.iotdm.communication.channel.addresses.iotdm.communication.channel.ports.channel.data.channel.configuration.channel.specific.configuration.Onem2mHttpsChannelConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.channel.data.ChannelConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of HTTPS server. Uses the same parent class as the HTTP server implementation,
 * but configuration is mandatory.
 */
public class IotdmHttpsPluginServerRx
    extends IotdmHttpBaseRxChannel<IotdmHttpsPluginServerRx.HttpsServerConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmHttpsPluginServerRx.class);
    public static final String ProtocolHTTPS = "https";
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

    public IotdmHttpsPluginServerRx(String ipAddress, int port,
                                    IotdmRxPluginsRegistryReadOnly registry,
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
        ssl_connector.setPort(port);
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
        Onem2mHttpsChannelConfigBuilder builder = new Onem2mHttpsChannelConfigBuilder()
                .setKeyManagerPassword(this.configuration.getKeyManagerPassword()) // TODO exposure
                .setKeyStoreFile(this.configuration.getKeyStoreFile())
                .setKeyManagerPassword(this.configuration.getKeyManagerPassword()); // TODO exposure

        return this.getChannelConfigBuilder().setChannelSpecificConfiguration(builder.build()).build();
    }
}
