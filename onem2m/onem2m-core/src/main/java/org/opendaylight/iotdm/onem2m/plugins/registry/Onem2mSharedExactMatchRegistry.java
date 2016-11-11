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
import org.opendaylight.iotdm.onem2m.plugins.registry.Onem2mLocalEndpointRegistry;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * This registry allows sharing of CommunicationChannel. Plugins are registered for
 * specified URIs and requests are handled by the registered plugins only if the
 * whole URI matches the URI registered by plugin.
 * The same plugin instance can register multiple URIs.
 */
public class Onem2mSharedExactMatchRegistry extends Onem2mLocalEndpointRegistry {
    private final Map<String, IotdmPlugin> pluginMap = new ConcurrentHashMap<>();

    public Onem2mSharedExactMatchRegistry(@Nonnull final Onem2mPluginManager.ChannelIdentifier channelId) {
        super(channelId);
    }

    @Override
    public boolean regPlugin(IotdmPlugin plugin, String onem2mUri) {
        pluginMap.put(onem2mUri, plugin);
        return true;
    }

    @Override
    public IotdmPlugin getPlugin(String onem2mUri) {
        return pluginMap.get(onem2mUri);
    }

    @Override
    public Stream<Map.Entry<String, IotdmPlugin>> getPluginStream() {
        return pluginMap.entrySet().stream();
    }

    @Override
    public boolean hasPlugin(IotdmPlugin plugin) {
        return pluginMap.containsValue(plugin);
    }

    @Override
    public boolean hasPlugin(IotdmPlugin plugin, String onem2mUri) {
        if (!pluginMap.containsKey(onem2mUri)) {
            return false;
        }

        IotdmPlugin storedPlugin = pluginMap.get(onem2mUri);
        return null != storedPlugin && storedPlugin.isPlugin(plugin);
    }

    @Override
    public boolean removePlugin(IotdmPlugin plugin) {
        boolean ret = false;
        for (Map.Entry<String, IotdmPlugin> entry : pluginMap.entrySet()) {
            if (entry.getValue().isPlugin(plugin)) {
                this.pluginMap.remove(entry.getKey());
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public boolean removePlugin(IotdmPlugin plugin, String onem2mUri) {
        return this.hasPlugin(plugin, onem2mUri) && (null != pluginMap.remove(onem2mUri));
    }

    @Override
    public boolean isEmpty() {
        return pluginMap.isEmpty();
    }
}
