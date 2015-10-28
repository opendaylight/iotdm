/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceAE;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContentInstance;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.NotificationPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.*;
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

    private DataBroker dataBroker;
    private static Onem2mDb db;
    private static DbResourceTree dbResourceTree;
    static final String NULL_RESOURCE_ID = "0";
    private AtomicInteger nextId;
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
    private Onem2mDb() {
        nextId = new AtomicInteger();
    }

    /**
     * Initialize the transaction chains for the database.
     * @param dataBroker data broker
     */
    public void initializeDatastore(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        dbResourceTree = new DbResourceTree(dataBroker);
    }

    /* When we turn on persistence, we will need to store this too */

    private String generateResourceId() {

        String b36ResourceId;
        int r = nextId.incrementAndGet();
        b36ResourceId = Integer.toString(r, 36);
        //b36ResourceId = Integer.toString(r);
        return b36ResourceId;
    }

    /*
    private String generateResourceId() {

        String b36ResourceId;
        do {
            int r = ThreadLocalRandom.current().nextInt(1, 1000000000); // 9 digit random id
            b36ResourceId = Integer.toString(r, 36); // gen at most 6 char string with 0-9,a-z as "digits"
        } while (dbResourceTree.retrieveResourceById(b36ResourceId) != null); // make sure it is not used already
        return b36ResourceId;
    }
    */


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
        DbTransaction dbTxn = new DbTransaction(dataBroker);

        dbResourceTree.createCseByName(dbTxn, onem2mRequest.getResourceName(), onem2mRequest.getResourceId());

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = dbResourceTree.createResource(dbTxn, onem2mRequest, NULL_RESOURCE_ID);

        // cache the resource
        onem2mRequest.setOnem2mResource(onem2mResource);

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }


    private void updateParentLastModifiedTime(DbTransaction dbTxn, RequestPrimitive onem2mRequest, String parentResourceId) {

        Attr attr = new AttrBuilder()
                .setKey(new AttrKey(ResourceContent.LAST_MODIFIED_TIME))
                .setName(ResourceContent.LAST_MODIFIED_TIME)
                .setValue(onem2mRequest.getResourceContent().getDbAttr(ResourceContent.CREATION_TIME))
                .build();
        dbResourceTree.updateAttr(dbTxn, parentResourceId, attr);
    }

    private void adjustContainerCurrValues(DbTransaction dbTxn, RequestPrimitive onem2mRequest, String containerResourceId) {

        Attr attr = new AttrBuilder()
                .setKey(new AttrKey(ResourceContainer.CURR_BYTE_SIZE))
                .setName(ResourceContainer.CURR_BYTE_SIZE)
                .setValue(onem2mRequest.containerCbs.toString())
                .build();
        dbResourceTree.updateAttr(dbTxn, containerResourceId, attr);

        attr = new AttrBuilder()
                .setKey(new AttrKey(ResourceContainer.CURR_NR_INSTANCES))
                .setName(ResourceContainer.CURR_NR_INSTANCES)
                .setValue(onem2mRequest.containerCni.toString())
                .build();
        dbResourceTree.updateAttr(dbTxn, containerResourceId, attr);

        attr = new AttrBuilder()
                .setKey(new AttrKey(ResourceContent.STATE_TAG))
                .setName(ResourceContent.STATE_TAG)
                .setValue(onem2mRequest.containerSt.toString())
                .build();
        dbResourceTree.updateAttr(dbTxn, containerResourceId, attr);
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



        String parentId = parentOnem2mResource.getResourceId();
        String prevId = Onem2mDb.NULL_RESOURCE_ID;

        String resourceType = onem2mRequest.getResourceContent().getDbAttr(ResourceContent.RESOURCE_TYPE);
        String resourceName = onem2mRequest.getResourceName();

        // see resourceAE for comments on why AE_IDs have these naming rules
        if (resourceType.contentEquals(Onem2m.ResourceType.AE)) {
            String from = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
            if (from == null) {
                if (resourceName == null) {
                    onem2mRequest.setResourceName("C" + onem2mRequest.getResourceId());
                }
                onem2mRequest.getResourceContent().setDbAttr(ResourceAE.AE_ID, onem2mRequest.getResourceName());
            } else {
                // in the last Design:
                // if from !=null, since  resourceName at least = from, how can the following happen?
                if (resourceName == null) {
                    // remove the "http://" or anything before //, including the //
                    String[] splitStrins = from.split("//");
                    // does not need to concern 2 //, we will check valid URI in the following steps
                    String removedHead = splitStrins[splitStrins.length-1];
                    onem2mRequest.setResourceName(removedHead);
                }
                //TODo : do we want to set the AE-ID like http://xxx/yyy  or  xxx?
                onem2mRequest.getResourceContent().setDbAttr(ResourceAE.AE_ID, onem2mRequest.getResourceName());
            }
        }

        /**
         * The resource name should be filled in with the resource-id if the name is blank.
         */
        if (onem2mRequest.getResourceName() == null) {
            onem2mRequest.setResourceName(onem2mRequest.getResourceId());
        }

        OldestLatest parentOldestLatest =
                dbResourceTree.retrieveOldestLatestByResourceType(parentId, resourceType);
        if (parentOldestLatest != null) {
            String oldestId = parentOldestLatest.getOldestId();
            String latestId = parentOldestLatest.getLatestId();

            // need to maintain the oldest and latest, and next-prev children too
            if (latestId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {

                latestId = onem2mRequest.getResourceId();
                oldestId = onem2mRequest.getResourceId();
                if (dbTxn == null) {
                    dbTxn = new DbTransaction(dataBroker);
                }
                dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentId, resourceType, oldestId, latestId);


            } else {

                prevId = latestId;
                Onem2mResource prevOnem2mResource = getResource(prevId);

                latestId = onem2mRequest.getResourceId();

                Child child = dbResourceTree.retrieveChildByName(parentId, prevOnem2mResource.getName());

                if (dbTxn == null) {
                    dbTxn = new DbTransaction(dataBroker);
                }
                dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentId, resourceType, oldestId, latestId);
                dbResourceTree.updateChildSiblingNextInfo(dbTxn, parentId, child, latestId);
            }
        }

        if (dbTxn == null) {
            dbTxn = new DbTransaction(dataBroker);
        }

        // see if a content instance creation caused the container to be updated
        if (onem2mRequest.mustUpdateContainer) {
            this.adjustContainerCurrValues(dbTxn, onem2mRequest, parentId);
        }

        // update the lmt of the parent to be the creation time of the child being created
        if (!parentId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            updateParentLastModifiedTime(dbTxn, onem2mRequest, parentId);
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

        DbTransaction dbTxn = new DbTransaction(dataBroker);

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
        onem2mRequest.setDbAttrSets(new DbAttrSet(onem2mRequest.getOnem2mResource().getAttrSet()));

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

    private Onem2mResource checkForLatestOldestContentInstance(Onem2mResource containerOnem2mResource,
                                                               String resourceName) {

        if (resourceName.contentEquals(ResourceContainer.LATEST) || resourceName.contentEquals("latest")) {
            String rt = getResourceType(containerOnem2mResource);
            if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {

                OldestLatest oldestLatest = dbResourceTree.retrieveOldestLatestByResourceType(
                        containerOnem2mResource.getResourceId(),
                        Onem2m.ResourceType.CONTENT_INSTANCE);
                if (oldestLatest == null) {
                    return null;
                }
                Onem2mResource onem2mResource = getResource(oldestLatest.getLatestId());
                if (onem2mResource != null) {
                    rt = getResourceType(onem2mResource);
                    if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
                        return onem2mResource;
                    }
                }
            }
        } else if (resourceName.contentEquals(ResourceContainer.OLDEST) || resourceName.contentEquals("oldest")) {
            String rt = getResourceType(containerOnem2mResource);
            if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {
                OldestLatest oldestLatest = dbResourceTree.retrieveOldestLatestByResourceType(
                        containerOnem2mResource.getResourceId(),
                        Onem2m.ResourceType.CONTENT_INSTANCE);
                if (oldestLatest == null) {
                    return null;
                }
                Onem2mResource onem2mResource = getResource(oldestLatest.getOldestId());
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
            onem2mRequest.setDbAttrSets(new DbAttrSet(onem2mRequest.getOnem2mResource().getAttrSet()));
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
        onem2mRequest.setDbAttrSets(new DbAttrSet(onem2mRequest.getOnem2mResource().getAttrSet()));
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

    private void deleteContentInstance(DbTransaction dbTxn,
                                       RequestPrimitive onem2mRequest,
                                       Onem2mResource containerResource) {

        String containerResourceId = containerResource.getResourceId();
        DbAttr containerDbAttrs = new DbAttr(containerResource.getAttr());

        String csString = onem2mRequest.getDbAttrs().getAttr(ResourceContentInstance.CONTENT_SIZE);
        ResourceContainer.setCurrValuesForThisDeletedContentInstance(onem2mRequest,
                containerDbAttrs,
                Integer.valueOf(csString));
        adjustContainerCurrValues(dbTxn, onem2mRequest, containerResourceId);
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
        String resourceType = onem2mRequest.getDbAttrs().getAttr(ResourceContent.RESOURCE_TYPE);
        OldestLatest parentOldestLatest = dbResourceTree.retrieveOldestLatestByResourceType(parentResourceId, resourceType);

        if (parentOldestLatest != null) {
            if (parentOldestLatest.getLatestId().contentEquals(parentOldestLatest.getOldestId())) {

                if (dbTxn == null) {
                    dbTxn = new DbTransaction(dataBroker);
                }

                // only child, set oldest/latest back to NULL
                dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentResourceId, resourceType,
                        Onem2mDb.NULL_RESOURCE_ID,
                        Onem2mDb.NULL_RESOURCE_ID);

            } else if (parentOldestLatest.getLatestId().contentEquals(thisResourceId)) {

                // deleting the latest, go back to prev and set is next to null, re point latest to prev
                Child curr = dbResourceTree.retrieveChildByName(parentResourceId, thisResourceName);
                String prevId = curr.getPrevId();
                Onem2mResource prevOnem2mResource = this.getResource(prevId);

                Child child = dbResourceTree.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());

                if (dbTxn == null) {
                    dbTxn = new DbTransaction(dataBroker);
                }

                dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentResourceId, resourceType,
                        parentOldestLatest.getOldestId(),
                        prevId);
                dbResourceTree.updateChildSiblingNextInfo(dbTxn, parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID);

            } else if (parentOldestLatest.getOldestId().contentEquals(thisResourceId)) {

                // deleting the oldest, go to next and set its prev to null, re point oldest to next
                Child curr = dbResourceTree.retrieveChildByName(parentResourceId, thisResourceName);
                String nextId = curr.getNextId();
                Onem2mResource nextOnem2mResource = this.getResource(nextId);

                Child child = dbResourceTree.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());

                if (dbTxn == null) {
                    dbTxn = new DbTransaction(dataBroker);
                }

                dbResourceTree.updateResourceOldestLatestInfo(dbTxn, parentResourceId, resourceType,
                        nextId,
                        parentOldestLatest.getLatestId());
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
                    dbTxn = new DbTransaction(dataBroker);
                }

                dbResourceTree.updateChildSiblingPrevInfo(dbTxn, parentResourceId, nextChild, Onem2mDb.NULL_RESOURCE_ID);
                dbResourceTree.updateChildSiblingNextInfo(dbTxn, parentResourceId, prevChild, Onem2mDb.NULL_RESOURCE_ID);
            }
        }

        if (dbTxn == null) {
            dbTxn = new DbTransaction(dataBroker);
        }

        // adjust the curr values in the parent container resource
        if (resourceType.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
            deleteContentInstance(dbTxn, onem2mRequest, parentOnem2mResource);
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
     * Dump content instances for the container Uri from Head to Tail.  Then again, from Tail to Head
     */
    public void dumpContentInstancesForContainer(String containerUri) {

        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
            LOG.error("dumpContentInstancesForContainer: cannot find container: {}", containerUri);
            return;
        }

        String resourceType = onem2mRequest.getDbAttrs().getAttr(ResourceContent.RESOURCE_TYPE);
        if (!resourceType.contentEquals(Onem2m.ResourceType.CONTAINER)) {
            LOG.error("dumpContentInstancesForContainer: resource is not a container: {}", containerUri, resourceType);
        }
        Onem2mResource o = onem2mRequest.getOnem2mResource();
        String containerResourceId = onem2mRequest.getOnem2mResource().getResourceId();

        OldestLatest containerOldestLatest = dbResourceTree.retrieveOldestLatestByResourceType(containerResourceId,
                Onem2m.ResourceType.CONTENT_INSTANCE);

        if (containerOldestLatest != null) {
            LOG.error("dumpContentInstancesForContainer: dumping oldest to latest: containerResourceUri:{}, containerId: {}, oldest={}, latest={}",
                    containerUri, containerResourceId,
                    containerOldestLatest.getOldestId(), containerOldestLatest.getLatestId());
            String resourceId = containerOldestLatest.getOldestId();
            while (resourceId != Onem2mDb.NULL_RESOURCE_ID) {
                Onem2mResource tempResource = getResource(resourceId);
                Child child = dbResourceTree.retrieveChildByName(containerResourceId, tempResource.getName());
                LOG.error("dumpContentInstancesForContainer: prev:{}, next:{} ", child.getPrevId(), child.getNextId());
                resourceId = child.getNextId();
            }
            LOG.error("dumpContentInstancesForContainer: dumping latest to oldest: containerResourceUri:{}, containerId: {}, oldest={}, latest={}",
                    containerUri, containerResourceId,
                    containerOldestLatest.getOldestId(), containerOldestLatest.getLatestId());
            resourceId = containerOldestLatest.getLatestId();
            while (resourceId != Onem2mDb.NULL_RESOURCE_ID) {
                Onem2mResource tempResource = getResource(resourceId);
                Child child = dbResourceTree.retrieveChildByName(containerResourceId, tempResource.getName());
                LOG.error("dumpContentInstancesForContainer: prev:{}, next:{} ", child.getPrevId(), child.getNextId());
                resourceId = child.getPrevId();
            }
        }
    }

    public List<String> findSubscriptionResources(RequestPrimitive onem2mRequest) {

        List<String> subscriptionResourceList = new ArrayList<String>();
        OldestLatest oldestLatest;

        String thisResourceId = onem2mRequest.getOnem2mResource().getResourceId();
        oldestLatest = dbResourceTree.retrieveOldestLatestByResourceType(thisResourceId,
                Onem2m.ResourceType.SUBSCRIPTION);
        if (oldestLatest != null) {
            String subscriptionResourceId = oldestLatest.getLatestId();
            while (!subscriptionResourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                subscriptionResourceList.add(subscriptionResourceId);
                // keep getting prev until NULL
                Onem2mResource onem2mResource = getResource(subscriptionResourceId);
                Child child = dbResourceTree.retrieveChildByName(thisResourceId, onem2mResource.getName());
                subscriptionResourceId = child.getPrevId();
            }
        }
        if (subscriptionResourceList.size() == 0) {
            String parentResourceId = onem2mRequest.getOnem2mResource().getParentId();
            oldestLatest = dbResourceTree.retrieveOldestLatestByResourceType(parentResourceId,
                    Onem2m.ResourceType.SUBSCRIPTION);
            if (oldestLatest != null) {
                String subscriptionResourceId = oldestLatest.getLatestId();
                while (!subscriptionResourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                    subscriptionResourceList.add(subscriptionResourceId);
                    // keep getting prev until NULL
                    Onem2mResource onem2mResource = getResource(subscriptionResourceId);
                    Child child = dbResourceTree.retrieveChildByName(parentResourceId, onem2mResource.getName());
                    subscriptionResourceId = child.getPrevId();
                }
            }
        }
        return subscriptionResourceList;
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
            //dataBroker.close();
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