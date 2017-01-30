/*
 * Copyright © 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2mitplugininvalid1.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mItPluginInvalid1Provider {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mItPluginInvalid1Provider.class);

    private final DataBroker dataBroker;

    public Onem2mItPluginInvalid1Provider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        //this is for Integration testing purposes - jar and kar file is builded with doSomething which is now
        //missing from Onem2mPluginManager
        //Onem2mPluginManager.getInstance().doSomehting();
        LOG.info("Onem2mItPluginInvalid1Provider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("Onem2mItPluginInvalid1Provider Closed");
    }
}