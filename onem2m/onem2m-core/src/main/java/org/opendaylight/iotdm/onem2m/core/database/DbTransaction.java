/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the transactional capabilities for the onem2m resource tree data store.  Transaction chaining
 * is used to increase data store throughput.  The users of this class needs to:
 *
 * 1) start a transaction eg. DbTransaction txn = new DbTransaction(dataBroker btc)
 * 2) perform the db operations on some portion of the IMDS, eg txn.delete(iid, LogicalDatastoreType.OPERATIONAL)
 * 3) perform more db opoerations
 * 4) commit all the db ooerations as a group, eg. txn.commitTransaction()
 */
public class DbTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(DbTransaction.class);
    private DataBroker dataBroker;
    private WriteTransaction writeTx;

    /**
     * Create a txn, it requires the chain as it uses the transaction chaining feature of the data store
     * @param dataBroker transaction chain
     */
    public DbTransaction(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.writeTx = dataBroker.newWriteOnlyTransaction();
    }

    /**
     * Commit the transaction to the data store
     * @return success/fail
     */
    public boolean commitTransaction() {
        boolean ret;

        try {
            writeTx.submit().checkedGet();
            ret = true;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed: {}", e.toString());
            ret = false;
        }
        return ret;
    }

    /**
     * Delete
     * @param deleteIID iid
     * @param logicalDatastoreType op vs config
     * @param <U> return value
     */
    public <U extends org.opendaylight.yangtools.yang.binding.DataObject> void delete
            (InstanceIdentifier<U> deleteIID, LogicalDatastoreType logicalDatastoreType) {
        writeTx.delete(logicalDatastoreType, deleteIID);
    }

    /**
     * Update
     * @param addIID iid
     * @param data new value
     * @param logicalDatastoreType op vs config
     * @param <U> return value
     */
    public <U extends org.opendaylight.yangtools.yang.binding.DataObject> void update // writeMerge
            (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        writeTx.merge(logicalDatastoreType, addIID, data, true);
    }

    /**
     * Create/Write
     * @param addIID iid
     * @param data new data
     * @param logicalDatastoreType op vs config
     * @param <U> return value
     */
    public <U extends org.opendaylight.yangtools.yang.binding.DataObject> void create // writePut
            (InstanceIdentifier<U> addIID, U data, LogicalDatastoreType logicalDatastoreType) {
        writeTx.put(logicalDatastoreType, addIID, data, true);
    }

    /**
     * This is a static routine that is a complete transaction for reading.  It is not really apart of the
     * DbTransaction where a sequence of open, trans, {db ops}, followed by db.commit is required.   We use that
     * only for changing the db.
     * @param dataBroker transaction chain
     * @param readIID iid
     * @param logicalDatastoreType op vs config
     * @param <U> return type
     * @return return date
     */
    public static <U extends org.opendaylight.yangtools.yang.binding.DataObject> U retrieve (
            DataBroker dataBroker,
            InstanceIdentifier<U> readIID,
            LogicalDatastoreType logicalDatastoreType) {

        U ret = null;
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        Optional<U> optionalDataObject;
        CheckedFuture<Optional<U>, ReadFailedException> submitFuture = readTx.read(logicalDatastoreType, readIID);
        try {
            optionalDataObject = submitFuture.checkedGet();
            if (optionalDataObject != null && optionalDataObject.isPresent()) {
                ret = optionalDataObject.get();
            }
        } catch (ReadFailedException e) {
            LOG.warn("failed to ....", e);
        } finally {
            readTx.close();
        }
        return ret;
    }
}