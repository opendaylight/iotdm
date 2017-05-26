/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.services;

import java.util.Collection;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginCommonInterface;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;

/**
 * Describes possible implementation of generic plugin and context registry for custom services
 * extending read only registry used by PluginManager.
 * @param <Tplugin> Type of IoTDM plugins stored in this registry
 * @param <Tctx> Type of context related to particular plugin instance
 */
public interface IotdmPluginsCustomServicePluginContextRegistry<Tplugin extends IotdmPluginCommonInterface, Tctx>
    extends IotdmPluginsCustomServiceReadOnlyRegistry {

    /**
     * Registry item storing plugin with its context
     * @param <Tplugin> Type of IoTDM plugin
     * @param <Tctx> Type of the plugin's context
     */
    class PluginContextRegistryItem<Tplugin, Tctx> {
        private final Tplugin plugin;
        private final Tctx context;

        public PluginContextRegistryItem(Tplugin plugin, Tctx context) {
            this.plugin = plugin;
            this.context = context;
        }

        public Tplugin getPlugin() {
            return plugin;
        }

        public Tctx getContext() {
            return context;
        }
    }

    /**
     * Register new IoTDM plugin with its opaque context data.
     * @param plugin IoTDM plugin instance
     * @param context Plugin's context data
     * @throws IotdmPluginRegistrationException
     */
    void registerPlugin(Tplugin plugin, Tctx context) throws IotdmPluginRegistrationException;

    /**
     * Unregister IoTDM plugin
     * @param plugin IoTDM plugin instance
     */
    void unregisterPlugin(Tplugin plugin);

    /**
     * Return context of the plugin stored with plugin when registered.
     * @param plugin IoTDM plugin instance
     * @return Context stored for the plugin instance
     */
    Tctx getPluginContext(Tplugin plugin);

    /**
     * Return collection of all plugins and their contexts registered in this registry.
     * @return Collection of registry items consisting of IoTDM plugin and its context
     */
    Collection<PluginContextRegistryItem<Tplugin, Tctx>> getAllRegistryItems();
}
