/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.coap;

import java.util.Objects;
import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mPluginChannelFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.commchannels.coap.rev170519.coaps.psk.config.CsePsk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.onem2m.core.coaps.config.DefaultCoapsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mCoapsPluginServerFactory extends Onem2mPluginChannelFactory<IotdmCoapsConfigBuilder> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapsPluginServerFactory.class);

    public Onem2mCoapsPluginServerFactory() {
        super(IotdmCoapsPluginServerRx.ProtocolCoAPS, IotdmBaseRxCommunicationChannel.CommunicationChannelType.SERVER, IotdmBaseRxCommunicationChannel.TransportProtocol.UDP);
    }

    @Override
    public IotdmBaseRxCommunicationChannel createInstance(String ipAddress, int port,
                                                          IotdmCoapsConfigBuilder configBuilder,
                                                          IotdmRxPluginsRegistryReadOnly registry) {
        if (Objects.isNull(configBuilder)) {
            // configuration must be passed
            return null;
        }

        try {
            configBuilder.verify();
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid configuration passed: {}", e);
            return null;
        }

        if (configBuilder.getUsesDefaultConfig()) {
            // Get default configuration from Onem2mCore
            DefaultCoapsConfig defCfg = Onem2mCoreProvider.getInstance().getDefaultCoapsConfig();
            if (null == defCfg) {
                LOG.warn("Default configuration doesn't exist");
                configBuilder.setDefaultConfigAvailable(false);
            } else {
                if (configBuilder.getUsesPsk()) {
                    if (null == defCfg.getCsePsk() || defCfg.getCsePsk().isEmpty()) {
                        LOG.warn("Default configuration of PSK doesn't exist");
                        configBuilder.setDefaultConfigAvailable(false);
                    }

                    for (CsePsk item : defCfg.getCsePsk()) {
                        configBuilder.addPsk(item.getCseId(), item.getPsk());
                    }

                    configBuilder.setDefaultConfigAvailable(true);
                } else {
                    if (null == defCfg.getKeyStoreFile() || null == defCfg.getKeyStorePassword() ||
                        null == defCfg.getKeyAlias()) {
                        LOG.warn("Default configuration of KeyStore doesn't exist");
                        configBuilder.setDefaultConfigAvailable(false);
                    }

                    configBuilder
                        .setKeyStoreFile(defCfg.getKeyStoreFile())
                        .setKeyStorePassword(defCfg.getKeyStorePassword())
                        .setKeyManagerPassword(defCfg.getKeyManagerPassword())
                        .setKeyAlias(defCfg.getKeyAlias())
                        .setDefaultConfigAvailable(true);
                }
            }
        }

        IotdmCoapsPluginServerRx.CoapsConfig config = null;
        try {
            config = configBuilder.build();
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to build configuration");
        }

        IotdmCoapsPluginServerRx server = new IotdmCoapsPluginServerRx(ipAddress, port, registry,
                                                                       config, configBuilder.getUsesDefaultConfig());
        if (server.init()) {
            // return the new instance of the COAPS server
            return server;
        }
        return null;
    }
}
