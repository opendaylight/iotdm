/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins;

/**
 * This interface describes methods used by PluginManager and other
 * services registering IotDM plugins in order
 * to make and maintain plugin registrations.
 */
public interface IotdmPlugin extends AutoCloseable {
    /**
     * Returns the name of plugin (common for all instances).
     * @return Plugin name
     */
    String getPluginName();

    /**
     * Returns the name of plugin instance unique among all instances
     * of the same plugin.
     * @return Instance name
     */
    default String getInstanceName() {
        return "default";
    }

    /**
     * Compares this instance and given plugin and returns true in case
     * of match and false otherwise.
     * @param plugin The second plugin instance.
     * @return True if the instances are the same, False otherwise.
     */
    default boolean isPlugin(IotdmPlugin plugin) {
        if (this.getPluginName().equals(plugin.getPluginName()) &&
            this.getInstanceName().equals(plugin.getInstanceName())) {
            return true;
        }
        return false;
    }

    /**
     * Returns debugging string describing the plugin instance.
     * @return String with plugin parameters.
     */
    default String getDebugString() {
        return "PluginName: " + this.getPluginName() + " InstanceName: " + this.getInstanceName();
    }
}
