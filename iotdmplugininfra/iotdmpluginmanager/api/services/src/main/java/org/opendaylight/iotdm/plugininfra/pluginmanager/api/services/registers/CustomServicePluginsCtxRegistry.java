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
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomServicePluginContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of generic registry of plugins and their contexts for custom services.
 * @param <Tplugin> Type of IoTDM plugin
 * @param <Tctx> Type of plugin context to be stored together with plugin instance
 */
public class CustomServicePluginsCtxRegistry<Tplugin extends IotdmPlugin, Tctx>
    implements IotdmPluginsCustomServicePluginContextRegistry<Tplugin, Tctx> {

    private final Logger LOG = LoggerFactory.getLogger(CustomServicePluginsCtxRegistry.class);
    private final ConcurrentHashMap<String,
                                    IotdmPluginsCustomServicePluginContextRegistry.
                                        PluginContextRegistryItem<Tplugin, Tctx>> registry = new ConcurrentHashMap<>();

    @Override
    public void registerPlugin(Tplugin plugin, Tctx context) throws IotdmPluginRegistrationException {
        if (registry.containsKey(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin))) {
            throw new IotdmPluginRegistrationException(
                "Plugin instance already registered: " + plugin.getDebugString());
        }

        registry.put(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin),
                     new IotdmPluginsCustomServicePluginContextRegistry.PluginContextRegistryItem<>(plugin, context));
        LOG.debug("New plugin instance registered: {}", plugin.getDebugString());
    }

    @Override
    public void unregisterPlugin(Tplugin plugin) {
        /* TODO how to deduplicate ? */
        if (! registry.containsKey(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin))) {
            LOG.warn("Attempt to unregistered non-registered plugin: {}", plugin.getDebugString());
            return;
        }

        registry.remove(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin));
    }

    @Override
    public boolean isPluginRegistered(IotdmPlugin plugin) {
        return registry.containsKey(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin));
    }

    @Override
    public Collection<IotdmPlugin> getRegisteredPlugins() {
        List<IotdmPlugin> list = new LinkedList<>();

        for (IotdmPluginsCustomServicePluginContextRegistry.PluginContextRegistryItem<Tplugin, Tctx> item :
                registry.values()) {
            list.add(item.getPlugin());
        }

        return list;
    }

    @Override
    public Tctx getPluginContext(Tplugin plugin) {
        IotdmPluginsCustomServicePluginContextRegistry.PluginContextRegistryItem<Tplugin, Tctx> item =
            registry.get(PluginManagerCustomServiceRegistryUtils.getPluginKey(plugin));
        if (null == item) {
            return null;
        }

        return item.getContext();
    }

    @Override
    public Collection<PluginContextRegistryItem<Tplugin, Tctx>> getAllRegistryItems() {
        return registry.values();
    }
}
