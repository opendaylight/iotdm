/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.dbapi;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.DbTransactions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.db.transactions.DbTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Onem2mDatastoreListener implements ClusteredDataTreeChangeListener<DbTransaction> {

    private final Logger LOG = LoggerFactory.getLogger(Onem2mDatastoreListener.class);

    private static final InstanceIdentifier<DbTransaction> IID =
            InstanceIdentifier.builder(DbTransactions.class)
                    .child(DbTransaction.class)
                    .build();
    private ListenerRegistration<Onem2mDatastoreListener> dcReg;
    private DataBroker dataBroker;


    public Onem2mDatastoreListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        dcReg = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                IID), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<DbTransaction>> changes) {

        DbTransaction dbTransaction;

        for (DataTreeModification<DbTransaction> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case WRITE:
                    dbTransaction = change.getRootNode().getDataAfter();
                    dbTransactionHandler(dbTransaction);
                    break;
            }
        }
    }

    private void dbTransactionHandler(DbTransaction dbt) {
        Onem2mResource onem2mResource = Onem2mDb.getInstance().getResource(dbt.getResourceId());
        if (onem2mResource != null) {
            switch (dbt.getOperation()) {
                case CREATE:
                    onem2mResourceCreated(onem2mResource);
                    break;
                case UPDATE:
                    onem2mResourceChanged(onem2mResource);
                    break;
                case DELETE:
                    onem2mResourceDeleted(onem2mResource);
                    break;
            }
        } else {
            LOG.warn("dbTransactionHandler: cannot find onem2mResource for: {}", dbt.getResourceId());
        }
    }
    private void onem2mResourceCreated(Onem2mResource onem2mResource) {
        onem2mResourceCreated(onem2mResource.getParentTargetUri(), onem2mResource);
    }

    private void onem2mResourceChanged(Onem2mResource onem2mResource) {
        onem2mResourceChanged(onem2mResource.getParentTargetUri(), onem2mResource);
    }

    private void onem2mResourceDeleted(Onem2mResource onem2mResource) {
        onem2mResourceDeleted(onem2mResource.getParentTargetUri(), onem2mResource);
    }

    public abstract void onem2mResourceCreated(String hName, Onem2mResource onem2mResource);
    public abstract void onem2mResourceChanged(String hName, Onem2mResource onem2mResource);
    public abstract void onem2mResourceDeleted(String hName, Onem2mResource onem2mResource);

}
