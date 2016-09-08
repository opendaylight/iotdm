/*
 * Copyright © 2016 Cisco Systems, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2mexample.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.iotdm.onem2mexample.cli.api.Onem2mExampleCliCommands;

public class Onem2mExampleCliCommandsImpl implements Onem2mExampleCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mExampleCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public Onem2mExampleCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("Onem2mExampleCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}