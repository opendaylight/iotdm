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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mSimpleAdapterProvider implements BindingAwareProvider, AutoCloseable {


    private static final Logger LOG = LoggerFactory.getLogger(Onem2mSimpleAdapterProvider.class);
    private DataBroker dataBroker;
    private Onem2mSimpleAdapterManager saMgr;
    private Onem2mSimpleAdapterHttpServer saHttpServer;
    //private Onem2mSimpleAdapterMqttClient saMqttClient;
    //private Onem2mSimpleAdapterCoapServer saCoapServer;

    @Override
    public void onSessionInitiated(ProviderContext session) {

        LOG.info("Onem2mSimpleAdapterProvider Session Initiated");
        DataBroker dataBroker = session.getSALService(DataBroker.class);
        Onem2mService onem2mService = session.getRpcService(Onem2mService.class);
        saMgr = new Onem2mSimpleAdapterManager(dataBroker, onem2mService);
        saHttpServer = new Onem2mSimpleAdapterHttpServer(saMgr);
        //saMqttClient = new Onem2mSimpleAdapterMqttClient(saMgr);
        //saCoapServer = new Onem2mSimpleAdapterCoapServer(saMgr);
        saMgr.setOnem2mSimpleAdapterHttpServer(saHttpServer);
        //saMgr.setOnem2mSimpleAdapterMqttClient(saMqttClient);
        //saMgr.setOnem2mSimpleAdapterCoapServer(saCoapServer);

    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mSimpleAdapterProvider Closed");
        saMgr.close();
    }


}
