/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.api.services;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.CustomServiceConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.CustomServiceState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.custom.services.output.iotdm.plugin.manager.custom.services.list.custom.service.plugins.table.custom.service.plugin.instances.PluginInstanceCustomData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.custom.services.list.CustomServicePluginData;

/**
 * Custom service providing some services for IoTDM plugins. Services implementing
 * this interface can register to PluginManager and can use functionality (services)
 * of PluginManager provided for the custom services.
 */
public interface IotdmPluginsCustomService {

    /**
     * Return unique name of this custom service. This name is used as key to registry
     * of custom services in PluginManager.
     * @return Custom service's unique name
     */
    String getCustomServiceName();

    /**
     * Return plugin data of the plugin instance augmented by this custom service.
     * These data are used by PluginManager in output of iotdm-plugin-manager-plugin-data RPC call.
     * @param plugin Plugin instance
     * @return Custom service specific plugin data
     */
    CustomServicePluginData getServiceSpecificPluginData(IotdmPlugin plugin);

    /**
     * Return custom data for the given plugin instance augmented by this custom service.
     * These data are used by PluginManager in output of iotdm-plugin-manager-custom-services RPC call.
     * @param plugin Plugin instance
     * @return Custom data of the plugin instance
     */
    PluginInstanceCustomData getPluginInstanceCustomData(IotdmPlugin plugin);

    /**
     * Return data about state of this custom service.
     * These data are used by PluginManager in output of iotdm-plugin-manager-custom-services RPC call.
     * @return Custom service state data augmented by state of this custom service
     */
    CustomServiceState getServiceStateData();

    /**
     * Return configuration data of this custom service.
     * These data are used by PluginManager in output of iotdm-plugin-manager-custom-services RPC call.
     * @return Custom service configuration data augmented by configuration of this custom service
     */
    CustomServiceConfig getServiceConfig();
}
