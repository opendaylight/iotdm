/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.db.transactions.DbTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
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
    private DbNotifier dbNotifier;

    public ResourceTreeWriter(WriteOnlyCache cache, DaoResourceTreeWriter daoWriter, ResourceTreeReader resourceTreeReader, DbNotifier dbNotifier) {
        this.cache = cache;
        this.resourceTreeReader = resourceTreeReader;
        this.daoWriter = daoWriter;
        this.dbNotifier = dbNotifier;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void close() {
        try {
            daoWriter.close();
        } catch (Exception e) {
            LOG.error("Failed to close DAO ResourceTreeWriter: {}", e);
        }
    }

    public String generateResourceId(String parentResourceId, Integer resourceType, Integer
            iotdmInstance) {
        return daoWriter.generateResourceId(parentResourceId, resourceType, iotdmInstance);
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

    public Object startWriteTransaction() {
        return daoWriter.startTransaction();
    }

    public boolean endWriteTransaction(Object transaction) {
        return daoWriter.endTransaction(transaction);
    }

    /**
     * Add a resource to the data store
     *
     * @param onem2mRequest    request
     * @param parentResourceId id of the parent resource
     * @param resourceType     resourceType of the resource
     * @return this new resource
     */
    public Onem2mResource createResource(Object transaction, RequestPrimitive onem2mRequest,
                                         String parentResourceId, Integer resourceType) {
        if (!daoWriter.createResource(transaction, onem2mRequest, parentResourceId, resourceType)) {
            LOG.error("createResource: Could not create resource db");

            return null;
        }

        Onem2mResource ret = cache.createResource(
                onem2mRequest.getResourceId(),
                onem2mRequest.getResourceName(),
                onem2mRequest.getJsonResourceContentString(),
                parentResourceId,
                resourceType,
                onem2mRequest.getParentTargetUri());
        if (ret == null) {
            LOG.error("createResource: Could not create resource inRam");
            return null;
        }

        dbNotifier.enqueueDbOperation(DbTransaction.Operation.CREATE, onem2mRequest.getResourceId());

        return ret;
    }

    /**
     * @param resourceId          this resource
     * @param jsonResourceContent serialized JSON object
     */
    public boolean updateJsonResourceContentString(Object transaction, String resourceId, String jsonResourceContent) {
        if (!daoWriter.updateJsonResourceContentString(transaction, resourceId, jsonResourceContent)) {
            LOG.error("updateJsonResourceContentString: DB could not write");
            return false;
        }

        cache.updateJsonResourceContentString(resourceId, jsonResourceContent);

        dbNotifier.enqueueDbOperation(DbTransaction.Operation.UPDATE, resourceId);

        return true;
    }

    /**
     * Move from old parent to parent id 1 (the delete parent)
     *
     */
    public boolean moveParentChildLinkToDeleteParent(String oldParentResourceId, String childResourceName, String childResourceId) {

        if (!daoWriter.moveParentChildLinkToDeleteParent(childResourceId, oldParentResourceId, childResourceName,
                Onem2m.SYS_DELETE_RESOURCE_ID)) {
            LOG.error("moveParentChildLinkToDeleteParent: DB could not perform operation: resourceId:{}", childResourceId);
            return false;
        }


        return true;
    }

    /**
     * Delete the resource using its id
     *
     * @param resourceId the resource id
     */
    public boolean deleteResource(Object transaction, String resourceId, String parentResourceId, String resourceName) {
        if (!daoWriter.deleteResource(transaction, resourceId, parentResourceId, resourceName)) {
            LOG.error("deleteResource: DB could not delete");
            return false;
        }
        cache.deleteResourceById(resourceId);

        return true;
    }

    public boolean createAeUnderCse(String cseBaseName,
                                    String aeId, String aeResourceId) {
        if (!daoWriter.createAeIdToResourceIdMapping(cseBaseName, aeId, aeResourceId)) {
            LOG.error("createAeIdToResourceIdMapping: DB could not write");
            return false;
        }

        return cache.createAeResourceIdByAeId(cseBaseName, aeId, aeResourceId);
    }

    public boolean deleteAeIdToResourceIdMapping(String cseBaseName, String aeId) {
        if (!daoWriter.deleteAeIdToResourceIdMapping(cseBaseName, aeId)) {
            LOG.error("deleteAeIdToResourceIdMapping: DB could not write");
            return false;
        }

        cache.deleteAeResourceIdByAeId(cseBaseName, aeId);
        return true;
    }

    // TODO: migrate the routing table from Onem2mRouterService into the cache

    public boolean createRemoteCseUnderCse(String cseBaseName, String remoteCseCseId, String remoteCseResourceId) {
        if (!daoWriter.createRemoteCseIdToResourceIdMapping(cseBaseName, remoteCseCseId, remoteCseResourceId)) {
            LOG.error("createRemoteCseIdToResourceIdMapping: DB could not write");
            return false;
        }

        // TODO: add caching
        return true;
    }

    public boolean deleteRemoteCseIdToResourceIdMapping(String cseBaseName, String remoteCseCseId) {
        if (!daoWriter.deleteRemoteCseIdToResourceIdMapping(cseBaseName, remoteCseCseId)) {
            LOG.error("deleteRemoteCseIdToResourceIdMapping: DB could not write");
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