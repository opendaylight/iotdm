/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.services;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;

/**
 * Describes possible implementation of generic plugin registry for custom services
 * extending read only registry used by PluginManager.
 * @param <Tplugin> Type of IoTDM plugins stored in this registry
 */
public interface IotdmPluginsCustomServicePluginRegistry<Tplugin extends IotdmPlugin>
    extends IotdmPluginsCustomServiceReadOnlyRegistry {

    /**
     * Register new IoTDM plugin.
     * @param plugin IoTDM plugin instance
     * @throws IotdmPluginRegistrationException
     */
    void registerPlugin(Tplugin plugin) throws IotdmPluginRegistrationException;

    /**
     * Unregister IoTDM plugin
     * @param plugin IoTDM plugin instance
     */
    void unregisterPlugin(Tplugin plugin);
}
