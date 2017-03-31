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
import java.util.ArrayList;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.Onem2mResourceElem;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.MDSALDaoResourceTreeFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.IotdmSpecificOperationalData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredAeIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredAeIdsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mParentChildList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .Onem2mParentChildListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.parent.child.list.Onem2mParentChildKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by gguliash on 5/20/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class MDSALResourceTreeReader implements DaoResourceTreeReader {
    private final Logger LOG = LoggerFactory.getLogger(MDSALResourceTreeReader.class);
    private final DataBroker broker;
    private LogicalDatastoreType dsType = LogicalDatastoreType.CONFIGURATION;
    private MDSALDaoResourceTreeFactory factory;

    public MDSALResourceTreeReader(MDSALDaoResourceTreeFactory factory, DataBroker broker) {
        this.broker = broker;
        this.factory = factory;
    }

    @Override
    public Onem2mCse retrieveCseByName(Onem2mCseKey key) {
        InstanceIdentifier<Onem2mCse> iid = InstanceIdentifier.create(Onem2mCseList.class)
                .child(Onem2mCse.class, key);
        return retrieve(iid, dsType);
    }

    private Onem2mResource retrieveFullResourceById(Onem2mResourceKey key) {
        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, key);
        return retrieve(iid, dsType);
    }

    @Override
    public Onem2mResourceElem retrieveResourceById(Onem2mResourceKey key) {
        Onem2mResource resource = retrieveFullResourceById(key);
        if (resource == null) return null;
        else
            return new Onem2mResourceElem(this, resource.getResourceId(), resource.getParentId(), resource.getName(), resource.getResourceType(),
                    resource.getResourceContentJsonString(), resource.getParentTargetUri());
    }

    @Override
    public List<Onem2mParentChild> retrieveParentChildList(Onem2mParentChildListKey key) {
        InstanceIdentifier<Onem2mParentChildList> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mParentChildList.class, key);

        Onem2mParentChildList list = retrieve(iid, dsType);
        if (null == list) {
            return null;
        }
        return list.getOnem2mParentChild();
    }

    @Override
    public List<Onem2mParentChild> retrieveParentChildListLimitN(Onem2mParentChildListKey key, int limit) {

        List<Onem2mParentChild> returnList = new ArrayList();

        if (limit <= 0) return returnList;

        InstanceIdentifier<Onem2mParentChildList> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mParentChildList.class, key);

        Onem2mParentChildList list = retrieve(iid, dsType);

        if (list != null) {
            int numElements = 0;
            for (Onem2mParentChild child : list.getOnem2mParentChild()) {
                returnList.add(child);
                if (++numElements == limit) break;
            }
        }

        return returnList;
    }

    @Override
    public Onem2mParentChild retrieveChildByName(String resourceId, String name) {
        InstanceIdentifier<Onem2mParentChild> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mParentChildList.class, new Onem2mParentChildListKey(resourceId))
                .child(Onem2mParentChild.class, new Onem2mParentChildKey(name));

        return retrieve(iid, dsType);
    }


    @Override
    public Onem2mCseList retrieveFullCseList() {
        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();
        return retrieve(iidCseList, dsType);
    }

    @Override
    public Onem2mResourceTree retrieveFullResourceList() {
        InstanceIdentifier<Onem2mResourceTree> iidTree = InstanceIdentifier.builder(Onem2mResourceTree.class).build();
        return retrieve(iidTree, dsType);
    }

    /**
     * Retrieve the resourceID of the AE by its AE-ID.
     * @param cseBaseName The name of cseBase.
     * @param aeId The AE-ID of the AE.
     * @return resourceID of the AE
     */
    @Override
    public String retrieveAeResourceIdByAeId(String cseBaseName, String aeId) {
        InstanceIdentifier<Onem2mRegisteredAeIds> iid = InstanceIdentifier.create(Onem2mCseList.class)
                                                                .child(Onem2mCse.class, new Onem2mCseKey(cseBaseName))
                                                                .child(Onem2mRegisteredAeIds.class, new Onem2mRegisteredAeIdsKey(aeId));

        Onem2mRegisteredAeIds aeIds = retrieve(iid, dsType);

        if (null == aeIds) {
            LOG.trace("Failed to retrieve AE-ID to resourceID mapping for cseBaseName: {}, AE-ID: {}",
                      cseBaseName, aeId);
            return null;
        }

        return aeIds.getResourceId();
    }

    private Integer isEntityRegisteredAtCseBase(final String entityId, final String cseBaseCseId) {
        /* TODO need to implement some CSE-ID to resource ID mapping */

        if (null != this.retrieveAeResourceIdByAeId(cseBaseCseId, entityId)) {
            // Entity is registered as AE
            return Onem2m.ResourceType.AE;
        }

        // Entity is not registered
        return null;
    }

    @Override
    public Integer isEntityRegistered(String entityId, String cseBaseCseId) {

        if (null != cseBaseCseId) {
            return isEntityRegisteredAtCseBase(entityId, cseBaseCseId);
        }

        // retrieve list of cseBase resources because the cseBaseCseId is not specified
        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();
        Onem2mCseList cseList = retrieve(iidCseList, dsType);
        if (cseList == null) return null;
        List<Onem2mCse> onem2mCseList = cseList.getOnem2mCse();

        // walk the list of cseBase resources and try to find AE or CSE with given ID
        for (Onem2mCse cseBase : onem2mCseList) {

            Onem2mResource cseBaseResource = this.retrieveResourceById(new Onem2mResourceKey(cseBase.getResourceId()));
            if (null == cseBaseResource) {
                LOG.error("Failed to retrieve cseBase resource");
                continue;
            }

            // CSE-ID and name are the same in our implementation
            cseBaseCseId = cseBase.getName();
            return isEntityRegisteredAtCseBase(entityId, cseBaseCseId);
        }

        return null;
    }

    @Override
    public int retrieveSystemStartId() {
        InstanceIdentifier<IotdmSpecificOperationalData> iid =
            InstanceIdentifier.builder(IotdmSpecificOperationalData.class).build();

        IotdmSpecificOperationalData operData = retrieve(iid, dsType);

        if (null == operData || null == operData.getSystemStartId()) {
            return 0;
        }

        return operData.getSystemStartId().intValue();
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

    @Override
    public void close() {

    }

}
