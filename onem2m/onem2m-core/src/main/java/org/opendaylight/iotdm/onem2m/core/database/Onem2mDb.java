/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Child;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.ChildBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.ChildKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
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
    private static final String nullResourceId = "0";
    public static Onem2mDb getInstance() {
        if (db == null)
            db = new Onem2mDb();
        return db;
    }
    private Onem2mDb() {

    }

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
        dbResourceTree.createResource(dbTxn, onem2mRequest, nullResourceId);

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }

    /**
     * The create resource is carried out by this routine.  The onem2mRequest has the parameters in it to effect the
     * creation request.  The resource specific routines have in .../core.service/rest already vetted the parameters so
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

        /**
         * The resource name should be filled in with the resource-id if the name is blank.
         */
        String resourceName = onem2mRequest.getResourceName();
        if (resourceName == null) {
            onem2mRequest.setResourceName(onem2mRequest.getResourceId().toString());
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

            if (onem2mResource.getParentId() == nullResourceId) {
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
            if (resourceId == nullResourceId) {
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
        if (!parentResourceId.contentEquals(nullResourceId))
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

    private void createResourceURIs(List<String> resourceURIList, int level, int numLevels, int numPerLevel, String resourceName) {
        if (level == numLevels) { return; }
        String baseResourceName = resourceName + level;
        for (int i = 0; i < numPerLevel; i++) {
            String name = baseResourceName + "-" + i;
            resourceURIList.add(name);
            String hierarchy[] = name.split("/"); // split the URI into its hierarchy of path component strings
            createResourceURIs(resourceURIList, level + 1, numLevels, numPerLevel, name + "/" + hierarchy[hierarchy.length-1] + "-");
        }
    }

    private void createNodes(List<String> resourceList, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse,
                             int level, int numLevels, int numPerLevel, String resourceName,  String parentResourceId) {
        if (level == numLevels) { return; }
        String baseResourceName = resourceName + level;
        for (int i = 0; i < numPerLevel; i++) {
            onem2mRequest.setOnem2mResource(dbResourceTree.retrieveResourceById(parentResourceId));
            String name = baseResourceName + "-" + i;
            onem2mRequest.setResourceName(name);
            while (createResource(onem2mRequest, onem2mResponse) == false) {
                LOG.warn("createResource: {} failed ... retrying", name);
            }
            resourceList.add(onem2mRequest.getResourceId());
            createNodes(resourceList, onem2mRequest, onem2mResponse,
                    level + 1, numLevels, numPerLevel, name + "-", onem2mRequest.getResourceId());
        }
    }

    /**
     * Use the actual API's that the service/rest layer would use to create the resources and subsequently find them
     * This routine allows any hierarchy to be created and tested.  I also need to try some deletion tests so TODO
     * need to figure out how to tests those scenarios.
     * @param cseName
     * @param numLevels
     * @param numPerLevel
     * @param numAttrs
     * @param numAttrSets
     * @param numMembersPerAttrSet
     * @return
     */
    public boolean testTreeUnderCse(String cseName, int numLevels, int numPerLevel) { //,
                                    //int numAttrs, int numAttrSets, int numMembersPerAttrSet) {

        int numInTree;
        long startTime, endTime, delta;

        Onem2mCse onem2mCse = dbResourceTree.retrieveCseByName(cseName);
        if (onem2mCse == null) {
            LOG.error("Cannot find cse: {}", cseName);
            return false;
        }
        RequestPrimitive onem2mRequest = new RequestPrimitiveProcessor();
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        /**
         * This creates a hierarchy of resources.  The name of the resource at each level is a unique name
         * in the format Level-level-instance-nextlevel-instance-nextnextlevel-instance ...
         * If you want to test to see if a name exists at a certain level, you have two choices.
         * This routine fills in a resourceList with the resourceId's that were created.  I verify that the
         * correct number of resources is returned based on the numLevels and numPerLevel
         * Also, you can look for resources by the hierarchical method by building up a URI string of resource names.
         */

        // compute how many resources that theoretically should be created
        int numResources = 0;
        for (int i = 0; i < numLevels; i++) {
            numResources += Math.pow(numPerLevel,i+1);
        }

        // create the hierarchy, note that resourceList is filled in with each resourceId for later non-h retrieval
        List<String> resourceList = new ArrayList<String>(numResources);

        startTime = System.nanoTime();
        createNodes(resourceList, onem2mRequest, onem2mResponse, 0, numLevels, numPerLevel, "Level-", onem2mCse.getResourceId());
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        LOG.error("testTreeUnderCse: time to create ... numLevels: {}, numPerLevel: {}, numResources: {}, delta: {}, K/ms: {}",
                numLevels, numPerLevel, numResources, delta, (numResources*1000)/delta);
        // verify all were actually created
        if (numResources != resourceList.size()) {
            LOG.error("testTreeUnderCse: mismatched resource count ... numLevels: {}, numPerLevel: {}, numExpected: {}, numCreated: {}",
                    numLevels, numPerLevel, numResources, resourceList.size());
        }
        numInTree = dbResourceTree.numResourcesInTree();
        if (numInTree != numResources + 1) { // count the cse resoruce too
            LOG.error("testTreeUnderCse: resource count wrong ... numLevels: {}, numPerLevel: {}, numExpected: {}, numCreated: {}",
                    numLevels, numPerLevel, numResources + 1, numInTree);
            return false;
        }

        // use the non-hierarchical method to look up the resourceId's
        startTime = System.nanoTime();
        for (String resourceId : resourceList) {
            String uri = "/" + cseName + "/" + resourceId;
            onem2mRequest.setPrimitive(RequestPrimitive.TO, uri);
            onem2mRequest.setResourceName(null);
            //LOG.info("Lookup uri: {}", uri);
            if (!FindResourceUsingURI(onem2mRequest, onem2mResponse)) {
                LOG.error("testTreeUnderCse: cannot find expected uri ... numLevels: {}, numPerLevel: {}, numExpected: {}, numCreated: {}, uri: {}",
                        numLevels, numPerLevel, numResources, resourceList.size(), uri);
                return false;
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        LOG.error("testTreeUnderCse: time to nhRead ... numLevels: {}, numPerLevel: {}, numResources: {}, delta: {}, K/ms: {}",
                numLevels, numPerLevel, numResources, delta, (numResources*1000)/delta);

        // construct the list of hierarchical resource URI's
        List<String> resourceURIList = new ArrayList<String>(numResources);
        createResourceURIs(resourceURIList, 0, numLevels, numPerLevel, "/" + cseName + "/Level-");
        if (numResources != resourceURIList.size()) {
            LOG.error("testTreeUnderCse: mismatched resource uri count ... numLevels: {}, numPerLevel: {}, numExpected: {}, numCreated: {}",
                    numLevels, numPerLevel, numResources, resourceURIList.size());
        }

        // use the hierarchical method to look up the resourceId's
        startTime = System.nanoTime();
        for (String uri : resourceURIList) {
            onem2mRequest.setPrimitive(RequestPrimitive.TO, uri);
            onem2mRequest.setResourceName(null);
            //LOG.info("Lookup uri: {}", uri);
            if (!FindResourceUsingURI(onem2mRequest, onem2mResponse)) {
                LOG.error("testTreeUnderCse: cannot find expected uri ... numLevels: {}, numPerLevel: {}, numExpected: {}, numCreated: {}, uri: {}",
                        numLevels, numPerLevel, numResources, resourceURIList.size(), uri);
                return false;
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        LOG.error("testTreeUnderCse: time to hRead ... numLevels: {}, numPerLevel: {}, numResources: {}, delta: {}, K/ms: {}",
                numLevels, numPerLevel, numResources, delta, (numResources*1000)/delta);

        //dumpDataStoreToLog();

        // TODO: write a bunch of delete tests but for now just delete the level 0 resources and the rest should go
        startTime = System.nanoTime();
        for (int i = 0; i < numPerLevel; ++i) {
            String uri = "/" + cseName + "/Level-0-" + i;

            onem2mRequest.setPrimitive(RequestPrimitive.TO, uri);
            onem2mRequest.setResourceName(null);
            //LOG.info("Lookup uri: {}", uri);
            if (!FindResourceUsingURI(onem2mRequest, onem2mResponse)) {
                LOG.error("testTreeUnderCse: cannot find expected uri ... numLevels: {}, numPerLevel: {}, numExpected: {}, numCreated: {}, uri: {}",
                        numLevels, numPerLevel, numResources, resourceURIList.size(), uri);
                return false;
            }
            while (DeleteResourceUsingURI(onem2mRequest, onem2mResponse) == false) {
                LOG.error("Delete failed for uri: {}, retrying ...", uri);
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        LOG.error("testTreeUnderCse: time to delete ... numLevels: {}, numPerLevel: {}, numResources: {}, delta: {}, K/ms: {}",
                numLevels, numPerLevel, numResources, delta, (numResources*1000)/delta);

        // use the non-hierarchical method to look up the resourceId's, they should ALL be gone now
        for (String resourceId : resourceList) {
            String uri = "/" + cseName + "/" + resourceId;
            onem2mRequest.setPrimitive(RequestPrimitive.TO, uri);
            onem2mRequest.setResourceName(null);
            //LOG.info("Lookup uri: {}", uri);
            if (FindResourceUsingURI(onem2mRequest, onem2mResponse)) {
                LOG.error("testTreeUnderCse: cannot find expected uri ... numLevels: {}, numPerLevel: {}, numExpected: {}, numCreated: {}, uri: {}",
                        numLevels, numPerLevel, numResources, resourceList.size(), uri);
                return false;
            }
        }

        numInTree = dbResourceTree.numResourcesInTree();
        if (numInTree != 1) {
            LOG.error("testTreeUnderCse: resource count wrong ... numLevels: {}, numPerLevel: {}, numExpected: {}, numCreated: {}",
                    numLevels, numPerLevel, 1, numInTree);
            return false;
        }

        dumpDataStoreToLog();

        return true;
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