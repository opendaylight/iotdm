/*
 * Copyright © 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.itiotdmplugin.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginManager;
import org.opendaylight.iotdm.plugininfra.commchannels.common.IotdmHandlerPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.plugininfra.commchannels.common.IotdmPluginRequest;
import org.opendaylight.iotdm.plugininfra.commchannels.common.IotdmPluginResponse;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmPluginSimpleConfigClient;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmPluginSimpleConfigException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.simpleconfig.IotdmSimpleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItIotdmHandlerPluginCustomProtocol implements IotdmHandlerPlugin, IotdmPluginSimpleConfigClient {
    private static final Logger LOG = LoggerFactory.getLogger(ItIotdmHandlerPluginCustomProtocol.class);
    protected DataBroker dataBroker;

    public ItIotdmHandlerPluginCustomProtocol(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        try {
            IotdmPluginManager.getInstance()
                              .registerSimpleConfigPlugin(this)
                              .registerPluginHttp(this, 8289, IotdmPluginManager.Mode.Exclusive, null);
        } catch (final IotdmPluginRegistrationException e) {
            LOG.error("Failed to register plugin: {}", e);
            // Clear all possibly successful registrations
            IotdmPluginManager.getInstance()
                              .unregisterIotdmPlugin(this)
                              .unregisterSimpleConfigPlugin(this);
        }
    }

    @Override
    public void handle(final IotdmPluginRequest request, final IotdmPluginResponse response) {
        LOG.info("Handler for the HTTP registered plugin. This plugin is used for integration test only and it doesn`t use this method");
    }

    @Override
    public String getPluginName() {
        return "itIotdmPlugin";
    }

    @Override
    public void close() {
        IotdmPluginManager.getInstance()
                          .unregisterIotdmPlugin(this)
                          .unregisterIotdmPlugin(this);
    }

    @Override
    public void configure(final IotdmSimpleConfig configuration) throws IotdmPluginSimpleConfigException {
        LOG.info("Configuration of plugin. This plugin is used for integration test only and it doesn`t use this method");
    }

    @Override
    public IotdmSimpleConfig getSimpleConfig() {
        return null;
    }
}
