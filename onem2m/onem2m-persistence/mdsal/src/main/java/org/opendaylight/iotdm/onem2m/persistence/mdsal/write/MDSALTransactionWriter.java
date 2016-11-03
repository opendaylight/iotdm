/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.persistence.mdsal.write;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.MDSALDaoResourceTreeFactory;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gguliash on 4/19/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class MDSALTransactionWriter {
    private static final Logger LOG = LoggerFactory.getLogger(MDSALTransactionWriter.class);
    private WriteTransaction writeTransaction = null;
    private DataBroker broker;
    private boolean isClosed = true;
    private MDSALDaoResourceTreeFactory factory;

    public MDSALTransactionWriter(MDSALDaoResourceTreeFactory factory, DataBroker broker) {
        this.broker = broker;
        this.factory = factory;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (isClosed) return;
        LOG.error("Why was not it closed");
        close();
    }

    /**
     * Delete
     *
     * @param deleteIID            iid
     * @param logicalDatastoreType op vs config
     * @param <U>                  return value
     */
    protected <U extends org.opendaylight.yangtools.yang.binding.DataObject> void delete
    (InstanceIdentifier<U> deleteIID, LogicalDatastoreType logicalDatastoreType) {
        writeTransaction.delete(logicalDatastoreType, deleteIID);
    }

    /**
     * Update
     *
     * @param addIID               iid
     * @param data                 new value
     * @param logicalDatastoreType op vs config
     * @param <U>                  return value
     */
    protected <U extends org.opendaylight.yangtools.yang.binding.DataObject> void update // writeMerge
    (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        writeTransaction.merge(logicalDatastoreType, addIID, data, true);
    }

    /**
     * Create/Write
     *
     * @param addIID               iid
     * @param data                 new data
     * @param logicalDatastoreType op vs config
     * @param <U>                  return value
     */
    protected <U extends org.opendaylight.yangtools.yang.binding.DataObject> void create // writePut
    (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        writeTransaction.put(logicalDatastoreType, addIID, data, true);
    }

    protected void reload() {
        close();
        writeTransaction = broker.newWriteOnlyTransaction();
        isClosed = false;
    }

    protected void close() {
        if (isClosed) return;
        try {
            writeTransaction.submit().checkedGet();
        } catch (Exception e) {
            LOG.error("Error = {}", e.getMessage());
        }
        writeTransaction = null;
        isClosed = true;
        //futureProcessor.addCheckedFuture(writeTransaction.submit());
    }
}
