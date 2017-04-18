/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.persistence.mdsal;


import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.opendaylight.iotdm.onem2m.core.database.dao.factory.DaoResourceTreeFactoryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mPersistenceMdsalProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mPersistenceMdsalProvider.class);
    private final DataBroker dataBroker;
    private final DaoResourceTreeFactoryRegistry daoFactoryRegistry;
    private MDSALDaoResourceTreeFactory factory;

    public Onem2mPersistenceMdsalProvider(DataBroker dataBroker, DaoResourceTreeFactoryRegistry daoFactoryRegistry) {
        this.dataBroker = dataBroker;
        this.daoFactoryRegistry = daoFactoryRegistry;
    }

    public void init() {
        this.factory = new MDSALDaoResourceTreeFactory(dataBroker);
        daoFactoryRegistry.registerDaoPlugin(factory);
        LOG.info("Onem2mPersistenceMdsalProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        Onem2mCoreProvider.getInstance().unregisterDaoPlugin();
        this.factory.close();
        LOG.info("Onem2mPersistenceMdsalProvider Closed");
    }
}
