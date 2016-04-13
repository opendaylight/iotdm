/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Onem2mDatastoreListener implements ClusteredDataTreeChangeListener<Onem2mResource> {

    private final Logger LOG = LoggerFactory.getLogger(Onem2mDatastoreListener.class);

    private static final InstanceIdentifier<Onem2mResource> RESOURCE_IID =
            InstanceIdentifier.builder(Onem2mResourceTree.class)
                    .child(Onem2mResource.class).build();
    private ListenerRegistration<Onem2mDatastoreListener> dcReg;
    private DataBroker dataBroker;

    public Onem2mDatastoreListener(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        dcReg = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                RESOURCE_IID), this);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Onem2mResource>> changes) {

        String onem2mResourceId;

        for (DataTreeModification<Onem2mResource> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case WRITE:
                    onem2mResourceId = change.getRootNode().getDataAfter().getResourceId();
                    onem2mResourceCreated(onem2mResourceId);
                    break;
                case SUBTREE_MODIFIED:
                    String beforeJsonResourceContent = change.getRootNode().getDataBefore().getResourceContentJsonString();
                    String afterJsonResourceContent = change.getRootNode().getDataAfter().getResourceContentJsonString();
                    if (beforeJsonResourceContent == null) {
                        if (afterJsonResourceContent != null) {
                            onem2mResourceId = change.getRootNode().getDataAfter().getResourceId();
                            onem2mResourceChanged(onem2mResourceId);
                        }
                    } else {
                        if (afterJsonResourceContent == null || !beforeJsonResourceContent.contentEquals(afterJsonResourceContent)) {
                            onem2mResourceId = change.getRootNode().getDataAfter().getResourceId();
                            onem2mResourceChanged(onem2mResourceId);
                        }
                    }
                    break;
                case DELETE:
                    Onem2mResource onem2mResource = change.getRootNode().getDataBefore();
                    onem2mResourceDeleted(onem2mResource);
                    break;
                default:
                    LOG.error("Onem2mDatastoreListener: onDataTreeChanged(Onem2mResource) non handled modification {}",
                            change.getRootNode().getModificationType());
                    break;
            }
        }
    }

    private void onem2mResourceCreated(String onem2mResourceId) {

        String h = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResourceId);
        LOG.info("onem2mResourceCreated: {} ...", h);
        onem2mResourceCreated(h, Onem2mDb.getInstance().getResource(onem2mResourceId));
    }

    private void onem2mResourceChanged(String onem2mResourceId) {

        String h = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResourceId);
        LOG.info("onem2mResourceChanged: {} ...", h);
        onem2mResourceChanged(h, Onem2mDb.getInstance().getResource(onem2mResourceId));
    }

    private void onem2mResourceDeleted(Onem2mResource onem2mResource) {

        String h = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource);
        LOG.info("onem2mResourceDeleted: {} ...", h);
        onem2mResourceDeleted(h, onem2mResource);
    }

    public abstract void onem2mResourceCreated(String hName, Onem2mResource onem2mResource);
    public abstract void onem2mResourceChanged(String hName, Onem2mResource onem2mResource);
    public abstract void onem2mResourceDeleted(String hName, Onem2mResource onem2mResource);

}