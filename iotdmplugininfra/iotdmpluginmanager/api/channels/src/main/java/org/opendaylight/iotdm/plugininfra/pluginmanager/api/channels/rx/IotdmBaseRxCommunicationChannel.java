/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.channel.data.ChannelConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.channel.data.ChannelConfigurationBuilder;

/**
 * Describes implementation of CommunicationChannels in general.
 * Implements some methods used by PluginManager in order to maintain running
 * channels.
 * @param <Tconfig> Type of configuration of the channel.
 */
public abstract class IotdmBaseRxCommunicationChannel<Tconfig> implements AutoCloseable {

    protected final String ipAddress;
    protected final int port;
    protected final IotdmRxPluginsRegistryReadOnly pluginRegistry;
    protected final Tconfig configuration;
    private final boolean usesDefaultConfiguration;

    private ReadWriteLock stateLock = new ReentrantReadWriteLock();
    private ChannelState state = ChannelState.INIT;

    public enum ChannelState {
        /**
         * Initial state
         */
        INIT,

        /**
         * Channel is running (without configuration or uses own configuration).
         */
        RUNNING,

        /**
         * Channel is running and uses default configuration.
         */
        RUNNINGDEFAULT,

        /**
         * Channel is not running because it is using default configuration but
         * the configuration is not available now.
         */
        WAITINGDEFAULT,

        /**
         * Channel has never been running since initialization of the channel
         * has failed.
         */
        INITFAILED,

        /**
         * Channel had been running but then has failed.
         */
        FAILED
    }

    // The main types of CommunicationChannels supported
    public enum CommunicationChannelType {
        SERVER, CLIENT
    }

    // Transport protocol used by the specific channel
    public enum TransportProtocol {
        TCP, UDP
    }

    public boolean getUsesDefaultConfiguration() {
        return this.usesDefaultConfiguration;
    }


    public ChannelState getState() {
        stateLock.readLock().lock();
        try {
            return state;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    public boolean isRunning() {
        stateLock.readLock().lock();
        try {
            return ((state == ChannelState.RUNNING) || (state == ChannelState.RUNNINGDEFAULT));
        } finally {
            stateLock.readLock().unlock();
        }
    }

    protected void setState(ChannelState state) {
        stateLock.writeLock().lock();
        try {
            this.state = state;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * The base constructor for CommunicationChannels.
     * @param ipAddress Can be used to identify specific interface or all local interfaces by "0.0.0.0".
     * @param port The port number.
     * @param registry Registry of all plugins which are registered to this channel.
     * @param config Configuration of the channel if required. Null can be passed.
     * @param usesDefaultCfg The configuration used by this channel comes from common default config.
     */
    public IotdmBaseRxCommunicationChannel(String ipAddress, int port,
                                           IotdmRxPluginsRegistryReadOnly registry,
                                           @Nullable Tconfig config, boolean usesDefaultCfg) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.configuration = config;
        this.pluginRegistry = registry;
        this.usesDefaultConfiguration = usesDefaultCfg;
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

    protected ChannelConfigurationBuilder getChannelConfigBuilder() {
        return new ChannelConfigurationBuilder()
                       .setIsDefault(this.usesDefaultConfiguration);
    }

    /**
     * Returns current configuration of the channel (if exists).
     * @return Channel configuration or null if the channel is not configurable.
     */
    public ChannelConfiguration getChannelConfig() {
        return getChannelConfigBuilder().build();
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
