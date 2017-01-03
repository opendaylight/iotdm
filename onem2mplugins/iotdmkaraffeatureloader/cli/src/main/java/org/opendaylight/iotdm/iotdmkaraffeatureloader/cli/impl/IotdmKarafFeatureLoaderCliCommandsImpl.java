/*
 * Copyright © 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.iotdmkaraffeatureloader.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.iotdmkaraffeatureloader.cli.api.IotdmKarafFeatureLoaderCliCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotdmKarafFeatureLoaderCliCommandsImpl implements IotdmKarafFeatureLoaderCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(IotdmKarafFeatureLoaderCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public IotdmKarafFeatureLoaderCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("IotdmKarafFeatureLoaderCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}