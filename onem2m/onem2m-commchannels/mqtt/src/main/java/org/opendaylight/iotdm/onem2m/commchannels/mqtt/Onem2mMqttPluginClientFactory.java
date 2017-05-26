/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.mqtt;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmBaseRxCommunicationChannel;
import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mPluginChannelFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurationBuilder;

/**
 * Factory class implementing instantiation of MQTT clients.
 */
public class Onem2mMqttPluginClientFactory extends Onem2mPluginChannelFactory {

    public Onem2mMqttPluginClientFactory() {
        super(IotdmMqttPluginClientRx.ProtocolMQTT, IotdmBaseRxCommunicationChannel.CommunicationChannelType.CLIENT, IotdmBaseRxCommunicationChannel.TransportProtocol.TCP);
    }


    public IotdmBaseRxCommunicationChannel createInstance(String ipAddress, int port, IotdmPluginConfigurationBuilder configBuilder,
                                                          IotdmRxPluginsRegistryReadOnly registry) {
        if (null != configBuilder) {
            // configuration must be null
            return null;
        }

        IotdmMqttPluginClientRx client = new IotdmMqttPluginClientRx(ipAddress, port, registry);
        if (client.init()) {
            // return the new instance of the Mqtt client
            return client;
        }

        return null;
    }
}
