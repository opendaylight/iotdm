/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.pluginmanager.customservices;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginManagerCustomServicesService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomService;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomServiceReadOnlyRegistry;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.services.IotdmPluginsCustomServiceRegistrationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.IotdmPluginFilters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.data.definition.IotdmCommonPluginData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of services of PluginManager provided to custom services implementations.
 */
public class PluginManagerCustomServicesService implements IotdmPluginManagerCustomServicesService {

    private final Logger LOG = LoggerFactory.getLogger(PluginManagerCustomServicesService.class);
    private final ConcurrentHashMap<String, CustomServiceRegistryItem> serviceRegistry = new ConcurrentHashMap<>();

    private static final PluginManagerCustomServicesService _instance = new PluginManagerCustomServicesService();

    /**
     * Registry item of the registry of custom services
     */
    private class  CustomServiceRegistryItem {
        private final IotdmPluginsCustomService service;
        private final IotdmPluginsCustomServiceReadOnlyRegistry registry;

        public CustomServiceRegistryItem(IotdmPluginsCustomService service,
                                         IotdmPluginsCustomServiceReadOnlyRegistry registry) {
            if (null == service || null == registry) {
                throw new InternalError("No service or registry provided");
            }
            this.service = service;
            this.registry = registry;
        }

        public IotdmPluginsCustomService getService() {
            return service;
        }

        public IotdmPluginsCustomServiceReadOnlyRegistry getRegistry() {
            return registry;
        }
    }

    /* Make the constructor private */
    private PluginManagerCustomServicesService() {}

    /* Provide the singleton */
    public static PluginManagerCustomServicesService getInstance() {
        return _instance;
    }

    private void checkService(IotdmPluginsCustomService customService)
        throws IotdmPluginsCustomServiceRegistrationException {
        if (serviceRegistry.containsKey(customService.getCustomServiceName())) {
            throw new IotdmPluginsCustomServiceRegistrationException(
                "Custom service with name " + customService.getCustomServiceName() + " already registered");
        }
    }

    private void logRegistered(IotdmPluginsCustomService customService) {
        LOG.debug("New custom service registered: {}", customService.getCustomServiceName());
    }

    @Override
    public void registerCustomService(IotdmPluginsCustomService customService,
                                      IotdmPluginsCustomServiceReadOnlyRegistry customServiceRegistry)
        throws IotdmPluginsCustomServiceRegistrationException {

        checkService(customService);
        serviceRegistry.put(customService.getCustomServiceName(),
                            new CustomServiceRegistryItem(customService, customServiceRegistry));
        logRegistered(customService);
    }

    @Override
    public boolean isCustomServiceRegistered(IotdmPluginsCustomService customService) {
        return serviceRegistry.containsKey(customService.getCustomServiceName());
    }

    @Override
    public void unregisterCustomService(IotdmPluginsCustomService customService) {
        if (! isCustomServiceRegistered(customService)) {
            LOG.warn("Attempt to unregister non-registered service: {}", customService.getCustomServiceName());
            return;
        }

        serviceRegistry.remove(customService.getCustomServiceName());
    }


    /*
     * Methods for PluginManager
     */

    /**
     * Return collection of custom services which have registered given plugin instance.
     * @param plugin Plugin instance
     * @return Collection of registered custom services
     */
    public Collection<IotdmPluginsCustomService> getServicesOfPlugin(IotdmPlugin plugin) {
        List<IotdmPluginsCustomService> list = new LinkedList<>();

        for (CustomServiceRegistryItem item : serviceRegistry.values()) {
            if (item.getRegistry().isPluginRegistered(plugin)) {
                list.add(item.getService());
            }
        }

        return list;
    }

    /**
     * Return collection of plugins registered by the given custom service.
     * @param customService Custom service registered by PluginManager
     * @return Collection of plugins
     */
    public Collection<IotdmPlugin> getPluginsOfService(IotdmPluginsCustomService customService) {
        CustomServiceRegistryItem item = serviceRegistry.get(customService.getCustomServiceName());
        if (null == item) {
            return new LinkedList<>();
        } else {
            return item.getRegistry().getRegisteredPlugins();
        }
    }

    /**
     * Return collection of all plugin instances registered by all custom services registered
     * by PluginManager.
     * @return Collection of all plugins
     */
    public Collection<IotdmPlugin> getAllPlugins() {
        List<IotdmPlugin> list = new LinkedList<>();

        for (CustomServiceRegistryItem item : serviceRegistry.values()) {
            Collection<IotdmPlugin> servicePlugins = item.getRegistry().getRegisteredPlugins();
            for (IotdmPlugin plugin : servicePlugins) {
                if (!list.contains(plugin)) {
                    list.add(plugin);
                }
            }
        }

        return list;
    }

    /**
     * Return collection of all services registered by PluginManager.
     * @return Collection of all services
     */
    public Collection<IotdmPluginsCustomService> getAllServices() {
        List<IotdmPluginsCustomService> list = new LinkedList<>();

        for (CustomServiceRegistryItem item : serviceRegistry.values()) {
            list.add(item.getService());
        }

        return list;
    }


    /*
     * Implementation of utility methods for custom services
     */

    @Override
    public void handlePluginRegistrationError(Logger LOG, String format, String... args)
            throws IotdmPluginRegistrationException {
        PluginManagerServicesUtils.handleRegistrationError(LOG, format, args);
    }

    @Override
    public boolean filterPlugin(final String pluginName, final String instanceId,
                         final IotdmPlugin plugin) {
        return PluginManagerServicesUtils.filterPlugin(pluginName, instanceId, plugin);
    }

    @Override
    public boolean applyPluginFilters(final IotdmPluginFilters filters,
                                      final IotdmPlugin plugin) {
        return PluginManagerServicesUtils.applyPluginFilters(filters, plugin);
    }

    @Override
    public IotdmCommonPluginData createIotdmPluginData(IotdmPlugin plugin) {
       return PluginManagerServicesUtils.createIotdmPluginData(plugin);
    }
}
