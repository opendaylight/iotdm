/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.registers;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;

/**
 * Implements some utility methods for plugin registries of custom services.
 */
public class PluginManagerCustomServiceRegistryUtils {

    /**
     * Generates unique plugin instance identifier string which is used as key in registry
     * implementations.
     * @param plugin IoTDM plugin instance
     * @return Key string unique for the given plugin
     */
    public static String getPluginKey(IotdmPlugin plugin) {
        return plugin.getPluginName() + ":" + plugin.getInstanceName();
    }
}
