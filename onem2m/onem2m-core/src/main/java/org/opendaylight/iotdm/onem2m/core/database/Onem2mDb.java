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
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContentInstance;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Child;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OneM2M data store uses the Binding Aware data store with transaction chaining.  See teh ODL coretutorials
 * dsbenchmark for details.  The data store implements the Resource tree in the onenm2m yang model.  It is a very
 * generic data/meta model.  It enables all of the onem2m resource types to be CRUDed.  The semantics of how these
 * resources are enforced in the onem2m-core/service layer.  The onenm2m-core/REST layer is used as the API into the
 * system.  The client (Onem2mRequestPrimitiveClient) is how the onenm2m-protocols adapt msgs to the onem2m protocol
 * independent format.  The message is then sent to the onem2m service.  See Onem2m.serviceRequest().
 *
 * The resource tree holds a list of cseBase's.  Onem2m provisions for one cseBase but I allowed for more just in case
 * more flexibility is reequired.  Examples of needing more might be customer partitioning.  Maybe a virtual tree is
 * given to each IOT network.  I dont know if this is realistic but the capability is there.  Also, there maybe a
 * feature that some internal application wishes to implement using a resoruce tree, so it can provision its own
 * cseBase if that makes sense.
 *
 * Each new cseBase provisioned is allocated a resource which is the root/base for its resoruce tree.  When any new
 * resource is provisioned unser the cseBase, it is allocated a new resource and is added as a child resoruce to the
 * cseBase resource, and the new resource's parentID is set to point to the cseBase.  This mechanism allows a tree
 * to be built.
 *
 * Accessing the resources.  A resource can be accessed by its hierarchical name or its non-hierarchical name.  The
 * latter sits directly .... add more stuff here TODO
 */

/**
 * Thoughts on interfaces to the data store.
 * 1) New Cse ... initial creation
 * 2) Add a new resource ...
 * 2a) Find the parent resourceId
 * 2b) add a new resource
 * 2c) add a child to the parent resource child list
 * 2d) update an attr or attr set in the parent resource like MOD_TIME
 * 2e) in 2b) make sure set the parentId to the parentResource
 *
 * 3) delete a resource, this is a logical tree so need to do work to delete below the hierarchy
 * 3a) find it, the service layer will need to read its children for subscription notifications
 * 3b) also, will need to recurse the tree and delete resources starting from leafs, could be tricky
 *
 * 4) notes on resource creation
 * 4a) a resource needs attr list, attsets plus members, child entry
 * 4b) where should these sub structures be filled in
 * 4c) the onem2mRequest has the primitives, who converts these to the db structures?
 *
 * 5) In the service layer, validate the primitives, fill in any extra that are required, then call a db routine
 *    to effect the transaction ..the db routine will hide the db implementation
 */
public class Onem2mDb implements TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mDb.class);
    private BindingTransactionChain bindingTransactionChain;
    private static Onem2mDb db;
    private static DbResourceTree dbResourceTree;
    private static final String NULL_RESOURCE_ID = "0";

    /**
     * Allows other parts of the system to access the one and only instance of hte "datastore" object
     * @return
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
     * @param bindingDataBroker
     */
    public void initializeDatastore(DataBroker bindingDataBroker) {
        this.bindingTransactionChain = bindingDataBroker.createTransactionChain(this);
        dbResourceTree = new DbResourceTree(bindingTransactionChain);
        LOG.info("bindingTxChain: {}", this.bindingTransactionChain);
        dbResourceTree.initializeDatastore();
    }

    /**
     * For now I am making this a String even though it is a number.  Also, I experimented making the id a
     * random number (and verifying that NO resource in the system uses the same number by looking it up in the db.
     * The reason: I was curious about attacks if I made the number atomically increasing from some starting base.
     * I can keep stats on "missed" non-hierarchical resource operations, and if they get out-of-control, maybe the
     * SDN portion of the controller can shut down the offender.  I'll leave it like this for the time being, and
     * I'll experiment later with performance and my other concerns.  The key is to keep the size of the resourceId
     * small as constrained devices will want to use the non-hierarchical URI's so the smaller the better.
     *
     * TODO: figure out my strategy here.
     * @return
     */
    private String generateResourceId() {
        Integer intResourceId;
        do {
            intResourceId = ThreadLocalRandom.current().nextInt(1, 1000000000); // 9 digit random id
        } while (dbResourceTree.retrieveResourceById(intResourceId.toString()) != null); // make sure it is not used already
        return intResourceId.toString();
    }

    public boolean FindCseByName(String name) {
        return (dbResourceTree.retrieveCseByName(name) != null) ? true : false;
    }

    /**
     * The cse is a special resource, its the root of the tree, and all resources are created under it.  Note that
     * it is possible to create and initalize multiple cse's but for now, we add one hard coded cse in the
     * Onem2mCoreProvider.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public boolean createCseResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // generate a unique id for this new resource
        onem2mRequest.setResourceId(generateResourceId());

        // allocate a transaction, for the series of updates and creates to the data store
        DbTransaction dbTxn = new DbTransaction(bindingTransactionChain);

        dbResourceTree.createCseByName(dbTxn, onem2mRequest.getResourceName(), onem2mRequest.getResourceId());

        // get current date/time, set the parent->modeTime, and the child->orig/create_time
        // Date now = new Date();
        // do a merge on the List<attr> of the parent for the MOD_TIME attr

        // now create the resource with the attributes stored in the onem2mRequest
        dbResourceTree.createResource(dbTxn, onem2mRequest, NULL_RESOURCE_ID);

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }

    /**
     * The create resource is carried out by this routine.  The onem2mRequest has the parameters in it to effect the
     * creation request.  The resource specific routines have in .../core/resource already vetted the parameters so
     * its just a matter of adding the resource to the data store.
     *
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private void internalCreateResource(DbTransaction dbTxn,
                                        RequestPrimitive onem2mRequest,
                                        ResponsePrimitive onem2mResponse) {

        /**
         * The resource name should be filled in with the resource-id if the name is blank.
         */
        String resourceName = onem2mRequest.getResourceName();
        if (resourceName == null) {
            onem2mRequest.setResourceName(onem2mRequest.getResourceId());
        }

        // get current date/time, set the parent->modeTime, and the child->orig/create_time
        // Date now = new Date();
        // do a merge on the List<attr> of the parent for the MOD_TIME attr

        // create a childEntry on the parent resourceId, <child-name, child-resourceId>
        dbResourceTree.createParentChildLink(dbTxn,
                onem2mRequest.getOnem2mResource().getResourceId(), // parent
                onem2mRequest.getResourceName(), // childName
                onem2mRequest.getResourceId()); // chileResourceId

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = dbResourceTree.createResource(dbTxn,
                onem2mRequest, onem2mRequest.getOnem2mResource().getResourceId());

        // now save this newly created resource
        onem2mRequest.setOnem2mResource(onem2mResource);
    }
    /**
     * The create resource is carried out by this routine.  The onem2mRequest has the parameters in it to effect the
     * creation request.  The resource specific routines have in .../core/resource already vetted the parameters so
     * its just a matter of adding the resource to the data store.
     *
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public boolean createResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // generate a unique id for this new resource
        onem2mRequest.setResourceId(generateResourceId());

        // allocate a transaction, for the series of updates and creates to the data store
        DbTransaction dbTxn = new DbTransaction(bindingTransactionChain);

        internalCreateResource(dbTxn, onem2mRequest, onem2mResponse);

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }

    /**
     * The contentInstance resource is special in that it requires the parent container to be involved.
     * 1) if maxByteSize and maxNrInstances are supported, then those limits have to be enforced.
     * 2) the latest/oldest in the container have to be maintained, as well as next/prev in the content instances
     *
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public boolean createContentInstanceResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        DbAttr contentInstanceAttrs = onem2mRequest.getResourceContent().getDbAttrList();

        // generate a unique id for this new content instance resource
        String contentInstanceResourceId = generateResourceId();

        // get the parent container resource
        String containerId = onem2mRequest.getOnem2mResource().getResourceId();
        Onem2mResource containerResource = dbResourceTree.retrieveResourceById(containerId);

        // find the latest/oldest
        Attr latest = dbResourceTree.retrieveAttrByName(containerId, ResourceContainer.LATEST);
        Attr oldest = dbResourceTree.retrieveAttrByName(containerId, ResourceContainer.OLDEST);
        Attr sibling = null;
        if (!latest.getValue().contentEquals(NULL_RESOURCE_ID)) {
            sibling = dbResourceTree.retrieveAttrByName(latest.getValue(), ResourceContentInstance.NEXT);
        }

        // store the contentInstance id in the request in prep for creation
        onem2mRequest.setResourceId(contentInstanceResourceId);

        // allocate a transaction, for the series of updates and creates to the data store
        DbTransaction dbTxn = new DbTransaction(bindingTransactionChain);

        // maintain the links from container to content instances
        if (latest.getValue().contentEquals(NULL_RESOURCE_ID)) {
            // TODO: investigate updating the set of attrs for the container as a group
            dbResourceTree.updateAttr(dbTxn, containerId, ResourceContainer.LATEST, contentInstanceResourceId);
            dbResourceTree.updateAttr(dbTxn, containerId, ResourceContainer.OLDEST, contentInstanceResourceId);
            dbResourceTree.updateAttr(dbTxn, containerId, ResourceContainer.CURR_NR_INSTANCES, "1");
            String currByteSize = contentInstanceAttrs.getAttr(ResourceContainer.CURR_BYTE_SIZE);
            dbResourceTree.updateAttr(dbTxn, containerId, ResourceContainer.CURR_BYTE_SIZE,currByteSize);

            // need to update next/prev to NULL for the contentInstance attrs
            contentInstanceAttrs.setAttr(ResourceContentInstance.NEXT, NULL_RESOURCE_ID);
            contentInstanceAttrs.setAttr(ResourceContentInstance.PREV, NULL_RESOURCE_ID);

        } else {
            if (oldest.getValue().contentEquals(latest.getValue())) {
                dbResourceTree.updateAttr(dbTxn, containerId, ResourceContainer.OLDEST, contentInstanceResourceId);
            }
            dbResourceTree.updateAttr(dbTxn, containerId, ResourceContainer.LATEST, contentInstanceResourceId);
            dbResourceTree.updateAttr(dbTxn, sibling.getValue(), ResourceContentInstance.NEXT, contentInstanceResourceId);
            contentInstanceAttrs.setAttr(ResourceContentInstance.PREV, sibling.getValue());
        }
/*
        // enforce max byte and nr instances rules
        if (latest.getValue() == NULL_RESOURCE_ID) {
            dbResourceTree.updateAttr(dbTxn, containerId, ResourceContainer.CURR_NR_INSTANCES, "1");
            String currByteSize = contentInstanceAttrs.getAttr(ResourceContainer.CURR_BYTE_SIZE);
            dbResourceTree.updateAttr(dbTxn, containerId, ResourceContainer.CURR_BYTE_SIZE,currByteSize);
        } else {

        }
        */
        // create the contentInstance
        internalCreateResource(dbTxn, onem2mRequest, onem2mResponse);

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }

    public Boolean FindResourceUsingIdAndName(String resourceId, String resourceName) {

        Onem2mResource onem2mResource = dbResourceTree.retrieveChildResourceByName(resourceId, resourceName);
        return (onem2mResource != null);
    }

    public Boolean FindResourceUsingURI(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String targetURI = onem2mRequest.getPrimitive((RequestPrimitive.TO));
        assert(targetURI != null); // go find why we got here with a null TO attr, checked in rest/service layer

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
            return true;
        }

        Onem2mResource onem2mResource = null;

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
                    break;
                }
                resourceId = onem2mResource.getResourceId();
            }
        }

        if (onem2mResource == null)
            return false; // resource not found

        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setDbAttrs(new DbAttr(onem2mRequest.getOnem2mResource().getAttr()));
        return true;
    }

    /**
     * Using the resourceId, build the non hierarchical name of the path.
     * @param resourceId
     * @return
     */
    public String GetNonHierarchicalNameForResource(String resourceId) {

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
     * Uinsg the resourceId, traverse up the hierarchy till reach the root building the path.
     * @param resourceId
     * @return
     */
    public String GetHierarchicalNameForResource(String resourceId) {
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
     * @param startResourceId
     * @param limit
     * @return
     */
    public List<String> GetHierarchicalResourceList(String startResourceId, int limit) {

        Onem2mResource onem2mResource = dbResourceTree.retrieveResourceById(startResourceId);
        if (onem2mResource == null || limit < 1) {
            return null;
        }
        List<String> resourceList = new ArrayList<String>(limit);
        resourceList.add(startResourceId);
        if (resourceList.size() >= limit) {
            return resourceList;
        }
        // tack new resources onto the end of resourceList as the children are read
        for (String resourceId : resourceList) {
            onem2mResource = dbResourceTree.retrieveResourceById(resourceId);
            List<Child> childResourceList = onem2mResource.getChild();
            for (Child childResource : childResourceList) {
                resourceList.add(childResource.getResourceId());
                if (resourceList.size() >= limit) {
                    return resourceList;
                }
            }
        }

        return resourceList;
    }

    /**
     * Return the resource
     * @param resourceId
     * @return
     */
    public Onem2mResource GetResource(String resourceId) {
        return dbResourceTree.retrieveResourceById(resourceId);
    }

    /**
     * Deletes are idempotent.  This routine is called after the requestProcessor has grabbed all info it needed
     * for the delete operation so now it is a bulk recursive delete.
     * @param onem2mRequest
     * @param onem2mResponse
     * @return
     */
    public Boolean DeleteResourceUsingURI(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // if resource not found, then quietly return OK, as the net result is the resource is gone
        if (onem2mRequest.getOnem2mResource() == null)
            return true;

        // save the parent
        String parentResourceId = onem2mRequest.getOnem2mResource().getParentId();
        // cache this resource to be deleted
        String thisResourceId = onem2mRequest.getOnem2mResource().getResourceId();
        String thisResourceName = onem2mRequest.getOnem2mResource().getName();

        // build a 'to be Deleted list' by walking the hierarchy
        List<String> resourceIdList = new ArrayList<String>();
        dbResourceTree.hierarchicalFindResource(thisResourceId, resourceIdList);

        // now in a transaction, smoke all the resources under this ResourceId
        DbTransaction dbTxn = new DbTransaction(bindingTransactionChain);
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

    public void dumpDataStoreToLog() {
        dbResourceTree.dumpRawTreeToLog();
        dbResourceTree.dumpHierarchicalTreeToLog();
    }

    public void cleanupDataStore() {
        dbResourceTree.initializeDatastore(); // reinitialize the data store.
    }

    /**
     * The URI can be /cseBase/x/y/z/, and this routine turns it into cseBase/x/y/z ie. strip leading and trailing /
     * @param uri
     * @return
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

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain,
                                         AsyncTransaction<?, ?> transaction, Throwable cause) {
        LOG.error("Broken chain {} in Db, transaction {}, cause {}",
                chain, transaction.getIdentifier(), cause);
        assert(false);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> chain) {
        LOG.info("Db closed successfully, chain {} BUT WHY, closed unintentionally", chain);
        assert(false);
    }
}