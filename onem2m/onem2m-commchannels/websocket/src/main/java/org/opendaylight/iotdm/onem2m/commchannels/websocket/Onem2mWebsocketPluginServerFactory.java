/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.websocket;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mPluginChannelFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurationBuilder;

/**
 * Factory class implementing instantiation of Websocket HTTP servers.
 */
public class Onem2mWebsocketPluginServerFactory extends Onem2mPluginChannelFactory {

    public Onem2mWebsocketPluginServerFactory() {
        super(IotdmWebsocketPluginServerRx.ProtocolWebsocket, IotdmBaseRxCommunicationChannel.CommunicationChannelType.SERVER, IotdmBaseRxCommunicationChannel.TransportProtocol.TCP);
    }


    public IotdmBaseRxCommunicationChannel createInstance(String ipAddress, int port, IotdmPluginConfigurationBuilder configBuilder,
                                                          IotdmRxPluginsRegistryReadOnly registry) {
        if (null != configBuilder) {
            // configuration must be null
            return null;
        }

        IotdmWebsocketPluginServerRx server = new IotdmWebsocketPluginServerRx(ipAddress, port, registry);
        if (server.init()) {
            // return the new instance of the Websocket HTTP server
            return server;
        }

        return null;
    }
}
