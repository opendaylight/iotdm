/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.dao.factory.DaoResourceTreeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * Created by gguliash on 4/19/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class TransactionManager implements Closeable{
    private static final Logger LOG = LoggerFactory.getLogger(TransactionManager.class);
    private final Cache cache;
    private final ResourceTreeReader resourceTreeReader;
    private final DaoResourceTreeReader daoResourceTreeReader;
    private final DaoResourceTreeFactory daoResourceTreeFactory;
    private final DbNotifier dbNotifier;
    private final BGDeleteProcessor bgDeleteProcessor;
    private final DataBroker dataBroker;

    public TransactionManager(DataBroker dataBroker, DaoResourceTreeFactory daoResourceTreeFactory) {
        this.daoResourceTreeFactory = daoResourceTreeFactory;
        this.daoResourceTreeReader = daoResourceTreeFactory.getDaoResourceTreeReader();
        this.cache = new Cache(daoResourceTreeReader);
        this.bgDeleteProcessor = new BGDeleteProcessor();
        this.resourceTreeReader = new ResourceTreeReader(cache, daoResourceTreeReader, bgDeleteProcessor);
        this.dataBroker = dataBroker;
        this.dbNotifier = new DbNotifier(dataBroker);
    }

    /**
     *
     * @return database writer interface which also updated cache
     */
    public ResourceTreeWriter getDbResourceTreeWriter() {
        return new ResourceTreeWriter(cache, daoResourceTreeFactory.getDaoResourceTreeWriter(), resourceTreeReader, dbNotifier);
    }

    /**
     *
     * @return database reader interface which uses cache if entry is inside
     */
    public ResourceTreeReader getTransactionReader() {
        return resourceTreeReader;
    }

    @Override
    public void close(){
        daoResourceTreeFactory.close();
    }
}
