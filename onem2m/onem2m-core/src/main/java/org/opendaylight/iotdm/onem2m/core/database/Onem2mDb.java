/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Child;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OneM2M data store uses the Binding Aware data store with transaction chaining.  See the ODL core tutorials
 * dsbenchmark for details.  The data store implements the Resource tree in the onenm2m yang model.  It is a very
 * generic data/meta model.  It enables all of the onem2m resource types to be CRUDed.  The semantics of how these
 * resources are enforced in the onem2m-core/resource layer.  The onenm2m-core/rest layer is used as the API into the
 * system.  The client (Onem2mRequestPrimitiveClient) is how the onenm2m-protocols adapt msgs to the onem2m protocol
 * independent format.  The message is then sent to the onem2m service.  See Onem2m.serviceRequest().
 *
 * The resource tree holds a list of cseBase's.  Onem2m provisions for one cseBase but I allowed for more just in case
 * more flexibility is required.  Examples of needing more might be customer partitioning.  Maybe a virtual tree is
 * given to each IOT network.  I don't know if this is realistic but the capability is there.  Also, there maybe a
 * feature that some internal application wishes to implement using a resource tree, so it can provision its own
 * cseBase if that makes sense.
 *
 * Each new cseBase provisioned is allocated a resource which is the root/base for its resource tree.  When any new
 * resource is provisioned under the cseBase, it is allocated a new resource and is added as a child resource to the
 * cseBase resource, and the new resource's parentID is set to point to the cseBase.  This mechanism allows a tree
 * to be built.
 *
 */
public class Onem2mDb implements TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mDb.class);
    private BindingTransactionChain bindingTransactionChain;
    private static Onem2mDb db;
    private static DbResourceTree dbResourceTree;
    static final String NULL_RESOURCE_ID = "0";

    /**
     * Allows other parts of the system to access the one and only instance of the "data store" object
     * @return the static instance of the db
     */
    public static Onem2mDb getInstance() {
        if (db == null) {
            db = new Onem2mDb();
        }
        return db;
    }
    private Onem2mDb() { }

    /**
     * Initialize the transaction chains for the database.
     * @param bindingDataBroker binding broker
     */
    public void initializeDatastore(DataBroker bindingDataBroker) {
        this.bindingTransactionChain = bindingDataBroker.createTransactionChain(this);
        dbResourceTree = new DbResourceTree(bindingTransactionChain);
        LOG.info("bindingTxChain: {}", this.bindingTransactionChain);
    }

    private String generateResourceId() {
        String b36ResourceId;
        do {
            int r = ThreadLocalRandom.current().nextInt(1, 1000000000); // 9 digit random id
            b36ResourceId = Integer.toString(r, 36); // gen at most 6 char string with 0-9,a-z as "digits"
        } while (dbResourceTree.retrieveResourceById(b36ResourceId) != null); // make sure it is not used already
        return b36ResourceId;
    }

    /**
     * Find the cse using its name
     * @param name cse name
     * @return found or not
     */
    public boolean findCseByName(String name) {
        return (dbResourceTree.retrieveCseByName(name) != null) ? true : false;
    }

    /**
     * The cse is a special resource, its the root of the tree, and all resources are created under it.
     * @param onem2mRequest request
     * @param onem2mResponse response
     * @return successful db create
     */
    public boolean createCseResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // generate a unique id for this new resource
        onem2mRequest.setResourceId(generateResourceId());

        // allocate a transaction, for the series of updates and creates to the data store
        DbTransaction dbTxn = new DbTransaction(bindingTransactionChain);

        dbResourceTree.createCseByName(dbTxn, onem2mRequest.getResourceName(), onem2mRequest.getResourceId());

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = dbResourceTree.createResource(dbTxn, onem2mRequest, NULL_RESOURCE_ID);

        // cache the resource
        onem2mRequest.setOnem2mResource(onem2mResource);

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }

    /**
     * The create resource is carried out by this routine.  The onem2mRequest has the parameters in it to effect the
     * creation request.  The resource specific routines have in .../core/resource already vetted the parameters so
     * its just a matter of adding the resource to the data store.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     * @return successful db create
     */
    public boolean createResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // generate a unique id for this new resource
        onem2mRequest.setResourceId(generateResourceId());

        // allocate a transaction, for the series of updates and creates to the data store
        DbTransaction dbTxn = null;

        Onem2mResource parentOnem2mResource = onem2mRequest.getOnem2mResource();

        /**
         * The resource name should be filled in with the resource-id if the name is blank.
         */
        String resourceName = onem2mRequest.getResourceName();
        if (resourceName == null) {
            onem2mRequest.setResourceName(onem2mRequest.getResourceId());
        }

        String parentId = parentOnem2mResource.getResourceId();
        String oldestId = parentOnem2mResource.getOldestId();
        String latestId = parentOnem2mResource.getLatestId();
        String prevId = Onem2mDb.NULL_RESOURCE_ID;

        // need to maintain the oldest and latest, and next-prev children too
        if (latestId.contains(Onem2mDb.NULL_RESOURCE_ID)) {

            latestId = onem2mRequest.getResourceId();
            oldestId = onem2mRequest.getResourceId();
            if (dbTxn == null) {
                dbTxn = new DbTransaction(bindingTransactionChain);
            }
            dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentOnem2mResource, oldestId, latestId);


        } else {

            prevId = latestId;
            Onem2mResource prevOnem2mResource = getResource(prevId);

            latestId = onem2mRequest.getResourceId();

            Child child = dbResourceTree.retrieveChildByName(parentId, prevOnem2mResource.getName());

            if (dbTxn == null) {
                dbTxn = new DbTransaction(bindingTransactionChain);
            }
            dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentOnem2mResource, oldestId, latestId);
            dbResourceTree.updateChildSiblingNextInfo(dbTxn, parentId, child, latestId);
        }

        if (dbTxn == null) {
            dbTxn = new DbTransaction(bindingTransactionChain);
        }

        // create a childEntry on the parent resourceId, <child-name, child-resourceId>
        dbResourceTree.createParentChildLink(dbTxn,
                parentId, // parent
                onem2mRequest.getResourceName(), // childName
                onem2mRequest.getResourceId(), // chileResourceId
                prevId, Onem2mDb.NULL_RESOURCE_ID); // siblings

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = dbResourceTree.createResource(dbTxn, onem2mRequest, parentId);

        // now save this newly created resource
        onem2mRequest.setOnem2mResource(onem2mResource);

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }

    /**
     * The update resource is carried out by this routine.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     * @return successful update
     */
    public boolean updateResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        DbTransaction dbTxn = new DbTransaction(bindingTransactionChain);

        /**
         * For each new attribute in the update list, replace it in the existing resource.  If the attr is null, then
         * remove it from the existing resource.
         */
        List<Attr> attrList = onem2mRequest.getResourceContent().getAttrList();
        for (Attr attr : attrList) {
            String value = attr.getValue();
            if (value != null) {
                dbResourceTree.updateAttr(dbTxn, onem2mRequest.getResourceId(), attr);

            } else {
                dbResourceTree.deleteAttr(dbTxn, onem2mRequest.getResourceId(), attr.getName());
            }
        }
        List<AttrSet> attrSetList = onem2mRequest.getResourceContent().getAttrSetList();
        for (AttrSet attrSet : attrSetList) {
            List<Member> member = attrSet.getMember();
            if (member != null) {
                dbResourceTree.updateAttrSet(dbTxn, onem2mRequest.getResourceId(), attrSet);

            } else {
                dbResourceTree.deleteAttrSet(dbTxn, onem2mRequest.getResourceId(), attrSet.getName());
            }
        }

        boolean success = dbTxn.commitTransaction();

        Onem2mResource onem2mResource = getResource(onem2mRequest.getResourceId());
        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setDbAttrs(new DbAttr(onem2mRequest.getOnem2mResource().getAttr()));
        //onem2mRequest.setDbAttrSets(new DbAttrSet(onem2mRequest.getOnem2mResource().getAttrSet()));

        return success;
    }

    /**
     * Locate resource in db using the target resource id at this level and a name
     * @param resourceId id at this level
     * @param resourceName name at this level
     * @return found status
     */
    public Boolean findResourceUsingIdAndName(String resourceId, String resourceName) {

        Onem2mResource onem2mResource = dbResourceTree.retrieveChildResourceByName(resourceId, resourceName);
        return (onem2mResource != null);
    }

    private Onem2mResource checkForLatestOldestContentInstance(Onem2mResource containerOnem2mResource, String resourceName) {

        if (resourceName.contentEquals("latest")) {
            String rt = getResourceType(containerOnem2mResource);
            /**
             * We are at a container resource and looking at "latest", check to see if the resource is a
             * content instance.  Note TODO: need to skip over non-CONTENT-INSTANCE resources
             * There might be subscription resources so skip until find a CONTENT-INSTANCE
             */
            if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {
                Onem2mResource onem2mResource = getResource(containerOnem2mResource.getLatestId());
                if (onem2mResource != null) {
                    rt = getResourceType(onem2mResource);
                    if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
                        return onem2mResource;
                    }
                }
            }
        } else if (resourceName.contentEquals("oldest")) {
            String rt = getResourceType(containerOnem2mResource);
            /**
             * We are at a container resource and looking at "latest", check to see if the resource is a
             * content instance
             */
            if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {
                Onem2mResource onem2mResource = getResource(containerOnem2mResource.getOldestId());
                if (onem2mResource != null) {
                    rt = getResourceType(onem2mResource);
                    if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
                        return onem2mResource;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Using the target URI, find the resource
     * @param onem2mRequest request
     * @param onem2mResponse response
     * @return found status
     */
    public Boolean findResourceUsingURI(String targetURI,
                                        RequestPrimitive onem2mRequest,
                                        ResponsePrimitive onem2mResponse) {

        targetURI = trimURI(targetURI); // get rid of leading and following "/"
        String hierarchy[] = targetURI.split("/"); // split the URI into its hierarchy of path component strings

        // start by looking at the cse root: the first level is the cse name
        Onem2mCse cse = dbResourceTree.retrieveCseByName(hierarchy[0]);
        if (cse == null)
            return false; // resource not found

        /**
         * Cases to consider:
         * 1) the targetURI is just the cse --> hierarchy.length == 1
         * 2) the target URI has just one more level --> hierarchy.length == 2
         * 2a) hierarchy[1] is a non-hierarchical id
         * 2b) hierarchy[1] is the name of a real resource
         * 3) the hierarchy needs to be traversed looking at each level by name until there are no more levels
         *    or we find the resource ny name
         */
        if (hierarchy.length == 1) { // case 1
            onem2mRequest.setOnem2mResource(dbResourceTree.retrieveResourceById(cse.getResourceId()));
            onem2mRequest.setDbAttrs(new DbAttr(onem2mRequest.getOnem2mResource().getAttr()));
            onem2mRequest.setResourceId(cse.getResourceId());
            return true;
        }

        Onem2mResource onem2mResource = null;
        Onem2mResource saveOnem2mResource = null;

        if (hierarchy.length == 2) { // case 2

            onem2mResource  = dbResourceTree.retrieveResourceById(hierarchy[1]); // case 2a
            if (onem2mResource == null)
                onem2mResource = dbResourceTree.retrieveChildResourceByName(cse.getResourceId(), hierarchy[1]); // case 2b
        } else { // case 3
            /**
             * This routine starts at hierarchy[1] and buzzes down the hierarchy looking for the resource name
             */
            String resourceId = cse.getResourceId();
            for (int hierarchyIndex = 1; hierarchyIndex < hierarchy.length; hierarchyIndex++) {
                onem2mResource = dbResourceTree.retrieveChildResourceByName(resourceId, hierarchy[hierarchyIndex]);
                if (onem2mResource == null) {
                    onem2mResource = checkForLatestOldestContentInstance(saveOnem2mResource, hierarchy[hierarchyIndex]);
                    if (onem2mResource == null) {
                        break;
                    }
                }
                resourceId = onem2mResource.getResourceId();
                saveOnem2mResource = onem2mResource;
            }
        }

        if (onem2mResource == null)
            return false; // resource not found

        onem2mRequest.setResourceId(onem2mResource.getResourceId());
        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setDbAttrs(new DbAttr(onem2mRequest.getOnem2mResource().getAttr()));
        return true;
    }

    /**
     * Using the target URI/attribute, strip off the attribute, and see if a resource is found.  Then look to see
     * if the attribute exists under this resource type.
     * @param onem2mRequest request
     * @param onem2mResponse response
     * @return found status
     */
    public Boolean findResourceUsingURIAndAttribute(String uriAndAttribute,
                                                    RequestPrimitive onem2mRequest,
                                                    ResponsePrimitive onem2mResponse) {
        String trimmedURI = trimURI(uriAndAttribute); // get rid of leading and following "/"
        String hierarchy[] = trimmedURI.split("/"); // split the URI into its hierarchy of path component strings
        if (hierarchy.length <= 1) {
            return false;
        }
        String targetAttribute = hierarchy[hierarchy.length-1];
        String targetURI = "";
        for (int i = 0; i < hierarchy.length-1; i++) {
            targetURI += "/" + hierarchy[i];
        }
        if (findResourceUsingURI(targetURI, onem2mRequest, onem2mResponse)) {
            String value = onem2mRequest.getDbAttrs().getAttr(targetAttribute);
            if (value != null) {
                onem2mRequest.setRetrieveByAttrName(targetAttribute);
                return false;// TODO: need more downstream work to support this
            }
        }
        return false;
    }

    /**
     * Using the resourceId, build the non hierarchical name of the path using the /cseName + /name of resource
     * @param resourceId resource id
     * @return name of resource using the /cse/resourceId format
     */
    public String getNonHierarchicalNameForResource(String resourceId) {

        if (resourceId == null || resourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            return null;
        }
        String hierarchy = "/" + resourceId;

        Onem2mResource onem2mResource = dbResourceTree.retrieveResourceById(resourceId);
        while (onem2mResource != null) {

            if (onem2mResource.getParentId().contentEquals(NULL_RESOURCE_ID)) {
                break;
            }
            resourceId = onem2mResource.getParentId();
            onem2mResource = dbResourceTree.retrieveResourceById(resourceId);
        }
        hierarchy = "/" + onem2mResource.getName() + hierarchy;

        return hierarchy;
    }

    /**
     * Using the resourceId, traverse up the hierarchy till reach the root building the path.
     * @param resourceId the resource id
     * @return name of the resource in hierarchical format
     */
    public String getHierarchicalNameForResource(String resourceId) {
        String hierarchy = "";

        Onem2mResource onem2mResource = dbResourceTree.retrieveResourceById(resourceId);
        while (onem2mResource != null) {
            hierarchy = "/" + onem2mResource.getName() + hierarchy;
            resourceId = onem2mResource.getParentId();
            if (resourceId.contentEquals(NULL_RESOURCE_ID)) {
                break;
            }
            onem2mResource = dbResourceTree.retrieveResourceById(resourceId);
        }
        return hierarchy;
    }

    /**
     * Process the hierarchy building a list of the resourceId's.  The routine does not use recursion for fear of
     * stack overflow.  It simply adds child resources on the end of a list that it is processing to build its
     * hierarchical list.
     * @param startResourceId target resource id
     * @param limit enforce a limit of how many are in the list
     * @return the list of resource id's
     */
    public List<String> getHierarchicalResourceList(String startResourceId, int limit) {

        Onem2mResource onem2mResource = dbResourceTree.retrieveResourceById(startResourceId);
        if (onem2mResource == null || limit < 1) {
            return null;
        }
        List<String> resourceList = new ArrayList<String>();
        resourceList.add(startResourceId);
        if (resourceList.size() >= limit) {
            return resourceList;
        }

        // tack new resources onto the end of resourceList as the children are read
        int resourceListLen = 1;
        for (int i = 0; i < resourceListLen; i++) {
            onem2mResource = dbResourceTree.retrieveResourceById(resourceList.get(i));
            List<Child> childResourceList = onem2mResource.getChild();
            for (Child childResource : childResourceList) {
                resourceList.add(childResource.getResourceId());
                if (resourceList.size() >= limit) {
                    return resourceList;
                }
            }
            resourceListLen += childResourceList.size();
        }

        return resourceList;
    }

    /**
     * Get the resource
     * @param resourceId the resource id to get
     * @return the resource info for this id
     */
    public Onem2mResource getResource(String resourceId) {
        return dbResourceTree.retrieveResourceById(resourceId);
    }

    /**
     * Find the resource type in the list of attrs
     * @param onem2mResource the input resource
     * @return the resource type
     */
    public String getResourceType(Onem2mResource onem2mResource) {
        for (Attr attr : onem2mResource.getAttr()) {
            if (attr.getName().contentEquals(ResourceContent.RESOURCE_TYPE)) {
                return attr.getValue();
            }
        }
        return null;
    }

    /**
     * Deletes are idempotent.  This routine is called after the requestProcessor has grabbed all info it needed
     * for the delete operation so now it is a bulk delete.
     * @param onem2mRequest request
     * @param onem2mResponse response
     * @return found status
     */
    public Boolean deleteResourceUsingURI(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        DbTransaction dbTxn = null;

        // if resource not found, then quietly return OK, as the net result is the resource is gone
        if (onem2mRequest.getOnem2mResource() == null)
            return true;

        // save the parent
        String parentResourceId = onem2mRequest.getOnem2mResource().getParentId();
        Onem2mResource parentOnem2mResource = this.getResource(parentResourceId);

        // cache this resource to be deleted
        String thisResourceId = onem2mRequest.getOnem2mResource().getResourceId();
        String thisResourceName = onem2mRequest.getOnem2mResource().getName();

        // build a 'to be Deleted list' by walking the hierarchy
        List<String> resourceIdList = getHierarchicalResourceList(thisResourceId, Onem2m.MAX_RESOURCES + 1);

        if (parentOnem2mResource.getLatestId().contentEquals(parentOnem2mResource.getOldestId())) {

            if (dbTxn == null) {
                dbTxn = new DbTransaction(bindingTransactionChain);
            }

            // only child, set oldest/latest back to NULL
            dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentOnem2mResource, Onem2mDb.NULL_RESOURCE_ID,
                    Onem2mDb.NULL_RESOURCE_ID);

        } else if (parentOnem2mResource.getLatestId().contentEquals(thisResourceId)) {

            // deleting the latest, go back to prev and set is next to null, re point latest to prev
            Child curr = dbResourceTree.retrieveChildByName(parentResourceId, thisResourceName);
            String prevId = curr.getPrevId();
            Onem2mResource prevOnem2mResource = this.getResource(prevId);

            Child child = dbResourceTree.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());

            if (dbTxn == null) {
                dbTxn = new DbTransaction(bindingTransactionChain);
            }

            dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentOnem2mResource, parentOnem2mResource.getOldestId(),
                    prevId);
            dbResourceTree.updateChildSiblingNextInfo(dbTxn, parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID);

        } else if (parentOnem2mResource.getOldestId().contentEquals(thisResourceId)) {

            // deleting the oldest, go to next and set its prev to null, re point oldest to next
            Child curr = dbResourceTree.retrieveChildByName(parentResourceId, thisResourceName);
            String nextId = curr.getNextId();
            Onem2mResource nextOnem2mResource = this.getResource(nextId);

            Child child = dbResourceTree.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());

            if (dbTxn == null) {
                dbTxn = new DbTransaction(bindingTransactionChain);
            }

            dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentOnem2mResource, nextId,
                    parentOnem2mResource.getLatestId());
            dbResourceTree.updateChildSiblingPrevInfo(dbTxn, parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID);

        } else {

            Child curr = dbResourceTree.retrieveChildByName(parentResourceId, thisResourceName);

            String nextId = curr.getNextId();
            Onem2mResource nextOnem2mResource = this.getResource(nextId);
            Child prevChild = dbResourceTree.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());


            String prevId = curr.getPrevId();
            Onem2mResource prevOnem2mResource = this.getResource(prevId);
            Child nextChild = dbResourceTree.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());


            if (dbTxn == null) {
                dbTxn = new DbTransaction(bindingTransactionChain);
            }

            dbResourceTree.updateChildSiblingPrevInfo(dbTxn, parentResourceId, nextChild, Onem2mDb.NULL_RESOURCE_ID);
            dbResourceTree.updateChildSiblingNextInfo(dbTxn, parentResourceId, prevChild, Onem2mDb.NULL_RESOURCE_ID);

        }

        if (dbTxn == null) {
            dbTxn = new DbTransaction(bindingTransactionChain);
        }

        // now in a transaction, smoke all the resources under this ResourceId
        for (String resourceId : resourceIdList) {
            dbResourceTree.deleteResourceById(dbTxn, resourceId);
        }
        onem2mRequest.setOnem2mResource(null);

        // now go clean up the parent of this resource as its child link needs to be removed
        if (!parentResourceId.contentEquals(NULL_RESOURCE_ID))
            dbResourceTree.removeParentChildLink(dbTxn, parentResourceId, thisResourceName);

        // commit the deleteFest!
        return dbTxn.commitTransaction();
    }

    /**
     * Dump resource info the the karaf log
     * @param resourceId resource to start dumping from
     */
    public void dumpResourceIdLog(String resourceId) {
        dbResourceTree.dumpRawTreeToLog(resourceId);
    }

    /**
     * Dump resource info the the karaf log
     * @param resourceId all or starting resource id
     */
    public void dumpHResourceIdToLog(String resourceId) {
        dbResourceTree.dumpHierarchicalTreeToLog(resourceId);
    }

    /**
     * Remove all resources from the datastore
     */
    public void cleanupDataStore() {
        dbResourceTree.reInitializeDatastore(); // reinitialize the data store.
    }

    /**
     * The URI can be /cseBase/x/y/z/, and this routine turns it into cseBase/x/y/z ie. strip leading and trailing /
     * @param uri the URI of the target
     * @return stripped URI
     */
    private static String trimURI(String uri) {
        uri = uri.trim();
        uri = uri.startsWith("/") ? uri.substring("/".length()) : uri;
        uri = uri.endsWith("/") ? uri.substring(0,uri.length()-1) : uri;
        return uri;
    }

    /**
     * shutdown the db, close the binding aware transaction chains
     */
    public void close() {
        try {
            bindingTransactionChain.close();
        }
        catch (IllegalStateException e) {
            LOG.error("Transaction close failed,", e);
        }
    }

    /**
     * onTransactionChainFailed
     * @param chain the chain
     * @param transaction the transaction
     * @param cause the fail reason
     */
    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain,
                                         AsyncTransaction<?, ?> transaction, Throwable cause) {
        LOG.error("Broken chain {} in Db, transaction {}, cause {}",
                chain, transaction.getIdentifier(), cause);
        assert(false);
    }

    /**
     * onTransactionChainSuccessful
     * @param chain the chain
     */
    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.info("Db closed successfully, chain {} BUT WHY, closed unintentionally", chain);
        assert(false);
    }
}