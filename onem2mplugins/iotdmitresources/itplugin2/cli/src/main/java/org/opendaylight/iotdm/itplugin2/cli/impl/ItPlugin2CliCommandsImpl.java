/*
 * Copyright © 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.itplugin2.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.iotdm.itplugin2.cli.api.ItPlugin2CliCommands;

public class ItPlugin2CliCommandsImpl implements ItPlugin2CliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(ItPlugin2CliCommandsImpl.class);
    private final DataBroker dataBroker;

    public ItPlugin2CliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("ItPlugin2CliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}