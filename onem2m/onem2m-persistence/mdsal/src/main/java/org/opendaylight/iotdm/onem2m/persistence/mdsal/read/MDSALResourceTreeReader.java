/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.persistence.mdsal.read;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.Onem2mResourceElem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gguliash on 5/20/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class MDSALResourceTreeReader implements DaoResourceTreeReader {
    private final Logger LOG = LoggerFactory.getLogger(MDSALResourceTreeReader.class);
    private final DataBroker broker;


    public MDSALResourceTreeReader(DataBroker broker) {
        this.broker = broker;
    }

    @Override
    public Onem2mCse retrieveCseByName(Onem2mCseKey key) {
        InstanceIdentifier<Onem2mCse> iid = InstanceIdentifier.create(Onem2mCseList.class)
                .child(Onem2mCse.class, key);
        return retrieve(iid, LogicalDatastoreType.OPERATIONAL);
    }

    private Onem2mResource retrieveFullResourceById(Onem2mResourceKey key) {
        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, key);
        return retrieve(iid, LogicalDatastoreType.OPERATIONAL);

    }

    @Override
    public Onem2mResourceElem retrieveResourceById(Onem2mResourceKey key) {
        Onem2mResource resource = retrieveFullResourceById(key);
        if (resource == null) return null;
        else
            return new Onem2mResourceElem(this, resource.getResourceId(), resource.getParentId(), resource.getName(), resource.getResourceType(),
                    resource.getOldestLatest(), resource.getChild(), resource.getResourceContentJsonString());
    }

    @Override
    public Onem2mCseList retrieveFullCseList() {
        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();
        return retrieve(iidCseList, LogicalDatastoreType.OPERATIONAL);
    }

    @Override
    public Onem2mResourceTree retrieveFullResourceList() {
        InstanceIdentifier<Onem2mResourceTree> iidTree = InstanceIdentifier.builder(Onem2mResourceTree.class).build();
        return retrieve(iidTree, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * This is a routine that is a complete transaction for reading.  It is not really apart of the
     * DbTransaction where a sequence of open, trans, {db ops}, followed by db.commit is required.   We use that
     * only for changing the db.
     *
     * @param readIID              iid
     * @param logicalDatastoreType op vs config
     * @param <U>                  return type
     * @return return date
     */
    private <U extends org.opendaylight.yangtools.yang.binding.DataObject> U retrieve(
            InstanceIdentifier<U> readIID,
            LogicalDatastoreType logicalDatastoreType) {

        try (ReadOnlyTransaction readOnlyTransaction = broker.newReadOnlyTransaction()) {
            U ret = null;
            Optional<U> optionalDataObject;
            CheckedFuture<Optional<U>, ReadFailedException> submitFuture = readOnlyTransaction.read(logicalDatastoreType, readIID);
            try {
                optionalDataObject = submitFuture.checkedGet();
                if (optionalDataObject != null && optionalDataObject.isPresent()) {
                    ret = optionalDataObject.get();
                }
            } catch (ReadFailedException e) {
                LOG.warn("failed to ....", e);
            }
            return ret;
        }
    }


}
