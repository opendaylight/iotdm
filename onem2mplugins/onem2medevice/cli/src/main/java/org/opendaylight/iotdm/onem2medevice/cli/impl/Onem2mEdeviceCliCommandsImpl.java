/*
 * Copyright © 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2medevice.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.iotdm.onem2medevice.cli.api.Onem2mEdeviceCliCommands;

public class Onem2mEdeviceCliCommandsImpl implements Onem2mEdeviceCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mEdeviceCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public Onem2mEdeviceCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("Onem2mEdeviceCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}