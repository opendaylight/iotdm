/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.registry;

import org.opendaylight.iotdm.onem2m.plugins.IotdmPlugin;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.Onem2mBaseCommunicationChannel;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Implementation of abstract registry which registers plugins for
 * specified URI or all URIs. Every instance of CommunicationChannel
 * needs to have one instance of EndpointRegistry where are stored registered
 * plugins associated with registered URIs.
 *
 * Class implements some common methods required by PluginManager.
 */
public abstract class Onem2mLocalEndpointRegistry {
    private final Onem2mPluginManager.ChannelIdentifier channelId;
    private AtomicReference<Onem2mBaseCommunicationChannel> associatedChannel = new AtomicReference<>(null);

    /**
     * Constructor sets channelId which describes CommunicationChannel to which the
     * instance of registry belongs.
     * @param channelId The channelId including CommunicationChannel parameters
     */
    public Onem2mLocalEndpointRegistry(@Nonnull final Onem2mPluginManager.ChannelIdentifier channelId) {
        this.channelId = channelId;
    }

    /**
     * Method sets the associated CommunicationChannel which
     * uses this instance of the registry.
     * @param channel The instance of associated CommunicationChannel
     */
    public void setAssociatedChannel(@Nonnull final Onem2mBaseCommunicationChannel channel) {
        if (null != this.associatedChannel.get()) {
            throw new RuntimeException("Attempt to associate multiple channels with one endpoint registry");
        }
        this.associatedChannel.set(channel);
    }

    public void unsetAssociatedChannel() {
        this.associatedChannel.set(null);
    }

    // Getter methods
    public String getProtocol() { return this.channelId.getProtocolName(); }
    public String getIpAddress() { return this.channelId.getIpAddress(); }
    public int getPort() { return this.channelId.getPort(); }
    public Onem2mBaseCommunicationChannel getAssociatedChannel() { return this.associatedChannel.get(); }
    public Onem2mPluginManager.Mode getMode() { return this.channelId.getMode(); }
    public Onem2mPluginManager.ChannelIdentifier getChannelId() { return this.channelId; }

    /* Abstract methods to be implemented */

    /**
     * Registers plugin for specific URI if not already used by another
     * plugin instance.
     * @param plugin Plugin instance
     * @param onem2mUri Local URI for which the plugin will be registered
     * @return True in case of success, False otherwise
     */
    public abstract boolean regPlugin(IotdmPlugin plugin, String onem2mUri);

    /**
     * Gets plugin registered for the given URI.
     * @param onem2mUri Registered URI
     * @return Registered plugin if exists
     */
    public abstract IotdmPlugin getPlugin(String onem2mUri);

    /**
     * Gets all registered plugins in stream.
     * @return Stream of LocalURI to Plugin mappings
     */
    public abstract Stream<Map.Entry<String, IotdmPlugin>> getPluginStream();

    /**
     * Method checks whether the given plugin is registered in this repository.
     * @param plugin Plugin instance to be verified
     * @return True if the plugin is registered, False otherwise
     */
    public abstract boolean hasPlugin(IotdmPlugin plugin);

    /**
     * Methods checks whether the given plugin is registered for specific URI.
     * @param plugin Plugin instance to be verified
     * @param onem2mUri The URI for which the plugin should be registered
     * @return True if the plugin is registered for the specified URI, False otherwise
     */
    public abstract boolean hasPlugin(IotdmPlugin plugin, String onem2mUri);

    /**
     * Removes all registrations of the given plugin instance (all URIs).
     * @param plugin Plugin to be removed
     * @return True if the plugin has been removed, False otherwise
     */
    public abstract boolean removePlugin(IotdmPlugin plugin);

    /**
     * Removes plugin registration for only one specific URI.
     * @param plugin Plugin to be removed
     * @param onem2mUri The registered URI
     * @return True if the plugin has been removed, False otherwise
     */
    public abstract boolean removePlugin(IotdmPlugin plugin, String onem2mUri);

    /**
     * Checks if the registry has registered some plugin.
     * @return True if at lease one plugin has been registered, False otherwise
     */
    public abstract boolean isEmpty();
}
