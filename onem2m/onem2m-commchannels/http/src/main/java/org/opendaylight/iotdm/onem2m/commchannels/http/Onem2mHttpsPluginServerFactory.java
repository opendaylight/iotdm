/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http;

import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mPluginChannelFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.onem2m.core.https.config.DefaultHttpsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpsPluginServerFactory extends Onem2mPluginChannelFactory<IotdmHttpsConfigBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpsPluginServerFactory.class);

    public Onem2mHttpsPluginServerFactory() {
        super(IotdmHttpsPluginServerRx.ProtocolHTTPS, IotdmBaseRxCommunicationChannel.CommunicationChannelType.SERVER, IotdmBaseRxCommunicationChannel.TransportProtocol.TCP);
    }

    public IotdmBaseRxCommunicationChannel createInstance(String ipAddress, int port,
                                                          IotdmHttpsConfigBuilder configBuilder,
                                                          IotdmRxPluginsRegistryReadOnly registry) {
        if (null == configBuilder) {
            LOG.error("No configuration passed");
            return null;
        }

        try {
            configBuilder.verify();
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid configuration passed: {}", e);
            return null;
        }

        if (configBuilder.getUsesDefaultConfig()) {
            // Use default configuration if exists
            DefaultHttpsConfig defCfg = Onem2mCoreProvider.getInstance().getDefaultHttpsConfig();
            if (null == defCfg) {
                LOG.warn("Default configuration of HTTPS not provided");
                configBuilder.setDefaultConfigAvailable(false);
            } else {
                configBuilder
                        .setKeyStoreFile(defCfg.getKeyStoreFile())
                        .setKeyStorePassword(defCfg.getKeyStorePassword())
                        .setKeyManagerPassword(defCfg.getKeyManagerPassword())
                        .setDefaultConfigAvailable(true);
            }
        }

        IotdmHttpsPluginServerRx.HttpsServerConfiguration httpsConfig = null;
        try {
            httpsConfig = configBuilder.build();
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to build HTTPS configuration: {}", e);
            return null;
        }

        IotdmHttpsPluginServerRx server =
                new IotdmHttpsPluginServerRx(ipAddress, port, registry, httpsConfig,
                                             configBuilder.getUsesDefaultConfig());
        if(server.init()) {
            return server;
        }

        return null;
    }
}