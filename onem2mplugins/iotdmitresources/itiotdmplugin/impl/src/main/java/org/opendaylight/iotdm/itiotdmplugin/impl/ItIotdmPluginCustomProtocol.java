/*
 * Copyright © 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.itiotdmplugin.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPlugin;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginDbClient;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRequest;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginResponse;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.simpleconfig.IotdmPluginSimpleConfigClient;
import org.opendaylight.iotdm.onem2m.plugins.simpleconfig.IotdmPluginSimpleConfigException;
import org.opendaylight.iotdm.onem2m.plugins.simpleconfig.IotdmSimpleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItIotdmPluginCustomProtocol implements IotdmPlugin, IotdmPluginDbClient, IotdmPluginSimpleConfigClient {
    private static final Logger LOG = LoggerFactory.getLogger(ItIotdmPluginCustomProtocol.class);
    protected DataBroker dataBroker;

    public ItIotdmPluginCustomProtocol(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        try {
            Onem2mPluginManager.getInstance()
                    .registerSimpleConfigPlugin(this)
                    .registerDbClientPlugin(this)
                    .registerPluginHttp(this, 8289, Onem2mPluginManager.Mode.Exclusive, null);
        } catch (final IotdmPluginRegistrationException e) {
            LOG.error("Failed to register plugin: {}", e);
            // Clear all possibly successful registrations
            Onem2mPluginManager.getInstance()
                    .unregisterIotdmPlugin(this)
                    .unregisterDbClientPlugin(this)
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
    public void close() throws Exception {
        Onem2mPluginManager.getInstance()
                .unregisterIotdmPlugin(this)
                .unregisterDbClientPlugin(this)
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
