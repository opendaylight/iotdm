/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.plugin.configuration.PluginSpecificConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.plugin.configuration.plugin.specific.configuration.SimpleConfigBuilder;

/**
 * Interface describes implementation of SimpleConfig clients.
 */
public interface IotdmPluginSimpleConfigClient extends IotdmPluginConfigurable {

    /**
     * Method passes SimpleConfig configuration to the plugin instance.
     * This method is called in these cases (implementation of the plugin must be able to handle them all):
     *  1. During registration when StartupConfig is available.
     *  2. When the configuration has been created through RPC call.
     *  3. When the configuration has been changed through RPC call.
     *  4. When the configuration has been deleted through RPC call, null is passed instead of configuration.
     *  5. Anytime when onem2m-core module is reloaded.
     *
     * Plugin instance should throw IotdmPluginSimpleConfigException in case
     * of invalid configuration.
     *
     * @param configuration New configuration for the plugin instance. Null is passed in case of configuration delete.
     * @throws IotdmPluginSimpleConfigException
     */
    void configure(IotdmSimpleConfig configuration) throws IotdmPluginSimpleConfigException;

    /**
     * Method returns current configuration of the plugin instance.
     * @return Current configuration
     */
    IotdmSimpleConfig getSimpleConfig();

    /**
     * Default implementation of the getRunningConfig() method
     * for SimpleConfig clients.
     * Method calls getSimpleConfig() of the SimpleConfig client
     * and builds common PluginConfiguration result.
     * @return Common PluginConfiguration instance
     */
    @Override
    default PluginSpecificConfiguration getRunningConfig() {
        IotdmSimpleConfig cfg = null;

        // Call the getSimpleConfig() of this plugin instance
        cfg = this.getSimpleConfig();
        if (null == cfg) {
            // There's not any configuration configured
            return null;
        }

        // Build the new PluginConfiguration instance including the SimpleConfig configuration
        // of this plugin instance
        return new SimpleConfigBuilder().setPluginSimpleConfig(cfg.getConfiguration()).build();
    }
}
