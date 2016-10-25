/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mParentChildListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChildKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.OldestLatest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.OldestLatestKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gguliash on 4/28/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class ResourceTreeReader {

    private final Logger LOG = LoggerFactory.getLogger(ResourceTreeReader.class);

    private ReadOnlyCache cache;
    private DaoResourceTreeReader daoResourceTreeReader;
    private TTLGarbageCollector ttlGarbageCollector;

    public ResourceTreeReader(Cache cache, DaoResourceTreeReader daoResourceTreeReader, TTLGarbageCollector ttlGarbageCollector) {
        this.cache = cache;
        this.daoResourceTreeReader = daoResourceTreeReader;
        this.ttlGarbageCollector = ttlGarbageCollector;
    }

    public TTLGarbageCollector getTTLGC() {
        return ttlGarbageCollector;
    }

    /**
     * Gwet the cse from the db store using its key ie. the cseName
     *
     * @param name cse name
     * @return the cse object
     */
    public Onem2mCse retrieveCseByName(String name) {
        return cache.retrieveCseByName(new Onem2mCseKey(name));
    }

    public Onem2mCse retrieveCse(String identificator) {
        Onem2mCse cse = retrieveCseByName(identificator);
        if(cse == null) {
            cse = retrieveCseById(identificator);
        }
        return cse;
    }

    private Onem2mCse retrieveCseById(String cseId) {
        Onem2mCseList cseList = daoResourceTreeReader.retrieveFullCseList();
        if (cseList != null) {
            for (Onem2mCse onem2mCse : cseList.getOnem2mCse()) {
                if (onem2mCse.getResourceId().equals(cseId)) {
                    return onem2mCse;
                }
            }
        }
        return null;
    }


    public List<String> getCseList() {
        List<String> cseStringList = null;
        Onem2mCseList cseList = daoResourceTreeReader.retrieveFullCseList();
        if (cseList != null) {
            cseStringList = new ArrayList<>();
            for (Onem2mCse onem2mCse : cseList.getOnem2mCse()) {
                cseStringList.add(onem2mCse.getName());
            }
        }
        return cseStringList;
    }

//    private String retrieveAeResourceIdByAeId(String cseBaseName, String aeId) {
//        // TODO implement cache
//        return daoResourceTreeReader.retrieveAeResourceIdByAeId(cseBaseName, aeId);
//    }

    /**
     * Checks whether the entity specified by the entityId is registered at the cseBase.
     * Returns type of entity as Onem2m resource type string if the entity is registered
     * as AE or remoteCSE.
     * @param entityId The ID of entity (CSE-ID or AE-ID)
     * @param cseBaseCseId CSE-ID of the cseBase
     * @return AE resource type or remoteCSE resource type if the entity is registered, null otherwise
     */
    public String isEntityRegistered(String entityId, String cseBaseCseId) {
        // TODO Temporary caching solution for CSE-IDs
        if (null != cseBaseCseId) {
            if (Onem2mRouterService.getInstance().hasRemoteCse(cseBaseCseId, entityId)) {
                // Entity is registered as remoteCSE
                return Onem2m.ResourceType.REMOTE_CSE;
            }
        } else {
            if (Onem2mRouterService.getInstance().hasRemoteCse(entityId)) {
                // Entity is registered as remoteCSE
                return Onem2m.ResourceType.REMOTE_CSE;
            }
        }

        // TODO implement cache
        return daoResourceTreeReader.isEntityRegistered(entityId, cseBaseCseId);
    }

    /**
     * Retrieve the OldestLatest structure using its resource type
     *
     * @param resourceId   this id
     * @param resourceType name of child
     * @return the child
     */
    public OldestLatest retrieveOldestLatestByResourceType(String resourceId, String resourceType) {
        return cache.retrieveOldestLatestByResourceType(new Onem2mResourceKey(resourceId),
                new OldestLatestKey(resourceType));
    }

    /**
     * Get a resource from the data store using its key, ie. its resourceId
     *
     * @param resourceId the resource id
     * @return the resource
     */
    public Onem2mResourceElem retrieveResourceById(String resourceId) {
        return cache.retrieveResourceById(new Onem2mResourceKey(resourceId));
    }

    /**
     * Retrieve the child using its resource name
     *
     * @param resourceId this id
     * @return the child
     */
    public List<Onem2mParentChild> retrieveParentChildList(String resourceId) {
        return daoResourceTreeReader.retrieveParentChildList(new Onem2mParentChildListKey(resourceId));
    }

    /**
     * Retrieve the child using its resource name
     *
     * @param resourceId this id
     * @param childName  name of child
     * @return the child
     */
    public Onem2mParentChild retrieveChildByName(String resourceId, String childName) {
        return daoResourceTreeReader.retrieveChildByName(resourceId, childName);
    }

    /**
     * Using the parent resourceId, lookup its child by name.
     *
     * @param resourceId this resource
     * @param name       name
     * @return the resource
     */
    public Onem2mResource retrieveChildResourceByName(String resourceId, String name) {
        Onem2mParentChild child = retrieveChildByName(resourceId, name);
        if (child != null) {
            return retrieveResourceById(child.getResourceId());
        }
        return null;
    }


    public String retrieveChildResourceIDByName(String resourceId, String name) {
        Onem2mParentChild child = retrieveChildByName(resourceId, name);
        if (child != null) {
            return child.getResourceId();
        }
        return null;
    }

    // TODO extend dumps with the AE-ID mappings ??

    /**
     * Dump resource info to the log
     *
     * @param onem2mResource this resource
     * @param dumpChildList  child verbose dump option
     */
    public void dumpResourceToLog(Onem2mResource onem2mResource, boolean dumpChildList) {
        LOG.info("    Resource: id: {}, name: {}, parentId: {}",
                onem2mResource.getResourceId(), onem2mResource.getName(), onem2mResource.getParentId());
        List<OldestLatest> oldestLatestList = onem2mResource.getOldestLatest();
        LOG.info("    OldestLatest List: count: {}", oldestLatestList.size());
        for (OldestLatest oldestLatest : oldestLatestList) {
            LOG.info("        oldestLatest: resource type: {}, oldest: {}, latest: {}",
                    oldestLatest.getResourceType(), oldestLatest.getOldestId(), oldestLatest.getLatestId());
        }
        List<Onem2mParentChild> childList = retrieveParentChildList(onem2mResource.getResourceId());
        LOG.info("    Child List: count: {}", childList.size());
        if (dumpChildList) {
            for (Onem2mParentChild child : childList) {
                LOG.info("        Child: name: {}, id: {}, prev: {}, next: {}",
                        child.getName(), child.getResourceId(),
                        child.getPrevId(), child.getNextId());
            }
        }
        LOG.info("    Resource Content: {}", onem2mResource.getResourceContentJsonString());
    }

    /**
     * Dump in raw form (no hierarchy) the resource tree
     *
     * @param resourceId this resource
     */
    public void dumpRawTreeToLog(String resourceId) {
        LOG.warn("Function is slow, does not use cache");

        // if a resource id is supplied, dump the specific resource or fall thru and dump them all
        if (resourceId != null) {
            LOG.info("Dumping ResourceId: {}, Start", resourceId);

            Onem2mResource onem2mResource = retrieveResourceById(resourceId);
            if (onem2mResource != null) {
                dumpResourceToLog(onem2mResource, false);
            }
            LOG.info("Dumping ResourceId: {}, End", resourceId);

            return;
        }

        Onem2mCseList cseList = daoResourceTreeReader.retrieveFullCseList();

        Onem2mResourceTree tree = daoResourceTreeReader.retrieveFullResourceList();

        LOG.info("Dumping Resource Tree: Start ...");
        List<Onem2mCse> onem2mCseList = cseList.getOnem2mCse();
        LOG.info("CSEBASE List: count: {}", onem2mCseList.size());
        for (Onem2mCse onem2mCse : onem2mCseList) {
            LOG.info("Cse: id: {}, name: {}", onem2mCse.getResourceId(), onem2mCse.getName());
        }

        List<Onem2mResource> onem2mResourceList = tree.getOnem2mResource();
        LOG.info("Resource List: count: {}", onem2mResourceList.size());
        for (Onem2mResource onem2mResource : onem2mResourceList) {
            if (daoResourceTreeReader.retrieveResourceById(new Onem2mResourceKey(onem2mResource.getResourceId())) == null) {
                LOG.warn("Could find resource key, but could not find, it's possible that it was removed");
            }
            dumpResourceToLog(onem2mResource, true);
        }
        LOG.info("Dumping Resource Tree: End ...");
    }

    private void dumpHierarchicalResourceToLog(String resourceId) {
        Onem2mResource onem2mResource = this.retrieveResourceById(resourceId);
        dumpResourceToLog(onem2mResource, false);
        List<Onem2mParentChild> childList = retrieveParentChildList(resourceId);
        for (Onem2mParentChild child : childList) {
            dumpHierarchicalResourceToLog(child.getResourceId());
        }
    }

    /**
     * Dump in hierarchical form
     *
     * @param resourceId the resource tree
     */
    public void dumpHierarchicalTreeToLog(String resourceId) {

        // if a resource id is supplied, dump the specific resource or fall thru and dump them all
        if (resourceId != null) {
            LOG.info("Dumping ResourceId: {}, Start", resourceId);
            dumpHierarchicalResourceToLog(resourceId);
            LOG.info("Dumping ResourceId: {}, End", resourceId);
            return;
        }
        Onem2mCseList cseList = daoResourceTreeReader.retrieveFullCseList();

        LOG.info("Dumping Hierarchical Resource Tree: Start ...");
        List<Onem2mCse> onem2mCseList = cseList.getOnem2mCse();
        LOG.info("CSEBASE List: count: {}", onem2mCseList.size());
        for (Onem2mCse onem2mCse : onem2mCseList) {
            LOG.info("Cse: id: {}, name: {}", onem2mCse.getResourceId(), onem2mCse.getName());
            dumpHierarchicalResourceToLog(onem2mCse.getResourceId());
        }
        LOG.info("Dumping Hierarchical Resource Tree: End ...");
    }
}
