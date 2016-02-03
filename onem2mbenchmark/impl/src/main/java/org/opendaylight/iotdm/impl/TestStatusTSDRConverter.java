/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.impl;

import org.opendaylight.iotdm.tsdr.AbstractIoT2TSDRConverter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.rev150105.TestStatus;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class TestStatusTSDRConverter extends AbstractIoT2TSDRConverter{
    @Override
    public boolean needTimeStamp() {
        return true;
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public String getNodeID(InstanceIdentifier<?> id, DataObject dataObject) {
        return "TestNode";
    }

    @Override
    public String getData(InstanceIdentifier<?> id, DataObject dataObject) {
        return ((TestStatus)dataObject).getExecStatus().name();
    }
}
