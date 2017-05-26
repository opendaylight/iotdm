/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.customservices;

import java.util.Map;
import java.util.Optional;
import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginManager;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginCommonInterface;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginLoader;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.data.definition.IotdmCommonPluginData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.data.definition.IotdmCommonPluginDataBuilder;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

/**
 * Utility methods for PluginManager and related services
 */
public final class PluginManagerServicesUtils {

    /**
     * Handles registration error. Logs message and throws RegistrationException with
     * the same message as logged.
     * @param LOG Logger instance
     * @param format Formatting string (Log4j style)
     * @param args Error message arguments
     * @throws IotdmPluginRegistrationException
     */
    public static void handleRegistrationError(Logger LOG, String format, String... args)
            throws IotdmPluginRegistrationException {
        String msg = MessageFormatter.arrayFormat(format, args).getMessage();
        LOG.error("IotdmPlugin registration error: {}", msg);
        throw new IotdmPluginRegistrationException(msg);
    }

    /**
     * Checks whether the plugin instance meets filtering criteria
     * @param pluginName Plugin name filter
     * @param instanceId Instance name filter
     * @param plugin The plugin instance to be checked
     * @return True if the plugin instance meets criteria, false otherwise.
     */
    public static boolean filterPlugin(final String pluginName, final String instanceId,
                                       final IotdmPluginCommonInterface plugin) {
        if (null == plugin) {
            return false;
        }

        if (null != pluginName) {
            if (! plugin.getPluginName().equals(pluginName)) {
                return false;
            }
        }

        if (null != instanceId) {
            if (! plugin.getInstanceName().equals(instanceId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether the plugin instance meets filtering criteria
     * @param filters Filtering criteria
     * @param plugin The plugin instance to be checked
     * @return True if the plugin instance meets criteria, false otherwise.
     */
    public static boolean applyPluginFilters(final IotdmPluginFilters filters,
                                             final IotdmPluginCommonInterface plugin) {
        if (null == plugin) {
            return false;
        }

        if (null == filters) {
            // no any constraints
            return true;
        }

        String pluginLoader = filters.getPluginLoaderName();
        String pluginName = filters.getPluginName();
        String pluginInstanceId = filters.getPluginInstanceName();

        Map<String, IotdmPluginLoader> pluginLoaders = IotdmPluginManager.getInstance().getMgrPluginLoaders();

        IotdmPluginLoader loader = null;
        if ((null != pluginLoader) && (! pluginLoader.isEmpty())) {
            if (! pluginLoaders.containsKey(pluginLoader)) {
                return false;
            }

            loader = pluginLoaders.get(pluginLoader);
        }

        if (null != loader) {
            if (! loader.hasLoadedPlugin(plugin)) {
                return false;
            }
        }

        return filterPlugin(pluginName, pluginInstanceId, plugin);
    }

    public static IotdmCommonPluginData createIotdmPluginData(final IotdmPluginCommonInterface plugin,
                                                              String loaderName) {
        Map<String, IotdmPluginLoader> pluginLoaders = IotdmPluginManager.getInstance().getMgrPluginLoaders();

        if (null == loaderName || loaderName.isEmpty()) {
            // Must walk all loaders and try to find it
            Optional<IotdmPluginLoader> currentLoader =
                    pluginLoaders.values().stream()
                            .filter(l -> l.hasLoadedPlugin(plugin)).findFirst();
            if (currentLoader.isPresent()) {
                loaderName = currentLoader.get().getLoaderName();
            }
        }

        // Build pluginData which are common for the instance
        IotdmCommonPluginDataBuilder pluginData = new IotdmCommonPluginDataBuilder()
                                                    .setPluginName(plugin.getPluginName())
                                                    .setPluginInstanceName(plugin.getInstanceName())
                                                    .setPluginClass(plugin.getClass().getName())
                                                    .setPluginLoader(loaderName);
        return pluginData.build();
    }

    /**
     * Creates instances of IotdmCommonPluginData for specific plugin instance.
     * @param plugin Plugin instance
     * @return Created IotdmCommonPluginData instance
     */
    public static IotdmCommonPluginData createIotdmPluginData(IotdmPluginCommonInterface plugin) {
        return createIotdmPluginData(plugin, null);
    }
}
