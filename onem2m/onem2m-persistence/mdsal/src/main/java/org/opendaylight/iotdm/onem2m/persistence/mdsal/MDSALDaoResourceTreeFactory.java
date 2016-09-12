/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.persistence.mdsal;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.database.dao.factory.DaoResourceTreeFactory;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.read.MDSALResourceTreeReader;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.write.MDSALResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.write.MDSALTransactionWriter;

import java.io.IOException;

/**
 * Created by gguliash on 5/20/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class MDSALDaoResourceTreeFactory implements DaoResourceTreeFactory {
    private DataBroker dataBroker;

    public MDSALDaoResourceTreeFactory(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public DaoResourceTreeWriter getDaoResourceTreeWriter() {
        return new MDSALResourceTreeWriter(new MDSALTransactionWriter(dataBroker));
    }

    @Override
    public DaoResourceTreeReader getDaoResourceTreeReader() {
        return new MDSALResourceTreeReader(dataBroker);
    }

    @Override
    public void close(){

    }
}
