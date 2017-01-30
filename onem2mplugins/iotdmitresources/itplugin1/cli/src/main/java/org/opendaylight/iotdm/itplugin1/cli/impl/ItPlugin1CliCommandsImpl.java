/*
 * Copyright © 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.itplugin1.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.iotdm.itplugin1.cli.api.ItPlugin1CliCommands;

public class ItPlugin1CliCommandsImpl implements ItPlugin1CliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(ItPlugin1CliCommandsImpl.class);
    private final DataBroker dataBroker;

    public ItPlugin1CliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("ItPlugin1CliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}