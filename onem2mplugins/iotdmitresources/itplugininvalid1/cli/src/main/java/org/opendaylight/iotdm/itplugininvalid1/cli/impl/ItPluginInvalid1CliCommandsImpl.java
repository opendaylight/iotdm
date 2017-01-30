/*
 * Copyright © 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.itplugininvalid1.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.iotdm.itplugininvalid1.cli.api.ItPluginInvalid1CliCommands;

public class ItPluginInvalid1CliCommandsImpl implements ItPluginInvalid1CliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(ItPluginInvalid1CliCommandsImpl.class);
    private final DataBroker dataBroker;

    public ItPluginInvalid1CliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("ItPluginInvalid1CliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}