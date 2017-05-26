/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.registry;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * This class implements registry which registers only one plugin instance
 * for all URIs.
 * CommunicationChannel which uses such registry runs in exclusive mode so
 * all requests are handled by the only one plugin instance registered.
 */
public class IotdmRxPluginsRegistryExclusive extends IotdmRxPluginsRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmRxPluginsRegistryExclusive.class);
    private IotdmPlugin plugin;

    public IotdmRxPluginsRegistryExclusive(IotdmPluginManager.ChannelIdentifier channelIdentifier) {
        super(channelIdentifier);
    }

    @Override
    public boolean regPlugin(IotdmPlugin plugin, String onem2mUri) {
        this.plugin = plugin;
        return true;
    }

    @Override
    public IotdmPlugin getPlugin(String onem2mUri) {
        return this.plugin;
    }

    @Override
    public Stream<Map.Entry<String, IotdmPlugin>> getPluginStream() {
        Map<String, IotdmPlugin> map = new HashMap<>();
        map.put("*", plugin);
        return map.entrySet().stream();
    }

    @Override
    public boolean hasPlugin(IotdmPlugin plugin) {
        return null != this.plugin && plugin.getPluginName().equals(this.plugin.getPluginName());
    }

    @Override
    public boolean hasPlugin(IotdmPlugin plugin, String onem2mUri) {
        return this.hasPlugin(plugin);
    }

    @Override
    public boolean removePlugin(IotdmPlugin plugin) {
        if (! hasPlugin(plugin)) {
            return false;
        }

        this.plugin = null;
        return true;
    }

    @Override
    public boolean removePlugin(IotdmPlugin plugin, String onem2mUri) {
        return this.removePlugin(plugin);
    }

    @Override
    public boolean isEmpty() { return this.plugin == null; }
}
