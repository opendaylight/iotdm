/*
 * Copyright © 2016 Cisco Systems, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2mexample.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.onem2m.dbapi.Onem2mPluginsDbApi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mExampleProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mExampleProvider.class);
    protected Onem2mService onem2mService;
    Onem2mExampleCustomProtocol onem2mExampleCustomProtocol;
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final Onem2mPluginsDbApi onem2mDbApi;

    public Onem2mExampleProvider(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry,
                                 final Onem2mPluginsDbApi onem2mDbApi) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.onem2mDbApi = onem2mDbApi;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        onem2mService = rpcProviderRegistry.getRpcService(Onem2mService.class);
        onem2mExampleCustomProtocol = new Onem2mExampleCustomProtocol(dataBroker, onem2mService, onem2mDbApi);

        LOG.info("Onem2mExampleProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        onem2mExampleCustomProtocol.close();
        LOG.info("Onem2mExampleProvider Closed");
    }
}