/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels;

import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;

import javax.annotation.Nullable;

/**
 * Describes implementation of CommunicationChannels in general.
 * Implements some methods used by PluginManager in order to maintain running
 * channels.
 * @param <Tconfig> Type of configuration of the channel.
 */
public abstract class Onem2mBaseCommunicationChannel<Tconfig> implements AutoCloseable {

    protected final String ipAddress;
    protected final int port;
    protected final Onem2mLocalEndpointRegistry pluginRegistry;
    protected final Tconfig configuration;

    // The main types of CommunicationChannels supported
    public enum CommunicationChannelType {
        SERVER, CLIENT
    }

    // Transport protocol used by the specific channel
    public enum TransportProtocol {
        TCP, UDP
    }

    /**
     * The base constructor for CommunicationChannels.
     * @param ipAddress Can be used to identify specific interface or all local interfaces by "0.0.0.0".
     * @param port The port number.
     * @param registry Registry of all plugins which are registered to this channel.
     * @param config Configuration of the channel if required. Null can be passed.
     */
    public Onem2mBaseCommunicationChannel(String ipAddress, int port,
                                          Onem2mLocalEndpointRegistry registry, @Nullable Tconfig config) {
        if (! this.validateConfig(config)) {
            throw new IllegalArgumentException("Invalid configuration passed");
        }

        this.ipAddress = ipAddress;
        this.port = port;
        this.configuration = config;
        this.pluginRegistry = registry;
    }

    /**
     * Validates the channel configuration.
     * @param config The configuration of this channel.
     * @return True if valid, False otherwise.
     */
    public boolean validateConfig(Tconfig config) {
        return null == config;
    }

    /**
     * Compares configuration of this channel with the given configuration.
     * This method shall be used in case of registration of multiple plugins
     * when more than one plugin specifies configuration for CommunicationChannel.
     * Configurations must equal in case of shared channel.
     * @param config Configuration to compare.
     * @return True if configuration are the same, False otherwise.
     */
    public boolean compareConfig(Tconfig config) {
        return null == config;
    }

    /**
     * Returns implemented protocol name.
     * @return The protocol name.
     */
    public abstract String getProtocol();

    /**
     * Starts the channel.
     * @return True if passed, False otherwise.
     */
    public abstract boolean init();
}
