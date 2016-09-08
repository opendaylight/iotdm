/*
 * Copyright © 2016 Cisco Systems, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2mexample.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.onem2m.client.*;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mExampleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mExampleProvider.class);
    protected Onem2mService onem2mService;
    Onem2mExampleCustomProtocol onem2mExampleCustomProtocol;
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;

    public Onem2mExampleProvider(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        try {
            Thread.sleep(10000); // TODO: verify if we have to remove this ...
        } catch(InterruptedException e) {
        }
        onem2mService = rpcProviderRegistry.getRpcService(Onem2mService.class);
        onem2mExampleCustomProtocol = new Onem2mExampleCustomProtocol(dataBroker, onem2mService);

        LOG.info("Onem2mExampleProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("Onem2mExampleProvider Closed");
    }
}