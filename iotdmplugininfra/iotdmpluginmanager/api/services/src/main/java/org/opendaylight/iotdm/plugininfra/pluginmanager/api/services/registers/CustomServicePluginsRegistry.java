/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.registers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginCommonInterface;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomServicePluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of generic registry of plugins for custom services.
 * @param <Tplugin> Type of plugins
 */
public class CustomServicePluginsRegistry<Tplugin extends IotdmPluginCommonInterface>
    implements IotdmPluginsCustomServicePluginRegistry<Tplugin> {

    private final Logger LOG = LoggerFactory.getLogger(CustomServicePluginsRegistry.class);
    private final ConcurrentHashMap<String, Tplugin> registry = new ConcurrentHashMap<>();

    @Override
    public void registerPlugin(Tplugin plugin) throws IotdmPluginRegistrationException {
        if (registry.containsKey(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin))) {
            throw new IotdmPluginRegistrationException(
                "Plugin instance already registered: " + plugin.getDebugString());
        }

        registry.put(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin), plugin);
        LOG.debug("New plugin instance registered: {}", plugin.getDebugString());
    }

    @Override
    public void unregisterPlugin(Tplugin plugin) {
        if (! registry.containsKey(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin))) {
            LOG.warn("Attempt to unregistered non-registered plugin: {}", plugin.getDebugString());
            return;
        }

        registry.remove(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin));
    }

    @Override
    public boolean isPluginRegistered(IotdmPluginCommonInterface plugin) {
        return registry.containsKey(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin));
    }

    @Override
    public Collection<IotdmPluginCommonInterface> getRegisteredPlugins() {
        List<IotdmPluginCommonInterface> list = new LinkedList<>();

        for (Tplugin plugin : registry.values()) {
            list.add(plugin);
        }
        return list;

        // Why cannot just do this ?
//        return registry.values();
    }
}
