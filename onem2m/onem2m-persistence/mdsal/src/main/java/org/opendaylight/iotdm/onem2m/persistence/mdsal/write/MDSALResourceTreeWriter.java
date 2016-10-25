/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.persistence.mdsal.write;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .Onem2mParentChildList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .Onem2mParentChildListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .Onem2mParentChildListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.parent.child.list.Onem2mParentChildBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChildKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredAeIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredAeIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredAeIdsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by gguliash on 5/20/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class MDSALResourceTreeWriter implements DaoResourceTreeWriter {
    private final Logger LOG = LoggerFactory.getLogger(MDSALResourceTreeWriter.class);
    private MDSALTransactionWriter writer;

    public MDSALResourceTreeWriter(MDSALTransactionWriter writer) {
        this.writer = writer;
    }

    public void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public boolean createCseByName(String name, String resourceId) {
        try {
            writer.reload();
            Onem2mCse onem2mCse = new Onem2mCseBuilder()
                    .setKey(new Onem2mCseKey(name))
                    .setName(name)
                    .setResourceId(resourceId)
                    .build();

            Onem2mCseKey key = onem2mCse.getKey();
            InstanceIdentifier<Onem2mCse> iid = InstanceIdentifier.create(Onem2mCseList.class)
                    .child(Onem2mCse.class, key);

            writer.create(iid, onem2mCse, LogicalDatastoreType.OPERATIONAL);

            return true;
        } catch (Exception e) {
            LOG.error("exception : {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

    }

    @Override
    public boolean createResource(RequestPrimitive onem2mRequest, String parentResourceId, String resourceType) {
        try {
            writer.reload();

            /**
             * Initialize empty oldestlatest lists for contentInstances, and subscriptions as these are the two sets
             * of lists that we need quick access to when processing requests for container/latest and notifications
             * These act like the head/tail of a linked list where the key of the list is the resource type
             */
            List<OldestLatest> oldestLatestList = new ArrayList<OldestLatest>();

            OldestLatest oldestLatestContentInstance = new OldestLatestBuilder()
                    .setKey(new OldestLatestKey(Onem2m.ResourceType.CONTENT_INSTANCE))
                    .setResourceType(Onem2m.ResourceType.CONTENT_INSTANCE)
                    .setLatestId(Onem2mDb.NULL_RESOURCE_ID)
                    .setOldestId(Onem2mDb.NULL_RESOURCE_ID)
                    .build();
            oldestLatestList.add(oldestLatestContentInstance);

            OldestLatest oldestLatestSubscription = new OldestLatestBuilder()
                    .setKey(new OldestLatestKey(Onem2m.ResourceType.SUBSCRIPTION))
                    .setResourceType(Onem2m.ResourceType.SUBSCRIPTION)
                    .setLatestId(Onem2mDb.NULL_RESOURCE_ID)
                    .setOldestId(Onem2mDb.NULL_RESOURCE_ID)
                    .build();
            oldestLatestList.add(oldestLatestSubscription);

            /**
             * Initialize the resource
             */
            Onem2mResourceKey key = new Onem2mResourceKey(onem2mRequest.getResourceId());
            String jsonContent = onem2mRequest.getResourceContent().getInJsonContent().toString();
            Onem2mResource onem2mResource = new Onem2mResourceBuilder()
                    .setKey(key)
                    .setResourceId(onem2mRequest.getResourceId())
                    .setName(onem2mRequest.getResourceName())
                    .setResourceType(resourceType)
                    .setParentId(parentResourceId) // parent resource
                    .setOldestLatest(oldestLatestList)
                    //.setChild(Collections.<Child>emptyList()) // moved to new parallel container
                    .setResourceContentJsonString(jsonContent)
                    .build();

            InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mResource.class, onem2mResource.getKey());


            writer.create(iid, onem2mResource, LogicalDatastoreType.OPERATIONAL);

            Onem2mParentChildListKey parentChildListKey = new Onem2mParentChildListKey(onem2mRequest.getResourceId());
            Onem2mParentChildList onem2mParentChildList = new Onem2mParentChildListBuilder()
                    .setKey(parentChildListKey)
                    .setParentResourceId(onem2mRequest.getResourceId())
                    .setOnem2mParentChild(Collections.<Onem2mParentChild>emptyList()) // new resource has NO children
                    .build();

            InstanceIdentifier<Onem2mParentChildList> pciid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, onem2mParentChildList.getKey());

            writer.create(pciid, onem2mParentChildList, LogicalDatastoreType.OPERATIONAL);

        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;
    }

    @Override
    public boolean updateResourceOldestLatestInfo(String resourceId, String resourceType, String oldest, String latest) {
        try {
            writer.reload();

            OldestLatest oldestlatest = new OldestLatestBuilder()
                    .setKey(new OldestLatestKey(resourceType))
                    .setResourceType(resourceType)
                    .setLatestId(latest)
                    .setOldestId(oldest)
                    .build();

            Onem2mResourceKey key = new Onem2mResourceKey(resourceId);
            OldestLatestKey oldestKey = oldestlatest.getKey();
            InstanceIdentifier<OldestLatest> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mResource.class, key)
                    .child(OldestLatest.class, oldestKey);

            writer.update(iid, oldestlatest, LogicalDatastoreType.OPERATIONAL);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }
        return true;
    }

    @Override
    public boolean updateJsonResourceContentString(String resourceId, String jsonResourceContent) {
        try {
            writer.reload();

            Onem2mResourceKey key = new Onem2mResourceKey(resourceId);
            Onem2mResource onem2mResource = new Onem2mResourceBuilder()
                    .setKey(key)
                    .setResourceContentJsonString(jsonResourceContent)
                    .build();

            InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mResource.class, onem2mResource.getKey());

            writer.update(iid, onem2mResource, LogicalDatastoreType.OPERATIONAL);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;
    }

    @Override
    public boolean createParentChildLink(String parentResourceId, String childName, String childResourceId, String prevId, String nextId) {
        try {
            writer.reload();

            Onem2mParentChild onem2mParentChild = new Onem2mParentChildBuilder()
                    .setKey(new Onem2mParentChildKey(childName))
                    .setName(childName)
                    .setResourceId(childResourceId)
                    .setNextId(nextId)
                    .setPrevId(prevId)
                    .build();

            InstanceIdentifier<Onem2mParentChild> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, new Onem2mParentChildListKey(parentResourceId))
                    .child(Onem2mParentChild.class, onem2mParentChild.getKey());

            writer.create(iid, onem2mParentChild, LogicalDatastoreType.OPERATIONAL);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;
    }

    @Override
    public boolean updateChildSiblingNextInfo(String parentResourceId, Onem2mParentChild child, String
            nextId) {
        try {
            writer.reload();

            Onem2mParentChild updateChild = new Onem2mParentChildBuilder(child)
                    .setNextId(nextId)
                    .build();

            InstanceIdentifier<Onem2mParentChild> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, new Onem2mParentChildListKey(parentResourceId))
                    .child(Onem2mParentChild.class, updateChild.getKey());

            writer.update(iid, updateChild, LogicalDatastoreType.OPERATIONAL);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;
    }

    @Override
    public boolean updateChildSiblingPrevInfo(String parentResourceId,
                                              Onem2mParentChild child,
                                              String prevId) {
        try {
            writer.reload();

            Onem2mParentChild updateChild = new Onem2mParentChildBuilder(child)
                    .setPrevId(prevId)
                    .build();

            InstanceIdentifier<Onem2mParentChild> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, new Onem2mParentChildListKey(parentResourceId))
                    .child(Onem2mParentChild.class, updateChild.getKey());

            writer.update(iid, updateChild, LogicalDatastoreType.OPERATIONAL);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;

    }

    @Override
    public boolean removeParentChildLink(String parentResourceId, String childResourceName) {

        try {
            writer.reload();

            Onem2mParentChildKey childKey = new Onem2mParentChildKey(childResourceName);
            InstanceIdentifier<Onem2mParentChild> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, new Onem2mParentChildListKey(parentResourceId))
                    .child(Onem2mParentChild.class, childKey);

            writer.delete(iid, LogicalDatastoreType.OPERATIONAL);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;
    }

    @Override
    public boolean deleteResourceById(String resourceId) {
        try {
            writer.reload();

            Onem2mResourceKey key = new Onem2mResourceKey(resourceId);
            InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mResource.class, key);


            writer.delete(iid, LogicalDatastoreType.OPERATIONAL);

            Onem2mParentChildListKey parentChildListKey = new Onem2mParentChildListKey(resourceId);

            InstanceIdentifier<Onem2mParentChildList> pciid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, parentChildListKey);

            writer.delete(pciid, LogicalDatastoreType.OPERATIONAL);

        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;
    }

    @Override
    public boolean createAeIdToResourceIdMapping(String cseBaseName,
                                                 String aeId, String aeResourceId) {
        try {
            writer.reload();

            Onem2mRegisteredAeIds registeredAe = new Onem2mRegisteredAeIdsBuilder()
                                                         .setKey(new Onem2mRegisteredAeIdsKey(aeId))
                                                         .setRegisteredAeId(aeId)
                                                         .setResourceId(aeResourceId)
                                                         .build();

            InstanceIdentifier<Onem2mRegisteredAeIds> iid =
                    InstanceIdentifier.create(Onem2mCseList.class)
                            .child(Onem2mCse.class, new Onem2mCseKey(cseBaseName))
                            .child(Onem2mRegisteredAeIds.class, registeredAe.getKey());

            writer.create(iid, registeredAe, LogicalDatastoreType.OPERATIONAL);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;
    }

    @Override
    public boolean deleteAeIdToResourceIdMapping(String cseBaseName, String aeId) {
        try {
            writer.reload();
            InstanceIdentifier<Onem2mRegisteredAeIds> iid =
                    InstanceIdentifier.create(Onem2mCseList.class)
                        .child(Onem2mCse.class, new Onem2mCseKey(cseBaseName))
                        .child(Onem2mRegisteredAeIds.class, new Onem2mRegisteredAeIdsKey(aeId));

            writer.delete(iid, LogicalDatastoreType.OPERATIONAL);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            return false;
        } finally {
            writer.close();
        }

        return true;
    }

    @Override
    public void reInitializeDatastore() {
        writer.reload();

        // overwrite the cseList with empty info
        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();
        Onem2mCseList cseList = new Onem2mCseListBuilder().setOnem2mCse(Collections.<Onem2mCse>emptyList()).build();

        // overwrite the resource tree with empty information
        InstanceIdentifier<Onem2mResourceTree> iidTreeList = InstanceIdentifier.builder(Onem2mResourceTree.class).build();
        Onem2mResourceTree tree = new Onem2mResourceTreeBuilder().setOnem2mResource(Collections.<Onem2mResource>emptyList()).build();

        writer.create(iidCseList, cseList, LogicalDatastoreType.OPERATIONAL);
        writer.create(iidTreeList, tree, LogicalDatastoreType.OPERATIONAL);
        writer.close();

    }

    @Override
    public void close() {
        writer.close();
    }
}
