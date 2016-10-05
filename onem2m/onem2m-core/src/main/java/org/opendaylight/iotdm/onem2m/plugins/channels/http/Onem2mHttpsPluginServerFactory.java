/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.http;

import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mPluginChannelFactory;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;

import static org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel.CommunicationChannelType.SERVER;
import static org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel.TransportProtocol.TCP;

public class Onem2mHttpsPluginServerFactory
        extends Onem2mPluginChannelFactory<Onem2mHttpsPluginServer.HttpsServerConfiguration> {

    public Onem2mHttpsPluginServerFactory() {
        super(Onem2mPluginManager.ProtocolHTTPS, SERVER, TCP);
    }


    public Onem2mBaseCommunicationChannel createInstance(String ipAddress, int port, Object config,
                                                         Onem2mLocalEndpointRegistry registry) {
        if (! (config instanceof Onem2mHttpsPluginServer.HttpsServerConfiguration)) {
            return null;
        }

        Onem2mHttpsPluginServer server =
                new Onem2mHttpsPluginServer(ipAddress, port, registry,
                                            (Onem2mHttpsPluginServer.HttpsServerConfiguration) config);
        if(server.init()) {
            return server;
        }

        return null;
    }
}