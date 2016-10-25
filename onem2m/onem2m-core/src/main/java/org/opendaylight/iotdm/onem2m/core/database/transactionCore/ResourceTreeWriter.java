/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChildKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.OldestLatest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * This class contain static functions invoked by the Onem2mDb class.  They are turn invoke the data store API
 * class to interact with the data store.  The ResourceTreeWriter is responsible for the maintenance of the cse
 * entries in the cse list as well as the resource entries in the resource list.c
 */
public class ResourceTreeWriter implements Closeable {

    private final Logger LOG = LoggerFactory.getLogger(ResourceTreeWriter.class);
    private WriteOnlyCache cache;
    private ResourceTreeReader resourceTreeReader;
    private DaoResourceTreeWriter daoWriter;

    public ResourceTreeWriter(WriteOnlyCache cache, DaoResourceTreeWriter daoWriter, ResourceTreeReader resourceTreeReader) {
        this.cache = cache;
        this.resourceTreeReader = resourceTreeReader;
        this.daoWriter = daoWriter;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void close() {
        daoWriter.close();
    }


    /**
     * Add the cse to the db store using its name/resourceId
     *
     * @param name       cse name
     * @param resourceId id
     */
    public boolean createCseByName(String name, String resourceId) {
        if (!daoWriter.createCseByName(name, resourceId)) {
            LOG.error("createCseByName: DataStore could not write name = {}, resourceId = {}", name, resourceId);
            return false;
        }
        if (!cache.createCseByName(name, resourceId)) {
            LOG.error("createCseByName: Cache could not write name = {}, resourceId = {}", name, resourceId);
            return false;
        }
        return true;
    }

    /**
     * Add a resource to the data store
     *
     * @param onem2mRequest    request
     * @param parentResourceId id of the parent resource
     * @param resourceType     resourceType of the resource
     * @return this new resource
     */
    public Onem2mResource createResource(RequestPrimitive onem2mRequest,
                                         String parentResourceId, String resourceType) {
        if (!daoWriter.createResource(onem2mRequest, parentResourceId, resourceType)) {
            LOG.error("createResource: Could not create resource db");

            return null;
        }

        Onem2mResource ret = cache.createResource(onem2mRequest.getResourceId(), onem2mRequest.getResourceName(),
                onem2mRequest.getResourceContent().getInJsonContent().toString(),
                parentResourceId, resourceType);
        if (ret == null) {
            LOG.error("createResource: Could not create resource inRam");
            return null;
        }


        return ret;
    }

    /**
     * Update the pointers to the oldest and latest children
     *
     * @param resourceId the resource id
     * @param oldest     pointer to the tail
     * @param latest     pointer to the head
     * @return the resource
     */
    public boolean updateResourceOldestLatestInfo(String resourceId,
                                                   String resourceType,
                                                   String oldest,
                                                   String latest) {

        if (!daoWriter.updateResourceOldestLatestInfo(resourceId, resourceType, oldest, latest)) {
            LOG.error("updateResourceOldestLatestInfo: DB Could not write");
            return false;
        }

        cache.updateResourceOldestLatestInfo(resourceId, resourceType, oldest, latest);

        return true;
    }

//    /**
//     * Retrieve the attr by name from the data store
//     * @param resourceId resource id of the attr
//     * @param attrName name of attr
//     * @return Attr
//     */
//    public Attr retrieveAttrByName(String resourceId, String attrName) {
//
//        InstanceIdentifier<Attr> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
//                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId))
//                .child(Attr.class, new AttrKey(attrName));
//
//        return DbTransaction.retrieve(dataBroker, iid, LogicalDatastoreType.OPERATIONAL);
//    }
//
//    /**
//     * Delete the attr by name from the data store
//     * @param dbTxn transaction id
//     * @param resourceId this resource
//     * @param attrName name
//     */
//    public void deleteAttr(DbTransaction dbTxn, String resourceId, String attrName) {
//
//        InstanceIdentifier<Attr> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
//                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId))
//                .child(Attr.class, new AttrKey(attrName));
//
//        dbTxn.delete(iid, LogicalDatastoreType.OPERATIONAL);
//    }
//

    /**
     * @param resourceId          this resource
     * @param jsonResourceContent serailized JSON object
     */
    public boolean updateJsonResourceContentString(String resourceId, String jsonResourceContent) {
        if (!daoWriter.updateJsonResourceContentString(resourceId, jsonResourceContent)) {
            LOG.error("updateJsonResourceContentString: DB could not write");
            return false;
        }

        cache.updateJsonResourceContentString(resourceId, jsonResourceContent);

        return true;
    }

    /**
     * Link the parent resource to the child resource in the data store.
     *
     * @param parentResourceId parent
     * @param childName        name of child
     * @param childResourceId  child res id
     * @param prevId           pointer to prev sibling
     * @param nextId           pointer to next sibling
     */
    public boolean createParentChildLink(String parentResourceId,
                                          String childName, String childResourceId,
                                          String prevId, String nextId) {
        if (!daoWriter.createParentChildLink(parentResourceId, childName, childResourceId, prevId, nextId)) {
            LOG.error("createParentChildLink: DB could not write");
            return false;
        }

        return true;
    }

    /**
     * Removes resource in the list of children of the parent resource id
     * @param parentResourceId parent resource id
     * @param resourceType resource type
     * @param thisResourceId resource id to be removed
     * @param thisResourceName resource name to be removed in parent resource child list
     * @return
     */
    public boolean deleteResourceInReferences(String parentResourceId,
                                              String resourceType, String thisResourceId, String thisResourceName) {
        OldestLatest parentOldestLatest = resourceTreeReader.retrieveOldestLatestByResourceType(parentResourceId, resourceType);

        if (parentOldestLatest != null) {
            if (parentOldestLatest.getLatestId().equals(parentOldestLatest.getOldestId())) {

                // only child, set oldest/latest back to NULL
                updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        Onem2mDb.NULL_RESOURCE_ID,
                        Onem2mDb.NULL_RESOURCE_ID);

            } else if (parentOldestLatest.getLatestId().equals(thisResourceId)) {

                // deleting the latest, go back to prev and set is next to null, re point latest to prev
                Onem2mParentChild curr = resourceTreeReader.retrieveChildByName(parentResourceId, thisResourceName);
                String prevId = curr.getPrevId();
                Onem2mResource prevOnem2mResource = resourceTreeReader.retrieveResourceById(prevId);

                Onem2mParentChild child = resourceTreeReader.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());

                if (!updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        parentOldestLatest.getOldestId(),
                        prevId))
                    return false;
                if (!updateChildSiblingNextInfo(parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID)) return false;

            } else if (parentOldestLatest.getOldestId().equals(thisResourceId)) {

                // deleting the oldest, go to next and set its prev to null, re point oldest to next
                Onem2mParentChild curr = resourceTreeReader.retrieveChildByName(parentResourceId, thisResourceName);
                String nextId = curr.getNextId();
                Onem2mResource nextOnem2mResource = resourceTreeReader.retrieveResourceById(nextId);

                Onem2mParentChild child = resourceTreeReader.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());

                if (!updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        nextId,
                        parentOldestLatest.getLatestId()))
                    return false;
                if (!updateChildSiblingPrevInfo(parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID)) return false;

            } else {

                Onem2mParentChild curr = resourceTreeReader.retrieveChildByName(parentResourceId, thisResourceName);

                String nextId = curr.getNextId();
                Onem2mResource nextOnem2mResource = resourceTreeReader.retrieveResourceById(nextId);
                Onem2mParentChild prevChild = resourceTreeReader.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());


                String prevId = curr.getPrevId();
                Onem2mResource prevOnem2mResource = resourceTreeReader.retrieveResourceById(prevId);
                Onem2mParentChild nextChild = resourceTreeReader.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());


                if (!updateChildSiblingPrevInfo(parentResourceId, nextChild, Onem2mDb.NULL_RESOURCE_ID))
                    return false;
                if (!updateChildSiblingNextInfo(parentResourceId, prevChild, Onem2mDb.NULL_RESOURCE_ID))
                    return false;
            }
        }

        return true;
    }

    /**
     * Adds resource in the parent resource children list
     * @param resourceType resource type
     * @param parentId parent resource id
     * @param resourceId resource id
     * @param resourceName resource name in the parent children list
     * @return
     */
    public boolean initializeElementInParentList(String resourceType, String parentId, String resourceId, String resourceName) {
        String prevId = Onem2mDb.NULL_RESOURCE_ID;
        OldestLatest parentOldestLatest =
                resourceTreeReader.retrieveOldestLatestByResourceType(parentId, resourceType);
        if (parentOldestLatest != null) {
            String oldestId = parentOldestLatest.getOldestId();
            String latestId = parentOldestLatest.getLatestId();

            // need to maintain the oldest and latest, and next-prev children too
            if (latestId.equals(Onem2mDb.NULL_RESOURCE_ID)) {

                latestId = oldestId = resourceId;

                if (!updateResourceOldestLatestInfo(parentId, resourceType, oldestId, latestId))
                    return false;


            } else {

                prevId = latestId;
                Onem2mResource prevOnem2mResource = resourceTreeReader.retrieveResourceById(prevId);

                latestId = resourceId;

                Onem2mParentChild child = resourceTreeReader.retrieveChildByName(parentId, prevOnem2mResource.getName());

                if (!updateResourceOldestLatestInfo(parentId, resourceType, oldestId, latestId)) return false;
                if (!updateChildSiblingNextInfo(parentId, child, latestId)) return false;
            }
        }
        // create a childEntry on the parent resourceId, <child-name, child-resourceId>
        if (!createParentChildLink(parentId, // parent
                resourceName, // childName
                resourceId, // chileResourceId
                prevId, Onem2mDb.NULL_RESOURCE_ID)) // siblings
            return false;

        return true;
    }

    /**
     * Update the Next pointer
     *
     * @param parentResourceId the parent
     * @param child            the child
     * @param nextId           its next pointer
     */
    public boolean updateChildSiblingNextInfo(String parentResourceId,
                                              Onem2mParentChild child,
                                              String nextId) {
        if (!daoWriter.updateChildSiblingNextInfo(parentResourceId, child, nextId)) {
            LOG.error("updateChildSiblingNextInfo: DB could not write");
            return false;
        }

        return true;
    }

    /**
     * Update the prev pointer
     *
     * @param parentResourceId the parent
     * @param child            the child
     * @param prevId           prev pointer
     */
    public boolean updateChildSiblingPrevInfo(String parentResourceId,
                                              Onem2mParentChild child,
                                              String prevId) {

        if (!daoWriter.updateChildSiblingPrevInfo(parentResourceId, child, prevId)) {
            LOG.error("updateChildSiblingPrevInfo: DB could not write");
            return false;
        }

        return true;
    }

    /**
     * Unlink the child resource from the parent resource
     *
     * @param parentResourceId  the parent
     * @param childResourceName child name
     */
    public boolean removeParentChildLink(String parentResourceId, String childResourceName) {
        if (!daoWriter.removeParentChildLink(parentResourceId, childResourceName)) {
            LOG.error("removeParentChildLink: DB could not write");
            return false;
        }

        return true;
    }


    /**
     * Delete the resource using its id
     *
     * @param resourceId the resource id
     */
    public boolean deleteResourceById(String resourceId) {
        if (!daoWriter.deleteResourceById(resourceId)) {
            LOG.error("deleteResourceById: DB could not write");
            return false;
        }
        cache.deleteResourceById(resourceId);

        return true;
    }

    // TODO: migrate the routing table from Onem2mRouterService into the cache

    public boolean createAeUnderCse(String cseBaseName,
                                    String aeId, String aeResourceId) {
        if (!daoWriter.createAeIdToResourceIdMapping(cseBaseName, aeId, aeResourceId)) {
            LOG.error("createAeIdToResourceIdMapping: DB could not write");
            return false;
        }

        // TODO: add caching
        return true;
    }

    public boolean deleteAeIdToResourceIdMapping(String cseBaseName, String aeId) {
        if (!daoWriter.deleteAeIdToResourceIdMapping(cseBaseName, aeId)) {
            LOG.error("deleteAeIdToResourceIdMapping: DB could not write");
            return false;
        }

        // TODO: add caching
        return true;
    }

    /**
     * Cleanup the data store.
     */
    public void reInitializeDatastore() {
        LOG.info("reInitializeDatastore");
        daoWriter.reInitializeDatastore();
        cache.reInitializeDatastore();
    }


}