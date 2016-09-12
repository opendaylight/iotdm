/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.dao.factory.DaoResourceTreeFactory;
import org.opendaylight.iotdm.onem2m.core.database.lock.Locker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by gguliash on 4/19/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class TransactionManager implements Closeable{
    private static final Logger LOG = LoggerFactory.getLogger(TransactionManager.class);
    private final Cache cache;
    private final ResourceTreeReader resourceTreeReader;
    private final DaoResourceTreeReader daoResourceTreeReader;
    private final Locker locker;
    private final DaoResourceTreeFactory daoResourceTreeFactory;

    public TransactionManager(DaoResourceTreeFactory daoResourceTreeFactory, Locker locker) {
        this.locker = locker;
        this.daoResourceTreeFactory = daoResourceTreeFactory;
        this.daoResourceTreeReader = daoResourceTreeFactory.getDaoResourceTreeReader();
        this.cache = new Cache(daoResourceTreeReader);
        this.resourceTreeReader = new ResourceTreeReader(cache, daoResourceTreeReader, new TTLGarbageCollector(this, locker));
    }

    /**
     *
     * @return locker which should be notified before any change in the database
     */
    public Locker getLocker() {
        return locker;
    }

    /**
     *
     * @return database writer interface which also updated cache
     */
    public ResourceTreeWriter getDbResourceTreeWriter() {
        return new ResourceTreeWriter(cache, daoResourceTreeFactory.getDaoResourceTreeWriter(), resourceTreeReader);
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
