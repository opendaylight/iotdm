/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.simpleadapter.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.TransactionManager;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginDbClient;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginsDbApi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mSimpleAdapterProvider implements IotdmPluginDbClient, BindingAwareProvider, AutoCloseable {


    private static final Logger LOG = LoggerFactory.getLogger(Onem2mSimpleAdapterProvider.class);
    private TransactionManager transactionManager = null;
    private DataBroker dataBroker = null;
    private Onem2mService onem2mService = null;
    private Onem2mSimpleAdapterManager saMgr = null;
    private Onem2mSimpleAdapterHttpServer saHttpServer = null;
    //private Onem2mSimpleAdapterMqttClient saMqttClient = null;
    //private Onem2mSimpleAdapterCoapServer saCoapServer = null;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        dataBroker = session.getSALService(DataBroker.class);
        onem2mService = session.getRpcService(Onem2mService.class);

        if (!Onem2mPluginsDbApi.getInstance().registerPlugin(this)) {
            LOG.error("Failed to register at PluginDbApi");
            return;
        }

        LOG.info("Onem2mSimpleAdapterProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mSimpleAdapterProvider Closed");
        saMgr.close();
        Onem2mPluginsDbApi.getInstance().unregisterPlugin(this);
    }

    @Override
    public boolean dbClientStart(final ResourceTreeWriter twc, final ResourceTreeReader trc) {
        saMgr = new Onem2mSimpleAdapterManager(trc, dataBroker, onem2mService);
        saHttpServer = new Onem2mSimpleAdapterHttpServer(saMgr);
        //saMqttClient = new Onem2mSimpleAdapterMqttClient(saMgr);
        //saCoapServer = new Onem2mSimpleAdapterCoapServer(saMgr);
        saMgr.setOnem2mSimpleAdapterHttpServer(saHttpServer);
        //saMgr.setOnem2mSimpleAdapterMqttClient(saMqttClient);
        //saMgr.setOnem2mSimpleAdapterCoapServer(saCoapServer);
        return true;
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
