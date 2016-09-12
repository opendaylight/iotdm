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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.helper.Consumer;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.resource.*;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
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
    //private static DbResourceTree dbResourceTree;
    public static final String NULL_RESOURCE_ID = "0";
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
        //dbResourceTree = new DbResourceTree(dataBroker);
    }

    /* When we turn on persistence, we will need to store this too */

    private String generateResourceId() {

        String b36ResourceId;
        int r = nextId.incrementAndGet();
        b36ResourceId = Integer.toString(r, 36);
        //b36ResourceId = Integer.toString(r);
        return b36ResourceId;
    }


    public List<String> getCseList(ResourceTreeReader trc) {
        return trc.getCseList();
    }

    /**
     * Find the cse using its name
     * @param name cse name
     * @return found or not
     */
    public boolean findCseByName(ResourceTreeReader trc, String name) {
        return (trc.retrieveCseByName(name) != null);
    }

    /**
     * The cse is a special resource, its the root of the tree, and all resources are created under it.
     * @param twc Database writer interface
     * @param onem2mRequest  request
     * @param onem2mResponse response
     * @return successful db create
     */
    public boolean createCseResource(ResourceTreeWriter twc, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // generate a unique id for this new resource
        onem2mRequest.setResourceId(generateResourceId());


        JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.RESOURCE_ID, onem2mRequest.getResourceId());
        JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.RESOURCE_NAME, onem2mRequest.getResourceName());

        if (!twc.createCseByName(onem2mRequest.getResourceName(), onem2mRequest.getResourceId())) return false;

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = twc.createResource(onem2mRequest,
                NULL_RESOURCE_ID, Onem2m.ResourceType.CSE_BASE);
        if (onem2mResource == null) return false;

        // cache the resource
        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setJsonResourceContent(onem2mRequest.getOnem2mResource().getResourceContentJsonString());

        return true;
    }

    private boolean updateParentLastModifiedTime(ResourceTreeWriter twc, RequestPrimitive onem2mRequest,
                                                 String parentResourceId, JSONObject parentJsonContent) {
        JsonUtils.put(parentJsonContent, ResourceContent.LAST_MODIFIED_TIME,
                onem2mRequest.getResourceContent().getInJsonContent().optString(ResourceContent.CREATION_TIME));
        return twc.updateJsonResourceContentString(parentResourceId,
                parentJsonContent.toString());
    }

    private boolean adjustContainerCurrValues(ResourceTreeWriter twc, RequestPrimitive onem2mRequest,
                                              String containerResourceId, JSONObject containerJsonContent) {

        JsonUtils.put(containerJsonContent, ResourceContainer.CURR_BYTE_SIZE, onem2mRequest.containerCbs);
        JsonUtils.put(containerJsonContent, ResourceContainer.CURR_NR_INSTANCES, onem2mRequest.containerCni);
        JsonUtils.put(containerJsonContent, ResourceContent.STATE_TAG, onem2mRequest.containerSt);
        return twc.updateJsonResourceContentString(
                containerResourceId,
                containerJsonContent.toString());
    }

    /**
     * The create resource is carried out by this routine.  The onem2mRequest has the parameters in it to effect the
     * creation request.  The resource specific routines have in .../core/resource already vetted the parameters so
     * its just a matter of adding the resource to the data store.
     * @param twc Database writer interface
     * @param onem2mRequest  request
     * @param onem2mResponse response
     * @return successful db create
     */
    public boolean createResource(ResourceTreeWriter twc, ResourceTreeReader trc, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // generate a unique id for this new resource
        onem2mRequest.setResourceId(generateResourceId());

        Onem2mResource parentOnem2mResource = onem2mRequest.getOnem2mResource();
        String parentId = parentOnem2mResource.getResourceId();

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
                    String removedHead = splitStrins[splitStrins.length - 1];
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

        if (!twc.initializeElementInParentList(resourceType, parentId,
                onem2mRequest.getResourceId(), onem2mRequest.getResourceName())) return false;

        // update the lmt of the parent to be the creation time of the child being created
        if (!parentId.equals(Onem2mDb.NULL_RESOURCE_ID)) {
            if (!updateParentLastModifiedTime(twc, onem2mRequest, parentId, onem2mRequest.getJsonResourceContent()))
                return false;
        }


        if (!parentId.contentEquals(NULL_RESOURCE_ID)) {
            JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.PARENT_ID, parentId);
        }
        JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.RESOURCE_ID, onem2mRequest.getResourceId());
        JsonUtils.put(onem2mRequest.getResourceContent().getInJsonContent(), ResourceContent.RESOURCE_NAME, onem2mRequest.getResourceName());

        // see if a content instance creation caused the container to be updated, note that the parent is the
        // container, and the request has the container's json object representing its resource content
        if (onem2mRequest.mustUpdateContainer) {
            if (!this.adjustContainerCurrValues(twc, onem2mRequest, parentId, onem2mRequest.getJsonResourceContent()))
                return false;
        }


        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = twc.createResource(onem2mRequest, parentId, resourceType);
        if (onem2mResource == null) return false;

        // now save this newly created resource
        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setJsonResourceContent(onem2mRequest.getOnem2mResource().getResourceContentJsonString());

        return true;
    }

    /**
     * The update resource is carried out by this routine.  The update uses the original JSON content in
     * onem2mRequest.getJSONResourceContent, then it cycles through each value in the new ResourceContent JSON obj
     * to see what has changed, and applies the change to the old resoruce.
     *
     * @param twc database writer interface
     * @param trc database reader interface
     * @param onem2mRequest  request
     * @param onem2mResponse response
     * @return successful update
     */
    public boolean updateResource(ResourceTreeWriter twc, ResourceTreeReader trc, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        for (String newKey : JSONObject.getNames(onem2mRequest.getResourceContent().getInJsonContent())) {
            if (onem2mRequest.getResourceContent().getInJsonContent().isNull(newKey)) {
                onem2mRequest.getJsonResourceContent().remove(newKey);
            } else {
                JsonUtils.put(onem2mRequest.getJsonResourceContent(), newKey, onem2mRequest.getResourceContent().getInJsonContent().opt(newKey));
            }
        }

        if (!twc.updateJsonResourceContentString(
                onem2mRequest.getResourceId(),
                onem2mRequest.getJsonResourceContent().toString())) return false;

        Onem2mResource onem2mResource = getResource(trc, onem2mRequest.getResourceId());
        onem2mRequest.setOnem2mResource(onem2mResource);
        return true;
    }

    /**
     * this method is used to update subscription during the process of sending notification
     *
     * @param twc database writer interface
     * @param resourceID        resourceID of the subscription resource
     * @param updatedJsonString the new json String
     * @return true if successfully updated
     */
    public boolean updateSubscriptionResource(ResourceTreeWriter twc, String resourceID, String updatedJsonString) {

        return twc.updateJsonResourceContentString(resourceID,
                updatedJsonString);
    }

    /**
     * Locate resource in db using the target resource id at this level and a name
     *
     * @param trc database reader interface
     * @param resourceId   id at this level
     * @param resourceName name at this level
     * @return found status
     */
    public Boolean findResourceUsingIdAndName(ResourceTreeReader trc, String resourceId, String resourceName) {

        Onem2mResource onem2mResource = trc.retrieveChildResourceByName(resourceId, resourceName);
        return (onem2mResource != null);
    }

    private Onem2mResource checkForLatestOldestContentInstance(ResourceTreeReader trc, Onem2mResource containerOnem2mResource,
                                                               String resourceName) {

        if (resourceName.contentEquals(ResourceContainer.LATEST) || resourceName.contentEquals("latest")) {
            String rt = containerOnem2mResource.getResourceType();
            if (rt != null && rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {

                OldestLatest oldestLatest = trc.retrieveOldestLatestByResourceType(
                        containerOnem2mResource.getResourceId(),
                        Onem2m.ResourceType.CONTENT_INSTANCE);
                if (oldestLatest == null) {
                    return null;
                }
                Onem2mResource onem2mResource = getResource(trc, oldestLatest.getLatestId());
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
                OldestLatest oldestLatest = trc.retrieveOldestLatestByResourceType(
                        containerOnem2mResource.getResourceId(),
                        Onem2m.ResourceType.CONTENT_INSTANCE);
                if (oldestLatest == null) {
                    return null;
                }
                Onem2mResource onem2mResource = getResource(trc, oldestLatest.getOldestId());
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

        private Onem2mCse retrieveCseBase(ResourceTreeReader trc) {
            if (null != this.cseBase) {
                // cseBase already set
                return this.cseBase;
            }

            if (null != this.cseBaseName) {
                this.cseBase = trc.retrieveCseByName(this.cseBaseName);
                if (null != this.cseBase) {
                    return this.cseBase;
                }
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
        protected Onem2mResource getResource(ResourceTreeReader trc) {
            if (! isLocalResource()) {
                return null;
            }

            Onem2mCse cseBase = null;
            Onem2mResource resource = null;
            if (this.hierarchyPathIndex >= this.hierarchyPath.length) {
                // the resource identified by URI is the CSE

                cseBase = this.retrieveCseBase(trc);
                if (null == cseBase) {
                    LOG.trace("Can't get resource without cseBase");
                    return null;
                }

                resource = trc.retrieveResourceById(cseBase.getResourceId());
                if (null == resource) {
                    LOG.error("Onem2m CSEBase without resource record in DB, URI: {}", getTargetURI());
                }
                return resource;
            }

            if (! isStructured()) {
                resource = trc.retrieveResourceById(this.hierarchyPath[this.hierarchyPathIndex]);
                if (null == resource) {
                    LOG.trace("Resource with ID {} not found, URI: {}",
                              this.hierarchyPath[this.hierarchyPathIndex],
                              getTargetURI());
                }
                return resource;
            } else {

                cseBase = this.retrieveCseBase(trc);
                if (null == cseBase) {
                    LOG.trace("Can't get resource without cseBase");
                    return null;
                }

                String resourceId = cseBase.getResourceId();
                Onem2mResource savedResource = null;
                for (int hierarchyIndex = this.hierarchyPathIndex;
                     hierarchyIndex < this.hierarchyPath.length;
                     hierarchyIndex++) {
                    resource = trc.retrieveChildResourceByName(resourceId,
                                                                          this.hierarchyPath[hierarchyIndex]);
                    if (resource == null) {
                        // check "/latest" in the URI
                        resource = checkForLatestOldestContentInstance(trc, savedResource,
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
        protected Onem2mCse getCseBase(ResourceTreeReader trc) {
            if (! isLocalResource()) {
                return null;
            }

            this.retrieveCseBase(trc);

            if (null == this.cseBase) {
                // Walk all parents of the resource in hierarchy and get the cseBase of the resource
                Onem2mResource resource = getResource(trc);
                while ((null != resource) && !resource.getResourceType().equals(Onem2m.ResourceType.CSE_BASE)) {
                    resource = trc.retrieveResourceById(resource.getParentId());
                }

                if (null == resource) {
                    LOG.error("Failed to find cseBase of the resource identified by URI: {}", this.targetURI);
                    return null;
                }
                return trc.retrieveCseByName(resource.getName());
            }

            return this.cseBase;
        }
    }

    /**
     *
     * @param trc database reader interface
     * @param targetURI target URI to get
     * @return CSE id
     */
    public String getCSEid(ResourceTreeReader trc, String targetURI) {
        targetURI = trimURI(targetURI); // get rid of leading and following "/"
        String hierarchy[] = targetURI.split("/"); // split the URI into its hierarchy of path component strings

        Onem2mCse cse = trc.retrieveCse(hierarchy[0]);
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
     *
     * @param trc database reader interface
     * @param targetURI      the URI of the target resource
     * @param onem2mRequest  request
     * @param onem2mResponse response
     * @return found status
     */
    public boolean findResourceUsingURI(ResourceTreeReader trc,
                                        String targetURI,
                                        RequestPrimitive onem2mRequest,
                                        ResponsePrimitive onem2mResponse) {

        CseBaseResourceLocator locator = null;
        try {
            locator = new CseBaseResourceLocator(targetURI);
        } catch (IllegalArgumentException ex) {
            LOG.error("Invalid URI ({}), {}", targetURI, ex.toString());
            return false;
        }

        Onem2mResource onem2mResource = locator.getResource(trc);

        if (onem2mResource == null) {

            return false; // resource not found
        } else {
            // onem2mresource is not empty, check whether this resource is expired.
            if (!isAlive(trc, onem2mResource)) {
                return false;
            }
        }
        onem2mRequest.setResourceId(onem2mResource.getResourceId());
        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setJsonResourceContent(onem2mRequest.getOnem2mResource().getResourceContentJsonString());
        return true;
    }



    /**
     * Checks if onem2mResource is expired. Also adds expired elements in TTLGarbageCollector.
     *
     * @param trc database reader interface
     * @param onem2mResource resource to be checked
     * @return true if is not expired
     */
    public Boolean isAlive(ResourceTreeReader trc, Onem2mResource onem2mResource) {
        try {
            JSONObject jsonObject = new JSONObject(onem2mResource.getResourceContentJsonString());
            String exptime = jsonObject.optString(ResourceContent.EXPIRATION_TIME);
            boolean aliveFlag = "".equals(exptime) || Onem2mDateTime.isAlive(exptime);
            if (!aliveFlag) {
                trc.getTTLGC().addGarbage(onem2mResource);
            }
            return (aliveFlag);

        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", onem2mResource.getResourceContentJsonString(), e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }

    /**
     * Get the resourceId of the hierarchical name
     *
     * @param trc database reader interface
     * @param targetURI URI
     * @return resource id
     */
    public String findResourceIdUsingURI(ResourceTreeReader trc, String targetURI) {

        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, targetURI);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();
        if (!Onem2mDb.getInstance().findResourceUsingURI(trc, targetURI, onem2mRequest, onem2mResponse)) {
            return null;
        }

        return onem2mRequest.getResourceId();

    }

    /**
     * Using the target URI/attribute, strip off the attribute, and see if a resource is found.  Then look to see
     * if the attribute exists under this resource type.
     *
     * @param uriAndAttribute uri and attributes
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
     *
     * @param trc database reader interface
     * @param onem2mResource resource
     * @return true if OldestLatest, of onem2mResource.getParentId, with key CONTENT_INSTANCE, latest id is onem2mResource.getResourceId()
     */
    public boolean isLatestCI(ResourceTreeReader trc, Onem2mResource onem2mResource) {

        if (!onem2mResource.getResourceType().contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
            return false;
        }

        OldestLatest parentOldestLatest =
                trc.retrieveOldestLatestByResourceType(onem2mResource.getParentId(),
                        Onem2m.ResourceType.CONTENT_INSTANCE);
        if (parentOldestLatest != null) {
            String latestId = parentOldestLatest.getLatestId();
            if (latestId.contentEquals(onem2mResource.getResourceId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determine whether the child id is in the targetId's hierarchy.
     *
     * @param trc database reader interface
     * @param targetResourceId key of the target
     * @param childResourceId key of the child
     * @return true if can find resource id in children list of target id
     */
    public boolean isResourceIdUnderTargetId(ResourceTreeReader trc, String targetResourceId, String childResourceId) {

        if (targetResourceId == null || targetResourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            return false;
        }
        if (childResourceId == null || childResourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            return false;
        }
        if (targetResourceId.contentEquals(childResourceId)) {
            return true;
        }

        Onem2mResource onem2mResource = trc.retrieveResourceById(childResourceId);
        while (onem2mResource != null) {

            if (onem2mResource.getParentId().contentEquals(NULL_RESOURCE_ID)) {
                return false;
            }

            String resourceId = onem2mResource.getParentId();
            if (targetResourceId.contentEquals(resourceId)) {
                return true;
            }
            onem2mResource = trc.retrieveResourceById(resourceId);
        }

        return false;
    }

    /**
     * Using the resourceId, build the non hierarchical name of the path using the /cseName + /name of resource
     *
     * @param trc database reader interface
     * @param resourceId resource id
     * @return name of resource using the /cse/resourceId format
     */
    public String getNonHierarchicalNameForResource(ResourceTreeReader trc, String resourceId) {

        if (resourceId == null || resourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            return null;
        }
        String hierarchy = "/" + resourceId;

        Onem2mResource onem2mResource = trc.retrieveResourceById(resourceId);
        while (onem2mResource != null) {

            if (onem2mResource.getParentId().contentEquals(NULL_RESOURCE_ID)) {
                break;
            }
            resourceId = onem2mResource.getParentId();
            onem2mResource = trc.retrieveResourceById(resourceId);
        }
        hierarchy = "/" + onem2mResource.getName() + hierarchy;

        return hierarchy;
    }

    /**
     * Using the resourceId, traverse up the hierarchy till reach the root building the path.
     *
     * @param trc database reader interface
     * @param resourceId the resource id
     * @return name of the resource in hierarchical format (structured CSE-Relative)
     */
    public String getHierarchicalNameForResource(ResourceTreeReader trc, String resourceId) {
        Onem2mResource onem2mResource = trc.retrieveResourceById(resourceId);
        return getHierarchicalNameForResource(trc, onem2mResource);
    }

    /**
     * Using the resourceId, traverse up the hierarchy till reach the root building the path.
     *
     * @param trc database reader interface
     * @param onem2mResource the resource
     * @return name of the resource in hierarchical format
     */
    public String getHierarchicalNameForResource(ResourceTreeReader trc, Onem2mResource onem2mResource) {
        StringBuffer hierarchy = new StringBuffer();

        while (onem2mResource != null) {
            String resourceName;
            if (isLatestCI(trc, onem2mResource)) {
                // todo: latest problem
                resourceName = "latest";
            } else {
                resourceName = onem2mResource.getName();
            }
            hierarchy.insert(0, "/" + resourceName);
            String resourceId = onem2mResource.getParentId();
            if (resourceId.equals(NULL_RESOURCE_ID)) {
                break;
            }
            onem2mResource = trc.retrieveResourceById(resourceId);
        }
        return hierarchy.toString();
    }

    /**
     * Process the hierarchy building a list of the resourceId's.  The routine does not use recursion for fear of
     * stack overflow.  It simply adds child resources on the end of a list that it is processing to build its
     * hierarchical list.
     * @param startResourceId target resource id
     * @param limit enforce a limit of how many are in the list
     * @return the list of resource id's
     */
    public List<String> getHierarchicalResourceList(ResourceTreeReader trc, String startResourceId, int limit) {

        Onem2mResource onem2mResource = trc.retrieveResourceById(startResourceId);
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
            onem2mResource = trc.retrieveResourceById(resourceList.get(i));
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
     *
     * @param trc database reader interface
     * @param resourceId the resource id to get
     * @return the resource info for this id
     */
    public Onem2mResource getResource(ResourceTreeReader trc, String resourceId) {
        return trc.retrieveResourceById(resourceId);
    }

    /**
     * Get the resource using URI
     *
     * @param trc database reader interface
     * @param targetURI the resource URI to get
     * @return the resource info for this URI, it no resource found, return null
     */
    public Onem2mResource getResourceUsingURI(ResourceTreeReader trc, String targetURI) {
        targetURI = trimURI(targetURI); // get rid of leading and following "/"
        String hierarchy[] = targetURI.split("/"); // split the URI into its hierarchy of path component strings

        // start by looking at the cse root: the first level is the cse name
        Onem2mCse cse = trc.retrieveCseByName(hierarchy[0]);
        if (cse == null)
            return null; // resource not found

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
            return trc.retrieveResourceById(cse.getResourceId());
        }

        Onem2mResource onem2mResource = null;
        Onem2mResource saveOnem2mResource = null;

        if (hierarchy.length == 2) { // case 2

            onem2mResource  = trc.retrieveResourceById(hierarchy[1]); // case 2a
            if (onem2mResource == null)
                onem2mResource = trc.retrieveChildResourceByName(cse.getResourceId(), hierarchy[1]); // case 2b
        } else { // case 3
            /**
             * This routine starts at hierarchy[1] and buzzes down the hierarchy looking for the resource name
             */
            String resourceId = cse.getResourceId();
            for (int hierarchyIndex = 1; hierarchyIndex < hierarchy.length; hierarchyIndex++) {
                onem2mResource = trc.retrieveChildResourceByName(resourceId, hierarchy[hierarchyIndex]);
                if (onem2mResource == null) {
                    // check "/latest" in the URI
                    onem2mResource = checkForLatestOldestContentInstance(trc, saveOnem2mResource, hierarchy[hierarchyIndex]);
                    if (onem2mResource == null) {
                        onem2mResource = checkForFanOutPoint(saveOnem2mResource, hierarchy[hierarchyIndex]);
                    }
                    if (onem2mResource == null) {
                        break;
                    }
                }
                resourceId = onem2mResource.getResourceId();
                saveOnem2mResource = onem2mResource;
            }
        }

        if (onem2mResource == null)
            return null; // resource not found

        return onem2mResource;


    }

    /**
     * Retrieve child with name and parent's id.
     *
     * @param trc database reader interface
     * @param resourceId resource key
     * @param childName name of the child
     * @return child resource id with parent resource id and child name
     */
    public String getChildResourceID(ResourceTreeReader trc, String resourceId, String childName) {
        return trc.retrieveChildResourceIDByName(resourceId, childName);
    }

    private boolean deleteContentInstance(ResourceTreeWriter twc,
                                          RequestPrimitive onem2mRequest,
                                          Onem2mResource containerResource) {

        String containerResourceId = containerResource.getResourceId();
        try {
            JSONObject containerResourceContent = new JSONObject(containerResource.getResourceContentJsonString());
            ResourceContainer.setCurrValuesForThisDeletedContentInstance(onem2mRequest,
                    containerResourceContent,
                    onem2mRequest.getJsonResourceContent().optInt(ResourceContentInstance.CONTENT_SIZE));
            return adjustContainerCurrValues(twc, onem2mRequest, containerResourceId, containerResourceContent);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", containerResource.getResourceContentJsonString(), e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }

    /**
     * Deletes are idempotent.  This routine is called after the requestProcessor has grabbed all info it needed
     * for the delete operation so now it is a bulk delete.
     *
     * @param twc database writer interface
     * @param trc database reader interface
     * @param onem2mRequest  request
     * @param onem2mResponse response
     * @return found status
     */
    public boolean deleteResourceUsingURI(ResourceTreeWriter twc, ResourceTreeReader trc, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // if resource not found, then quietly return OK, as the net result is the resource is gone
        if (onem2mRequest.getOnem2mResource() == null)
            return true;

        // save the parent
        String parentResourceId = onem2mRequest.getOnem2mResource().getParentId();
        Onem2mResource parentOnem2mResource = this.getResource(trc, parentResourceId);

        // cache this resource to be deleted
        String thisResourceId = onem2mRequest.getOnem2mResource().getResourceId();
        String thisResourceName = onem2mRequest.getOnem2mResource().getName();

        // build a 'to be Deleted list' by walking the hierarchy
        List<String> resourceIdList = getHierarchicalResourceList(trc, thisResourceId, Onem2m.MAX_RESOURCES + 1);
        String resourceType = onem2mRequest.getOnem2mResource().getResourceType();
        OldestLatest parentOldestLatest = trc.retrieveOldestLatestByResourceType(parentResourceId, resourceType);

        if (parentOldestLatest != null) {
            if (parentOldestLatest.getLatestId().contentEquals(parentOldestLatest.getOldestId())) {

                // only child, set oldest/latest back to NULL
                twc.updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        Onem2mDb.NULL_RESOURCE_ID,
                        Onem2mDb.NULL_RESOURCE_ID);

            } else if (parentOldestLatest.getLatestId().contentEquals(thisResourceId)) {

                // deleting the latest, go back to prev and set is next to null, re point latest to prev
                Child curr = trc.retrieveChildByName(parentResourceId, thisResourceName);
                String prevId = curr.getPrevId();
                Onem2mResource prevOnem2mResource = this.getResource(trc, prevId);

                Child child = trc.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());

                twc.updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        parentOldestLatest.getOldestId(),
                        prevId);
                twc.updateChildSiblingNextInfo(parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID);

            } else if (parentOldestLatest.getOldestId().contentEquals(thisResourceId)) {

                // deleting the oldest, go to next and set its prev to null, re point oldest to next
                Child curr = trc.retrieveChildByName(parentResourceId, thisResourceName);
                String nextId = curr.getNextId();
                Onem2mResource nextOnem2mResource = this.getResource(trc, nextId);

                Child child = trc.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());

                twc.updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        nextId,
                        parentOldestLatest.getLatestId());
                twc.updateChildSiblingPrevInfo(parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID);

            } else {

                Child curr = trc.retrieveChildByName(parentResourceId, thisResourceName);

                String nextId = curr.getNextId();
                Onem2mResource nextOnem2mResource = this.getResource(trc, nextId);
                Child prevChild = trc.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());


                String prevId = curr.getPrevId();
                Onem2mResource prevOnem2mResource = this.getResource(trc, prevId);
                Child nextChild = trc.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());

                twc.updateChildSiblingPrevInfo(parentResourceId, nextChild, Onem2mDb.NULL_RESOURCE_ID);
                twc.updateChildSiblingNextInfo(parentResourceId, prevChild, Onem2mDb.NULL_RESOURCE_ID);
            }
        }


        // adjust the curr values in the parent container resource
        if (resourceType.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
            if (!deleteContentInstance(twc, onem2mRequest, parentOnem2mResource)) return false;
        }

        // now in a transaction, smoke all the resources under this ResourceId
        for (String resourceId : resourceIdList) {
            if (!twc.deleteResourceById(resourceId)) return false;
        }
        onem2mRequest.setOnem2mResource(null);

        // now go clean up the parent of this resource as its child link needs to be removed
        if (!parentResourceId.contentEquals(NULL_RESOURCE_ID))
            if (!twc.removeParentChildLink(parentResourceId, thisResourceName)) return false;

        return true;
    }

    /**
     * Removes all the resources in onem2mResource subtree and onem2mResource Child in it's parent children list.
     *
     * @param twc database writer interface
     * @param trc database reader interface
     * @param onem2mResource element
     * @return true if successfully removed
     */
    public boolean deleteResourceUsingResource(ResourceTreeWriter twc, ResourceTreeReader trc, Onem2mResource onem2mResource) {

        // save the parent
        String parentResourceId = onem2mResource.getParentId();
        Onem2mResource parentOnem2mResource = this.getResource(trc, parentResourceId);

        // cache this resource to be deleted
        String thisResourceId = onem2mResource.getResourceId();
        String thisResourceName = onem2mResource.getName();

        // build a 'to be Deleted list' by walking the hierarchy
        List<String> resourceIdList = getHierarchicalResourceList(trc, thisResourceId, Onem2m.MAX_RESOURCES + 1);
        String resourceType = onem2mResource.getResourceType();
        OldestLatest parentOldestLatest = trc.retrieveOldestLatestByResourceType(parentResourceId, resourceType);

        if (parentOldestLatest != null) {
            if (parentOldestLatest.getLatestId().contentEquals(parentOldestLatest.getOldestId())) {


                // only child, set oldest/latest back to NULL
                twc.updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        Onem2mDb.NULL_RESOURCE_ID,
                        Onem2mDb.NULL_RESOURCE_ID);

            } else if (parentOldestLatest.getLatestId().contentEquals(thisResourceId)) {

                // deleting the latest, go back to prev and set is next to null, re point latest to prev
                Child curr = trc.retrieveChildByName(parentResourceId, thisResourceName);
                String prevId = curr.getPrevId();
                Onem2mResource prevOnem2mResource = this.getResource(trc, prevId);
                Child child = trc.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());

                twc.updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        parentOldestLatest.getOldestId(),
                        prevId);
                twc.updateChildSiblingNextInfo(parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID);

            } else if (parentOldestLatest.getOldestId().contentEquals(thisResourceId)) {

                // deleting the oldest, go to next and set its prev to null, re point oldest to next
                Child curr = trc.retrieveChildByName(parentResourceId, thisResourceName);
                String nextId = curr.getNextId();
                Onem2mResource nextOnem2mResource = this.getResource(trc, nextId);

                Child child = trc.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());

                twc.updateResourceOldestLatestInfo(parentResourceId, resourceType,
                        nextId,
                        parentOldestLatest.getLatestId());
                twc.updateChildSiblingPrevInfo(parentResourceId, child, Onem2mDb.NULL_RESOURCE_ID);

            } else {

                Child curr = trc.retrieveChildByName(parentResourceId, thisResourceName);

                String nextId = curr.getNextId();
                Onem2mResource nextOnem2mResource = this.getResource(trc, nextId);
                Child prevChild = trc.retrieveChildByName(parentResourceId, nextOnem2mResource.getName());


                String prevId = curr.getPrevId();
                Onem2mResource prevOnem2mResource = this.getResource(trc, prevId);
                Child nextChild = trc.retrieveChildByName(parentResourceId, prevOnem2mResource.getName());

                twc.updateChildSiblingPrevInfo(parentResourceId, nextChild, Onem2mDb.NULL_RESOURCE_ID);
                twc.updateChildSiblingNextInfo(parentResourceId, prevChild, Onem2mDb.NULL_RESOURCE_ID);
            }
        }

        // adjust the curr values in the parent container resource
        if (resourceType.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {

            try {
                JSONObject containerResourceContent = new JSONObject(parentOnem2mResource.getResourceContentJsonString());
                Integer cni = containerResourceContent.optInt(ResourceContainer.CURR_NR_INSTANCES);
                Integer cbs = containerResourceContent.optInt(ResourceContainer.CURR_BYTE_SIZE);
                Integer st = containerResourceContent.optInt(ResourceContent.STATE_TAG);

                cni--;
                st++;
                try {
                    JSONObject contentInstanceJsonContent = new JSONObject(onem2mResource.getResourceContentJsonString());
                    cbs -= contentInstanceJsonContent.optInt(ResourceContentInstance.CONTENT_SIZE);
                } catch (JSONException e) {
                    LOG.error("Invalid JSON {}", parentOnem2mResource.getResourceContentJsonString(), e);
                    throw new IllegalArgumentException("Invalid JSON", e);
                }
                JsonUtils.put(containerResourceContent, ResourceContainer.CURR_BYTE_SIZE, cbs);
                JsonUtils.put(containerResourceContent, ResourceContainer.CURR_NR_INSTANCES, cni);
                JsonUtils.put(containerResourceContent, ResourceContent.STATE_TAG, st);
                twc.updateJsonResourceContentString(
                        parentResourceId,
                        containerResourceContent.toString());

            } catch (JSONException e) {
                LOG.error("Invalid JSON {}", parentOnem2mResource.getResourceContentJsonString(), e);
                throw new IllegalArgumentException("Invalid JSON", e);
            }
        }

        // now in a transaction, smoke all the resources under this ResourceId
        for (String resourceId : resourceIdList) {
            twc.deleteResourceById(resourceId);
        }

        // now go clean up the parent of this resource as its child link needs to be removed
        if (!parentResourceId.contentEquals(NULL_RESOURCE_ID))
            if (!twc.removeParentChildLink(parentResourceId, thisResourceName)) return false;
        return true;
    }

    /**
     *
     * @param twc database writer interface
     * @param trc database reader interface
     * @param subscriptionID id
     * @return true if delete was successful
     */
    public boolean deleteSubscription(ResourceTreeWriter twc, ResourceTreeReader trc, String subscriptionID) {
        Onem2mResource subscriptionResource = getResource(trc, subscriptionID);
        return deleteResourceUsingResource(twc, trc, subscriptionResource);
    }

    /**
     * Dump content instances for the container Uri from Head to Tail.  Then again, from Tail to Head
     * @param trc database reader interface
     * @param containerUri containerURI
     */
    public void dumpContentInstancesForContainer(ResourceTreeWriter twc, ResourceTreeReader trc, String containerUri) {

        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        if (!Onem2mDb.getInstance().findResourceUsingURI(trc, containerUri, onem2mRequest, onem2mResponse)) {
            LOG.error("dumpContentInstancesForContainer: cannot find container: {}", containerUri);
            return;
        }

        String resourceType = onem2mRequest.getOnem2mResource().getResourceType();
        if (!resourceType.contentEquals(Onem2m.ResourceType.CONTAINER)) {
            LOG.error("dumpContentInstancesForContainer: resource is not a container: {}", containerUri, resourceType);
        }
        Onem2mResource o = onem2mRequest.getOnem2mResource();
        String containerResourceId = onem2mRequest.getOnem2mResource().getResourceId();

        OldestLatest containerOldestLatest = trc.retrieveOldestLatestByResourceType(containerResourceId,
                Onem2m.ResourceType.CONTENT_INSTANCE);

        if (containerOldestLatest != null) {
            LOG.error("dumpContentInstancesForContainer: dumping oldest to latest: containerResourceUri:{}, containerId: {}, oldest={}, latest={}",
                    containerUri, containerResourceId,
                    containerOldestLatest.getOldestId(), containerOldestLatest.getLatestId());
            String resourceId = containerOldestLatest.getOldestId();
            while (!resourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                Onem2mResource tempResource = getResource(trc, resourceId);
                Child child = trc.retrieveChildByName(containerResourceId, tempResource.getName());
                LOG.error("dumpContentInstancesForContainer: prev:{}, next:{} ", child.getPrevId(), child.getNextId());
                deleteResourceUsingResource(twc, trc, tempResource);
                // todo: find another way to improve the delete speed
                resourceId = child.getNextId();
            }
            LOG.error("dumpContentInstancesForContainer: dumping latest to oldest: containerResourceUri:{}, containerId: {}, oldest={}, latest={}",
                    containerUri, containerResourceId,
                    containerOldestLatest.getOldestId(), containerOldestLatest.getLatestId());
            resourceId = containerOldestLatest.getLatestId();
            while (!resourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                Onem2mResource tempResource = getResource(trc, resourceId);
                // tempResource might be null, if all the cins are deleted in the previous step
                if (tempResource == null) return;
                Child child = trc.retrieveChildByName(containerResourceId, tempResource.getName());
                LOG.error("dumpContentInstancesForContainer: prev:{}, next:{} ", child.getPrevId(), child.getNextId());
                deleteResourceUsingResource(twc, trc, tempResource);
                // todo: find another way to improve the delete speed
                resourceId = child.getPrevId();

            }
        }
    }

    /**
     * This is for notification evenet Type F, if eventType inside eventNotificationCriteria is F,
     * then send the notification. This method is used to find all the subscriptionID with type F.
     * this method will recursively find all the subscription, even under CSE
     *
     * @param trc database reader interface
     * @param resourceID resourceID
     * @return list of ancestors
     */
    public List<String> findAllAncestorsSubscriptionID(ResourceTreeReader trc, String resourceID) {
        List<String> subscriptionResourceList = new ArrayList<>();
        OldestLatest oldestLatest;
        // we will check parent's parents recursively until it is CSE, CSE's parenetID is 0
        Onem2mResource resource = getResource(trc, resourceID);
        while(!resource.getParentId().contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            oldestLatest = trc.retrieveOldestLatestByResourceType(resource.getParentId(),
                    Onem2m.ResourceType.SUBSCRIPTION);
            if (oldestLatest != null && !oldestLatest.getLatestId().contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                subscriptionResourceList = getSubscriptionTypeF(trc, subscriptionResourceList, oldestLatest, resource);
            }
            // continue tracing up layer
            resource = getResource(trc, resource.getParentId());
            // todo: what happens if getResource(0)  null or throw error?

        }

        return subscriptionResourceList;
    }

    /**
     * child method of findAllAncestorsSubscriptionID
     *
     * @param trc database reader interface
     * @param oldestLatest oldlatest
     * @param resource     resource
     * @return list of subscription
     */
    private List<String> getSubscriptionTypeF(ResourceTreeReader trc, List<String> subscriptionResourceList, OldestLatest oldestLatest, Onem2mResource resource) {
        // oldLatest is always not null so must check whether the number is 0
        String subscriptionResourceId = oldestLatest.getLatestId();
        while (!subscriptionResourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            Onem2mResource onem2mSubResource = getResource(trc, subscriptionResourceId);
            try {
                JSONObject subscriptionJsonObject = new JSONObject(onem2mSubResource.getResourceContentJsonString());
                JSONObject enc = subscriptionJsonObject.optJSONObject(ResourceSubscription.EVENT_NOTIFICATION_CRITERIA);
                if (enc != null && enc.getJSONArray(ResourceSubscription.NOTIFICATION_EVENT_TYPE).toString().contains(Onem2m.EventType.ANY_DESCENDENT_CHANGE)) {
                    subscriptionResourceList.add(subscriptionResourceId);
                }
            } catch (JSONException e) {
                LOG.error("Invalid JSON {}", onem2mSubResource.getResourceContentJsonString(), e);
                throw new IllegalArgumentException("Invalid JSON", e);
            }
            // keep getting prev until NULL
            Child child = trc.retrieveChildByName(resource.getParentId(), onem2mSubResource.getName());
            subscriptionResourceId = child.getPrevId();
        }
        return subscriptionResourceList;
    }

    /**
     * find the direct parents' subscriptions IDs, if not found, return empty list.
     * according to the eventType, find the subscriptionID
     * eventType could be A,B,C,D,E
     *
     * @param trc database reader interface
     * @param resourceID resourceID
     * @param eventType  eventType
     * @return list of direct parent subscriptions
     */
    public List<String> finddirectParentSubscriptionID(ResourceTreeReader trc, String resourceID, String eventType) {
        List<String> subscriptionResourceList = new ArrayList<>();
        OldestLatest oldestLatest;
        // we will check parent's parents recursively until it is CSE, CSE's parenetID is 0
        Onem2mResource resource = getResource(trc, resourceID);
        if (!resource.getParentId().contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            oldestLatest = trc.retrieveOldestLatestByResourceType(resource.getParentId(),
                    Onem2m.ResourceType.SUBSCRIPTION);
            if (oldestLatest != null && !oldestLatest.getLatestId().contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                // oldLatest is always not null so must check whether the number is 0
                String subscriptionResourceId = oldestLatest.getLatestId();
                while (!subscriptionResourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                    Onem2mResource onem2mSubResource = getResource(trc, subscriptionResourceId);
                    try {
                        JSONObject subscriptionJsonObject = new JSONObject(onem2mSubResource.getResourceContentJsonString());
                        // todo: if TS 0001 is right
                        JSONObject enc = subscriptionJsonObject.optJSONObject(ResourceSubscription.EVENT_NOTIFICATION_CRITERIA);
                        if (enc != null) {
                            JSONArray netlist = enc.optJSONArray(ResourceSubscription.NOTIFICATION_EVENT_TYPE);
                            if (netlist != null && netlist.toString().contains(eventType)) {
                                subscriptionResourceList.add(subscriptionResourceId);
                            }
                        }
                    } catch (JSONException e) {
                        LOG.error("Invalid JSON {}", onem2mSubResource.getResourceContentJsonString(), e);
                        throw new IllegalArgumentException("Invalid JSON", e);
                    }
                    // keep getting prev until NULL
                    Child child = trc.retrieveChildByName(resource.getParentId(), onem2mSubResource.getName());
                    subscriptionResourceId = child.getPrevId();
                }
            }
        }

        return subscriptionResourceList;
    }

    /**
     *
     * @param trc database reader interface
     * @param resourceID resource id
     * @param eventType event type
     * @return list of resourceId subscriptions
     */
    public List<String> findSelfSubscriptionID(ResourceTreeReader trc, String resourceID, String eventType) {
        List<String> subscriptionResourceList = new ArrayList<>();
        OldestLatest oldestLatest;
        oldestLatest = trc.retrieveOldestLatestByResourceType(resourceID,
                Onem2m.ResourceType.SUBSCRIPTION);
        if (oldestLatest != null && !oldestLatest.getLatestId().contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            // oldLatest is always not null so must check whether the number is 0
            String subscriptionResourceId = oldestLatest.getLatestId();
            while (!subscriptionResourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
                Onem2mResource onem2mSubResource = getResource(trc, subscriptionResourceId);
                try {
                    JSONObject subscriptionJsonObject = new JSONObject(onem2mSubResource.getResourceContentJsonString());
                    // todo: if TS 0001 is right
                    JSONObject enc = subscriptionJsonObject.optJSONObject(ResourceSubscription.EVENT_NOTIFICATION_CRITERIA);
                    if (enc != null) {
                        JSONArray net = enc.optJSONArray(ResourceSubscription.NOTIFICATION_EVENT_TYPE);
                        if (net != null && net.toString().contains(eventType)) {
                            subscriptionResourceList.add(subscriptionResourceId);
                        }
                    } else if (eventType.contentEquals(Onem2m.EventType.UPDATE_RESOURCE)) {
                        //default is update.
                        subscriptionResourceList.add(subscriptionResourceId);
                    }
                } catch (JSONException e) {
                    LOG.error("Invalid JSON {}", onem2mSubResource.getResourceContentJsonString(), e);
                    throw new IllegalArgumentException("Invalid JSON", e);
                }
                // keep getting prev until NULL
                Child child = trc.retrieveChildByName(resourceID, onem2mSubResource.getName());
                subscriptionResourceId = child.getPrevId();
            }
        }


        return subscriptionResourceList;
    }

    /**
     * Dump resource info the the karaf log
     *
     * @param trc database reader interface
     * @param resourceId resource to start dumping from
     */
    public void dumpResourceIdLog(ResourceTreeReader trc, String resourceId) {
        trc.dumpRawTreeToLog(resourceId);
    }

    /**
     * Dump resource info the the karaf log
     * @param trc database reader interface
     * @param resourceId all or starting resource id
     */
    public void dumpHResourceIdToLog(ResourceTreeReader trc, String resourceId) {
        trc.dumpHierarchicalTreeToLog(resourceId);
    }

    /**
     * Remove all resources from the datastore
     * @param twc database writer interface
     */
    public void cleanupDataStore(ResourceTreeWriter twc) {
        twc.reInitializeDatastore(); // reinitialize the data store.
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
