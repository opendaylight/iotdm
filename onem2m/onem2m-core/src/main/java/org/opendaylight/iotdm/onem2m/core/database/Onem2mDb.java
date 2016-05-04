/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceAE;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceGroup;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContentInstance;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.*;
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
        return (dbResourceTree.retrieveCseByName(name) != null);
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

        JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.RESOURCE_ID, onem2mRequest.getResourceId());
        JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.RESOURCE_NAME, onem2mRequest.getResourceName());

        dbResourceTree.createCseByName(dbTxn, onem2mRequest.getResourceName(), onem2mRequest.getResourceId());

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = dbResourceTree.createResource(dbTxn, onem2mRequest,
                NULL_RESOURCE_ID, Onem2m.ResourceType.CSE_BASE);

        // cache the resource
        onem2mRequest.setOnem2mResource(onem2mResource);

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }

    private void updateParentLastModifiedTime(DbTransaction dbTxn, RequestPrimitive onem2mRequest,
                                              String parentResourceId,  JSONObject parentJsonContent) {
        JsonUtils.put(parentJsonContent, ResourceContent.LAST_MODIFIED_TIME,
                onem2mRequest.getResourceContent().getInJsonContent().optString(ResourceContent.CREATION_TIME));
        dbResourceTree.updateJsonResourceContentString(dbTxn,
                parentResourceId,
                parentJsonContent.toString());
    }

    private void adjustContainerCurrValues(DbTransaction dbTxn, RequestPrimitive onem2mRequest,
                                           String containerResourceId, JSONObject containerJsonContent) {

        JsonUtils.put(containerJsonContent, ResourceContainer.CURR_BYTE_SIZE, onem2mRequest.containerCbs);
        JsonUtils.put(containerJsonContent, ResourceContainer.CURR_NR_INSTANCES, onem2mRequest.containerCni);
        JsonUtils.put(containerJsonContent, ResourceContent.STATE_TAG, onem2mRequest.containerSt);
        dbResourceTree.updateJsonResourceContentString(dbTxn,
                containerResourceId,
                containerJsonContent.toString());
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

        String resourceType = Integer.toString(onem2mRequest
                .getResourceContent()
                .getInJsonContent()
                .optInt(ResourceContent.RESOURCE_TYPE));
        String resourceName = onem2mRequest.getResourceName();

        // see resourceAE for comments on why AE_IDs have these naming rules
        if (resourceType.contentEquals(Onem2m.ResourceType.AE)) {
            String from = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
            if (from == null) {
                if (resourceName == null) {
                    onem2mRequest.setResourceName("C" + onem2mRequest.getResourceId());
                }
                JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceAE.AE_ID, onem2mRequest.getResourceName());
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
                JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceAE.AE_ID, onem2mRequest.getResourceName());
            }
        }

        /**
         * The resource name should be filled in with the resource-id if the name is blank.
         */
        if (onem2mRequest.getResourceName() == null) {
            onem2mRequest.setResourceName(onem2mRequest.getResourceId());
        }

        if (!parentId.contentEquals(NULL_RESOURCE_ID)) {
            JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.PARENT_ID, parentId);
        }
        JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.RESOURCE_ID, onem2mRequest.getResourceId());
        JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.RESOURCE_NAME, onem2mRequest.getResourceName());

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

        // see if a content instance creation caused the container to be updated, note that the parent is the
        // container, and the request has the container's json object representing its resource content
        if (onem2mRequest.mustUpdateContainer) {
            this.adjustContainerCurrValues(dbTxn, onem2mRequest, parentId, onem2mRequest.getJsonResourceContent());
        }

        // update the lmt of the parent to be the creation time of the child being created
        if (!parentId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            updateParentLastModifiedTime(dbTxn, onem2mRequest, parentId, onem2mRequest.getJsonResourceContent());
        }

        // create a childEntry on the parent resourceId, <child-name, child-resourceId>
        dbResourceTree.createParentChildLink(dbTxn,
                parentId, // parent
                onem2mRequest.getResourceName(), // childName
                onem2mRequest.getResourceId(), // chileResourceId
                prevId, Onem2mDb.NULL_RESOURCE_ID); // siblings

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = dbResourceTree.createResource(dbTxn, onem2mRequest, parentId, resourceType);

        // now save this newly created resource
        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setJsonResourceContent(onem2mRequest.getOnem2mResource().getResourceContentJsonString());

        // now commit these to the data store
        return dbTxn.commitTransaction();
    }

    /**
     * The update resource is carried out by this routine.  The update uses the original JSON content in
     * onem2mRequest.getJSONResourceContent, then it cycles through each value in the new ResourceContent JSON obj
     * to see what has changed, and applies the change to the old resoruce.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     * @return successful update
     */
    public boolean updateResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        for (String newKey : JSONObject.getNames(onem2mRequest.getResourceContent().getInJsonContent())) {
            if (onem2mRequest.getResourceContent().getInJsonContent().isNull(newKey)) {
                onem2mRequest.getJsonResourceContent().remove(newKey);
            } else {
                JsonUtils.put(onem2mRequest.getJsonResourceContent(), newKey, onem2mRequest.getResourceContent().getInJsonContent().opt(newKey));
            }
        }

        DbTransaction dbTxn = new DbTransaction(dataBroker);

        dbResourceTree.updateJsonResourceContentString(dbTxn,
                onem2mRequest.getResourceId(),
                onem2mRequest.getJsonResourceContent().toString());

        boolean success = dbTxn.commitTransaction();

        Onem2mResource onem2mResource = getResource(onem2mRequest.getResourceId());
        onem2mRequest.setOnem2mResource(onem2mResource);

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
            String rt = containerOnem2mResource.getResourceType();
            if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {

                OldestLatest oldestLatest = dbResourceTree.retrieveOldestLatestByResourceType(
                        containerOnem2mResource.getResourceId(),
                        Onem2m.ResourceType.CONTENT_INSTANCE);
                if (oldestLatest == null) {
                    return null;
                }
                Onem2mResource onem2mResource = getResource(oldestLatest.getLatestId());
                if (onem2mResource != null) {
                    rt = onem2mResource.getResourceType();
                    if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
                        return onem2mResource;
                    }
                }
            }
        } else if (resourceName.contentEquals(ResourceContainer.OLDEST) || resourceName.contentEquals("oldest")) {
            String rt = containerOnem2mResource.getResourceType();
            if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {
                OldestLatest oldestLatest = dbResourceTree.retrieveOldestLatestByResourceType(
                        containerOnem2mResource.getResourceId(),
                        Onem2m.ResourceType.CONTENT_INSTANCE);
                if (oldestLatest == null) {
                    return null;
                }
                Onem2mResource onem2mResource = getResource(oldestLatest.getOldestId());
                if (onem2mResource != null) {
                    rt = onem2mResource.getResourceType();
                    if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
                        return onem2mResource;
                    }
                }
            }
        }
        return null;
    }

    private Onem2mResource checkForFanOutPoint(Onem2mResource groupOnem2mResource,
                                               String resourceName) {

        if (resourceName.contentEquals(ResourceGroup.FAN_OUT_POINT) || resourceName.equalsIgnoreCase("fanoutpoint")) {
            String rt = groupOnem2mResource.getResourceType();
            if (rt != null && rt.contentEquals(Onem2m.ResourceType.GROUP)) {
                // if parent's resourceType is group
                return groupOnem2mResource;
                // todo : do the group operaion
            }
        }
        return null;
    }

    /**
     * Class provides methods checking URI if is valid, if identifies local or
     * remote resource.
     * Implements methods for retrieving resource data from DB according to
     * the targetURI.
     */
    public final class CseBaseResourceLocator {
        private Onem2mCse cseBase = null;
        private boolean isStructured = false;
        private String targetURI = null;
        private boolean isLocalResource = true;
        private String[] hierarchyPath = null;
        private int hierarchyPathIndex = 0;
        private String remoteCseCseId = null;
        private String cseBaseName = null;
        private String cseBaseCseId = null;

        /**
         * Processes and validates URI.
         * @param targetURI The URI identifying resource (local or remote).
         * @throws IllegalArgumentException
         */
        CseBaseResourceLocator(String targetURI) throws IllegalArgumentException {

            if (null == targetURI || targetURI.isEmpty()) {
                throw new IllegalArgumentException("No targetURI passed");
            }

            this.targetURI = targetURI;
            String[] hierarchy = targetURI.split("/");
            String cseID = null;
            int hierarchyIndex = 0;

            if (targetURI.startsWith("//")) { // absolute (//FQDN/<CSE_ID>/...)
                if (hierarchy.length < 4) {
                    throw new IllegalArgumentException("Invalid absolute URI: " + targetURI);
                }

                // TODO check the FQDN ???

                cseID = hierarchy[3];
                hierarchyIndex = 4;
            } else if (targetURI.startsWith("/")) { // SP-Relative (/<CSE_ID>/...)
                if (hierarchy.length < 3) {
                    throw new IllegalArgumentException("Invalid SP-Relative URI: " + targetURI);
                }
                cseID = hierarchy[1];
                hierarchyIndex = 2;
            } else { // CSE-Relative
                if (hierarchy.length == 0) {
                    throw new IllegalArgumentException("Invalid CSE-Relative URI: " + targetURI);
                }
            }

            // If the URI contains CSE_ID then check if it's local
            if (null != cseID) {
                // SP-Relative, absolute
                // We have CSE_ID, retrieve particular CSEBase
                if (! Onem2mRouterService.getInstance().hasCseBaseCseId(cseID)) {
                    LOG.trace("Non-local resource URI: {}", targetURI);
                    this.isLocalResource = false;
                    this.remoteCseCseId = cseID;
                    return;
                } else {
                    this.cseBaseCseId = cseID;
                }
            }

            /*
             * Now need to distinguish if the URI is structured or unstructured
             * Current hierarchyIndex can point to:
             *  a) CSE name (structured)
             *  b) resourceID (unstructured)
             */
            if (Onem2mRouterService.getInstance().hasCseBaseName(hierarchy[hierarchyIndex])) {
                // a)
                if ((null != cseID) &&
                        (!Onem2mRouterService.getInstance().hasCseBaseNameCseId(hierarchy[hierarchyIndex], cseID))) {
                    throw new IllegalArgumentException("URI with CSE_NAME and CSE_ID mix-up: " + targetURI);
                }
                this.isStructured = true;
                this.cseBaseName = hierarchy[hierarchyIndex];
                hierarchyIndex++;
            } else {
                // b)
                this.isStructured = false;
            }

            this.hierarchyPathIndex = hierarchyIndex;
            this.hierarchyPath = hierarchy;
        }

        private Onem2mCse retrieveCseBase() {
            if (null != this.cseBase) {
                // cseBase already set
                return this.cseBase;
            }

            if (null != this.cseBaseName) {
                this.cseBase = dbResourceTree.retrieveCseByName(this.cseBaseName);
                if (null != this.cseBase) {
                    return this.cseBase;
                }
            }

            if (null != this.cseBaseCseId) {
                this.cseBase = dbResourceTree.retrieveCseByCseId(this.cseBaseCseId);
            }

            return this.cseBase;
        }

        public boolean isStructured() {
            return this.isStructured;
        }

        public boolean isLocalResource() {
            return this.isLocalResource;
        }

        public String getTargetURI() {
            return this.targetURI;
        }

        public String getRemoteCseCseId() { return this.remoteCseCseId; }

        /**
         * Returns resource identified by the URI if the resource is local and
         * if exists, null is returned otherwise.
         * @return Onem2mResource
         */
        protected Onem2mResource getResource() {
            if (! isLocalResource()) {
                return null;
            }

            Onem2mCse cseBase = null;
            Onem2mResource resource = null;
            if (this.hierarchyPathIndex >= this.hierarchyPath.length) {
                // the resource identified by URI is the CSE

                cseBase = this.retrieveCseBase();
                if (null == cseBase) {
                    LOG.trace("Can't get resource without cseBase");
                    return null;
                }

                resource = dbResourceTree.retrieveResourceById(cseBase.getResourceId());
                if (null == resource) {
                    LOG.error("Onem2m CSEBase without resource record in DB, URI: {}", getTargetURI());
                }
                return resource;
            }

            if (! isStructured()) {
                resource = dbResourceTree.retrieveResourceById(this.hierarchyPath[this.hierarchyPathIndex]);
                if (null == resource) {
                    LOG.trace("Resource with ID {} not found, URI: {}",
                              this.hierarchyPath[this.hierarchyPathIndex],
                              getTargetURI());
                }
                return resource;
            } else {

                cseBase = this.retrieveCseBase();
                if (null == cseBase) {
                    LOG.trace("Can't get resource without cseBase");
                    return null;
                }

                String resourceId = cseBase.getResourceId();
                Onem2mResource savedResource = null;
                for (int hierarchyIndex = this.hierarchyPathIndex;
                     hierarchyIndex < this.hierarchyPath.length;
                     hierarchyIndex++) {
                    resource = dbResourceTree.retrieveChildResourceByName(resourceId,
                                                                          this.hierarchyPath[hierarchyIndex]);
                    if (resource == null) {
                        // check "/latest" in the URI
                        resource = checkForLatestOldestContentInstance(savedResource,
                                                                       this.hierarchyPath[hierarchyIndex]);
                        if (resource == null) {
                            resource = checkForFanOutPoint(savedResource,
                                                           this.hierarchyPath[hierarchyIndex]);
                        }
                        if (resource == null) {
                            break;
                        }
                    }
                    resourceId = resource.getResourceId();
                    savedResource = resource;
                }
                return resource;
            }

           //  return null;
        }

        /**
         * Returns cseBase of the resource.
         * (This might be useful because we support more than one cseBase)
         * Returns the baseCSE resource if successful, null otherwise.
         * @return Onem2mCse
         */
        protected Onem2mCse getCseBase() {
            if (! isLocalResource()) {
                return null;
            }

            this.retrieveCseBase();

            if (null == this.cseBase) {
                // Walk all parents of the resource in hierarchy and get the cseBase of the resource
                Onem2mResource resource = getResource();
                while ((null != resource) && !resource.getResourceType().equals(Onem2m.ResourceType.CSE_BASE)) {
                    resource = dbResourceTree.retrieveResourceById(resource.getParentId());
                }

                if (null == resource) {
                    LOG.error("Failed to find cseBase of the resource identified by URI: {}", this.targetURI);
                    return null;
                }
                return dbResourceTree.retrieveCseByName(resource.getName());
            }

            return this.cseBase;
        }
    }

    public String getCSEid (String targetURI) {
        CseBaseResourceLocator locator = null;
        try {
            locator = new CseBaseResourceLocator(targetURI);
        } catch (IllegalArgumentException ex) {
            LOG.error("Invalid URI ({}), {}", targetURI, ex);
            return "";
        }

        Onem2mCse cse = locator.getCseBase();
        if (null == cse) {
            return "";
        }

        return cse.getResourceId();
    }

    public CseBaseResourceLocator createResourceLocator(String targetURI) {
        return new CseBaseResourceLocator(targetURI);
    }

    /**
     * Checks whether the URI points to local resource or not.
     * @param targetURI URI
     * @return true for local, false for remote resource
     */
    public Boolean isLocalResourceURI(String targetURI) {
        return new CseBaseResourceLocator(targetURI).isLocalResource();
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

        CseBaseResourceLocator locator = null;
        try {
            locator = new CseBaseResourceLocator(targetURI);
        } catch (IllegalArgumentException ex) {
            LOG.error("Invalid URI ({}), {}", targetURI, ex.toString());
            return false;
        }

        Onem2mResource onem2mResource = locator.getResource();
        if (onem2mResource == null)
            return false; // resource not found

        onem2mRequest.setResourceId(onem2mResource.getResourceId());
        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setJsonResourceContent(onem2mRequest.getOnem2mResource().getResourceContentJsonString());
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

    public String getChildResourceID(String cseid, String childName) {
        return dbResourceTree.retrieveChildResourceIDByName(cseid, childName);
    }

    private void deleteContentInstance(DbTransaction dbTxn,
                                       RequestPrimitive onem2mRequest,
                                       Onem2mResource containerResource) {

        String containerResourceId = containerResource.getResourceId();
        try {
            JSONObject containerResourceContent = new JSONObject(containerResource.getResourceContentJsonString());
            ResourceContainer.setCurrValuesForThisDeletedContentInstance(onem2mRequest,
                    containerResourceContent,
                    onem2mRequest.getJsonResourceContent().optInt(ResourceContentInstance.CONTENT_SIZE));
            adjustContainerCurrValues(dbTxn, onem2mRequest, containerResourceId, containerResourceContent);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", containerResource.getResourceContentJsonString(), e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
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
        String resourceType = onem2mRequest.getOnem2mResource().getResourceType();
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

        String resourceType = onem2mRequest.getOnem2mResource().getResourceType();
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
            while (!resourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                Onem2mResource tempResource = getResource(resourceId);
                Child child = dbResourceTree.retrieveChildByName(containerResourceId, tempResource.getName());
                LOG.error("dumpContentInstancesForContainer: prev:{}, next:{} ", child.getPrevId(), child.getNextId());
                resourceId = child.getNextId();
            }
            LOG.error("dumpContentInstancesForContainer: dumping latest to oldest: containerResourceUri:{}, containerId: {}, oldest={}, latest={}",
                    containerUri, containerResourceId,
                    containerOldestLatest.getOldestId(), containerOldestLatest.getLatestId());
            resourceId = containerOldestLatest.getLatestId();
            while (!resourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
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
    public static String trimURI(String uri) {
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
