/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.coap;

import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mPluginChannelFactory;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;

import java.util.Objects;

import static org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel.CommunicationChannelType.SERVER;
import static org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel.TransportProtocol.UDP;

/**
 * Factory class implementing instantiation of COAP servers.
 */
public class Onem2mCoapPluginServerFactory extends Onem2mPluginChannelFactory {

    public Onem2mCoapPluginServerFactory() {
        super(Onem2mPluginManager.ProtocolCoAP, SERVER, UDP);
    }


    public Onem2mBaseCommunicationChannel createInstance(String ipAddress, int port, Object config,
                                                         Onem2mLocalEndpointRegistry registry) {
        if (Objects.nonNull(config)) {
            // configuration must be null
            return null;
        }

        Onem2mCoapPluginServer server = new Onem2mCoapPluginServer(ipAddress, port, registry);
        if (server.init()) {
            // return the new instance of the COAP server
            return server;
        }
        return null;
    }
}
