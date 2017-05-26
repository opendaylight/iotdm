/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.simpleadapter.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.iotdm.onem2m.dbapi.Onem2mDbApiClientPlugin;
import org.opendaylight.iotdm.onem2m.dbapi.Onem2mPluginsDbApi;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mSimpleAdapterProvider implements Onem2mDbApiClientPlugin, AutoCloseable {


    private static final Logger LOG = LoggerFactory.getLogger(Onem2mSimpleAdapterProvider.class);
    private final DataBroker dataBroker;
    private final Onem2mService onem2mService;
    private Onem2mSimpleAdapterManager saMgr = null;
    private Onem2mSimpleAdapterHttpServer saHttpServer = null;
    private final Onem2mPluginsDbApi onem2mDbApi;
    //private Onem2mSimpleAdapterMqttClient saMqttClient = null;
    //private Onem2mSimpleAdapterCoapServer saCoapServer = null;

    public Onem2mSimpleAdapterProvider(DataBroker dataBroker, RpcProviderRegistry rpcRegistry,
                                       Onem2mPluginsDbApi onem2mDbApi) {
        this.dataBroker = dataBroker;
        this.onem2mService = rpcRegistry.getRpcService(Onem2mService.class);
        this.onem2mDbApi = onem2mDbApi;
    }

    public void init() {
        try {
            onem2mDbApi.registerDbClientPlugin(this);
        } catch (IotdmPluginRegistrationException e) {
            LOG.error("Failed to register at PluginDbApi: {}", e);
            return;
        }

        LOG.info("Onem2mSimpleAdapterProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mSimpleAdapterProvider Closed");
        saMgr.close();
        onem2mDbApi.unregisterDbClientPlugin(this);
    }

    @Override
    public void dbClientStart() {
        saMgr = new Onem2mSimpleAdapterManager(dataBroker, onem2mService, onem2mDbApi);
        saHttpServer = new Onem2mSimpleAdapterHttpServer(saMgr);
        //saMqttClient = new Onem2mSimpleAdapterMqttClient(saMgr);
        //saCoapServer = new Onem2mSimpleAdapterCoapServer(saMgr);
        saMgr.setOnem2mSimpleAdapterHttpServer(saHttpServer);
        //saMgr.setOnem2mSimpleAdapterMqttClient(saMqttClient);
        //saMgr.setOnem2mSimpleAdapterCoapServer(saCoapServer);
    }

    @Override
    public void dbClientStop() {
        if (null != saMgr) {
            saMgr.close();
            saMgr = null;
        }
    }

    @Override
    public String getPluginName() {
        return "Onem2mSimpleAdapterProvider";
    }
}
