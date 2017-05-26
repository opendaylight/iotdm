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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.data.definition.IotdmCommonPluginData;
import org.slf4j.Logger;

/**
 * Service of PluginManager providing services for other custom services registering
 * IoTDM plugins.
 */
public interface IotdmPluginManagerCustomServicesService {

    /**
     * Register custom service and its read only registry of IoTDM plugins.
     * @param customService Custom plugin service implementation
     * @param customServiceRegistry Registry of IoTDM plugins registered by the custom service
     * @throws IotdmPluginsCustomServiceRegistrationException
     */
    void registerCustomService(IotdmPluginsCustomService customService,
                               IotdmPluginsCustomServiceReadOnlyRegistry customServiceRegistry)
        throws IotdmPluginsCustomServiceRegistrationException;

    /**
     * Check whether the custom service is already registered.
     * @param customService The custom service implementation
     * @return True if registered, False if not registered
     */
    boolean isCustomServiceRegistered(IotdmPluginsCustomService customService);

    /**
     * Unregister custom service implementation.
     * @param customService
     */
    void unregisterCustomService(IotdmPluginsCustomService customService);


    /*
     * Utility methods for custom services
     */

    /**
     * Utility method for custom services which can be used to log and throw exception in
     * case of registration failure of the plugin registering to the custom service.
     * @param LOG Logger object used to log error message
     * @param format Formatting string of the error message
     * @param args Arguments of the error message
     * @throws IotdmPluginRegistrationException The exception is thrown all the time
     */
    void handlePluginRegistrationError(Logger LOG, String format, String... args)
        throws IotdmPluginRegistrationException;

    /**
     * Checks whether the plugin instance meets filtering criteria passed as
     * pluginName and/or instanceId.
     * @param pluginName Plugin name filter
     * @param instanceId Instance name filter
     * @param plugin The plugin instance to be checked
     * @return True if the plugin instance meets criteria, false otherwise.
     */
    boolean filterPlugin(final String pluginName, final String instanceId,
                         final IotdmPlugin plugin);

    /**
     * Check whether the plugin instance meets filtering criteria passed as object
     * of IotdmPluginsFilters.
     * @param filters Filtering criteria
     * @param plugin The plugin instance to be checked
     * @return True if the plugin instance meets criteria, false otherwise.
     */
    boolean applyPluginFilters(final IotdmPluginFilters filters,
                               final IotdmPlugin plugin);

    /**
     * Creates instance of IoTDMCommonPluginData set to plugin data of the given plugin
     * instance.
     * @param plugin Plugin instance
     * @return Plugin data object filled by data of the plugin instance
     */
    IotdmCommonPluginData createIotdmPluginData(IotdmPlugin plugin);
}
