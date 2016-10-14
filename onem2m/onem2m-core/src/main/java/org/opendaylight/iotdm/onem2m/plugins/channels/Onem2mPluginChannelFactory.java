/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels;

import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginConfigurationBuilder;
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;

import javax.annotation.Nonnull;

/**
 * Describes implementation of factory for instantiation of CommunicationChannels.
 * Provides constant information about concrete CommunicationChannel implementation used
 * by PluginManager e.g.:
 *  - protocol name
 *  - channel type
 *  - used transport protocol type
 * @param <Tcb> Type of the configuration builder used to builde configuration.
 */
public abstract class Onem2mPluginChannelFactory<Tcb extends IotdmPluginConfigurationBuilder> {
    protected final String protocol;
    protected final Onem2mBaseCommunicationChannel.CommunicationChannelType channelType;
    protected final Onem2mBaseCommunicationChannel.TransportProtocol transportProtocol;

    public Onem2mPluginChannelFactory(
                                @Nonnull final String protocol,
                                @Nonnull final Onem2mBaseCommunicationChannel.CommunicationChannelType channelType,
                                @Nonnull final Onem2mBaseCommunicationChannel.TransportProtocol transportProtocol) {
        this.protocol = protocol;
        this.channelType = channelType;
        this.transportProtocol = transportProtocol;
    }

    /**
     * Creates new instance of CommunicationChannel.
     * @param ipAddress IP address of the local interface to be used for SERVER channel type or
     *                  remote interface for CLIENT channel type.
     *                  All interfaces are used for SERVER if 0.0.0.0 is passed.
     * @param port Port number to be used. Local port for SERVER, remote for CLIENT.
     * @param configBuilder Configuration builder with pre-set values if needed. Build method of the
     *                      builder will be called and new configuration will be used to configure new
     *                      instance of CommunicationChannel.
     * @param registry Registry of local endpoints and registered plugins maintained by PluginManager.
     *                 Is used by CommunicationChannel to find the right plugin for handling of the request.
     * @return The new instance of the CommunicationChannel.
     */
    public abstract Onem2mBaseCommunicationChannel createInstance(String ipAddress, int port,
                                                                  Tcb configBuilder,
                                                                  Onem2mLocalEndpointRegistry registry);

    /**
     * Returns the name of implemented protocol.
     * @return Protocol name.
     */
    public String getProtocol() {
        return this.protocol;
    }

    /**
     * Returns type of channel.
     * @return Type of channel.
     */
    public Onem2mBaseCommunicationChannel.CommunicationChannelType getChannelType() {
        return this.channelType;
    }

    /**
     * Returns used type of transport protocol.
     * @return Transport protocol type.
     */
    public Onem2mBaseCommunicationChannel.TransportProtocol getTransportProtocol() {
        return this.transportProtocol;
    }
}
