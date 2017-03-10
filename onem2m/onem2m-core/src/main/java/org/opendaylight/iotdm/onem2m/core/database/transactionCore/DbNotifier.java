/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.DbTransactions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.db.transactions.DbTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.db.transactions.DbTransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.db.transactions.DbTransactionKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DbNotifier is a layer of s/w that the ResoruceTreeWriter can call when a onem2mResource is created.  It feeds
 * the Onem2mDataTreeChangeListener.  Given that we have an mdsal database, or 3rd party plugin datastores,
 * a write to a 3rd party will not trigger the listener.  So, we implemented a DbTransaction table in the OPERATIONAL
 * datastore.  When the Writer creates, modifies, or deletes a resource, we q a DbTransaction to the DbNotifier
 * who in turn writes to the datastore.  The Onem2mDataTreeChangeListener "listens" to DbTransaction changes.
 * The Onem2mDataTreeChangeListener handles onem2mResource changes.  The DbTransaction also needs a way clean itself up.
 * So DbNotifier also listens to DbTransactions; it does this so that the "records" can be deleted/cleaned up.
 */
public class DbNotifier implements ClusteredDataTreeChangeListener<DbTransaction> {

    private final Logger LOG = LoggerFactory.getLogger(DbNotifier.class);
    private DataBroker dataBroker;
    private AtomicLong txId;
    private TransactionDequeHandler transactionDequeHandler;
    private TransactionDeleteHandler transactionDeleteHandler;

    private static final InstanceIdentifier<DbTransaction> IID =
            InstanceIdentifier.builder(DbTransactions.class)
                    .child(DbTransaction.class)
                    .build();
    private ListenerRegistration<DbNotifier> dcReg;
    private final LinkedBlockingQueue<DbTransaction> transactionQueue;
    private final LinkedBlockingQueue<Integer> deleteQueue;

    public DbNotifier(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        dcReg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, IID),
                this);
        transactionQueue = new LinkedBlockingQueue<DbTransaction>(1000);
        deleteQueue = new LinkedBlockingQueue<Integer>();
        txId = new AtomicLong(0);
        transactionDequeHandler = new TransactionDequeHandler();
        transactionDeleteHandler = new TransactionDeleteHandler();
        Thread transactionDequeHandlerThread = new Thread(transactionDequeHandler);
        transactionDequeHandlerThread.start();
        Thread transactionDeleteHandlerThread = new Thread(transactionDeleteHandler);
        transactionDeleteHandlerThread.start();
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<DbTransaction>> changes) {

        DbTransaction dbTransaction;

        for (DataTreeModification<DbTransaction> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case WRITE:
                    dbTransaction = change.getRootNode().getDataAfter();
                    enqueueDeleteOperation(dbTransaction.getDbTransactionId());
                    break;
            }
        }
    }

    public void enqueueDbOperation(DbTransaction.Operation opCode, String resourceId) {

        //LOG.info("enqueueDbOperation: opCode:{}, resId:{}", opCode, resourceId);

        try {
            DbTransactionKey key = new DbTransactionKey(txId.incrementAndGet());
            transactionQueue.put(new DbTransactionBuilder()
                    .setKey(key)
                    .setDbTransactionId(key.getDbTransactionId())
                    .setOperation(opCode)
                    .setResourceId(resourceId)
                    .build());
        } catch (Exception e) {
            LOG.error("enqueueDbOperation: interrupted: result in lost notifications: opCode:{}, resId:{}",
                    opCode, resourceId);
            LOG.error("{}", e.toString());
        }
    }

    private class TransactionDequeHandler implements Runnable {

        private TransactionDequeHandler() {};
        private WriteTransaction writeTransaction = null;
        private final Integer NUM_PUTS_PER_TXN = 20;

        @Override
        public void run() {

            Thread.currentThread().setName("db-ntfr-wrtr");

            int putCount = 0;
            WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
            DbTransaction dbt = null;

            while (true) {

                try {
                    dbt = transactionQueue.poll(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.error("{}", e.toString());
                    dbt = null;
                }

                if (dbt != null) {
                    DbTransactionKey txnKey = new DbTransactionKey(dbt.getKey());
                    InstanceIdentifier<DbTransaction> iid =
                            InstanceIdentifier.builder(DbTransactions.class)
                                    .child(DbTransaction.class, txnKey)
                                    .build();
                    wt.put(LogicalDatastoreType.OPERATIONAL, iid, dbt);
                    putCount++;
                }

                if ((dbt == null && putCount != 0) || putCount == NUM_PUTS_PER_TXN) {
//                    LOG.info("submitting: {} put transactions", putCount);
                    Futures.addCallback(wt.submit(), new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(final Void result) {
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            LOG.error("Transaction failed, {}", t);
                        }
                    });
                    wt = dataBroker.newWriteOnlyTransaction();
                    putCount = 0;
                }
            }
        }
    }

    public void enqueueDeleteOperation(long txId) {

        Integer id = Integer.valueOf((int)txId);
        try {
            deleteQueue.put(id);
        } catch (Exception e) {
            LOG.error("enqueueDbOperation: interrupted: result in lost notifications: id:{}",
                    id);
            LOG.error("{}", e);
        }
    }

    private class TransactionDeleteHandler implements Runnable {

        private TransactionDeleteHandler() {};
        private WriteTransaction writeTransaction = null;
        private final Integer NUM_DELS_PER_TXN = 20;

        @Override
        public void run() {

            Thread.currentThread().setName("db-ntfr-dltr");

            int delCount = 0;
            WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
            Integer txId = null;

            while (true) {

                try {
                    txId = deleteQueue.poll(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.error("{}", e.toString());
                    txId = null;
                }

                if (txId != null) {
                    DbTransactionKey txnKey = new DbTransactionKey(txId.longValue());
                    InstanceIdentifier<DbTransaction> iid =
                            InstanceIdentifier.builder(DbTransactions.class)
                                    .child(DbTransaction.class, txnKey)
                                    .build();
                    wt.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    delCount++;
                }

                if ((txId == null && delCount != 0) || delCount == NUM_DELS_PER_TXN) {
//                    LOG.info("submitting: {} delete transactions", delCount);
                    Futures.addCallback(wt.submit(), new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(final Void result) {
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            LOG.error("Transaction failed, {}", t);
                        }
                    });
                    wt = dataBroker.newWriteOnlyTransaction();
                    delCount = 0;
                }
            }
        }
    }
}
