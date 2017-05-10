/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.BGDeleteProcessor;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
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
    private static Integer iotdmInstanceId = 0;
    public ResourceTreeWriter twc;
    public ResourceTreeReader trc;
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
    }

    public BGDeleteProcessor getBGDeleteProcessor() {
        return trc.getBgDp();
    }

    /* required so that mulitple iotdm instances working on a single instance db will generate unique resource id's */
    public void setIotdmInstanceId(Integer iotdmInstanceId) {
        this.iotdmInstanceId = iotdmInstanceId;
    }

    public void registerDbReaderAndWriter(ResourceTreeWriter twc, ResourceTreeReader trc) {
        this.trc = trc;
        this.twc = twc;
    }
    /**
     * Initialize the transaction chains for the database.
     * @param dataBroker data broker
     */
    public void initializeDatastore(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    private String generateResourceId(String parentResourceId,
                                      Integer resourceType) {

        return twc.generateResourceId(parentResourceId, resourceType, iotdmInstanceId);
    }

    public Object startWriteTransaction() {
        return twc.startWriteTransaction();

    }
    public boolean endWriteTransaction(Object transaction) {
        return twc.endWriteTransaction(transaction);

    }
    public List<String> getCseList() {
        return trc.getCseList();
    }

    public List<Onem2mCse> retrieveCseBaseList() {
        return trc.retrieveCseBaseList();
    }
    /**
     * Find the cse using its name
     * @param name cse name
     * @return found or not
     */
    public boolean findCseByName(String name) {
        return (trc.retrieveCseByName(name) != null);
    }

    /**
     * The cse is a special resource, its the root of the tree, and all resources are created under it.
     * @param onem2mRequest  request
     * @param onem2mResponse response
     * @return successful db create
     */
    public boolean createCseResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        onem2mRequest.setResourceId(generateResourceId(NULL_RESOURCE_ID, Onem2m.ResourceType.CSE_BASE));

        JSONObject jsonPrimitiveContent = onem2mRequest.getBaseResource().getInJsonContent();

        JsonUtils.put(jsonPrimitiveContent, BaseResource.RESOURCE_ID, onem2mRequest.getResourceId());
        JsonUtils.put(jsonPrimitiveContent, BaseResource.RESOURCE_NAME, onem2mRequest.getResourceName());

        if (!twc.createCseByName(onem2mRequest.getResourceName(), onem2mRequest.getResourceId())) return false;

        onem2mRequest.setJsonResourceContentString(jsonPrimitiveContent.toString());

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = twc.createResource(null, onem2mRequest,
                NULL_RESOURCE_ID, Onem2m.ResourceType.CSE_BASE);
        if (onem2mResource == null) return false;

        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setJsonResourceContent(jsonPrimitiveContent);

        return true;
    }


    /**
     * The create resource is carried out by this routine.  The onem2mRequest has the parameters in it to effect the
     * creation request.  The resource specific routines have in .../core/resource already vetted the parameters so
     * its just a matter of adding the resource to the data store.
     * @param onem2mRequest  request
     * @param onem2mResponse response
     * @return successful db create
     */
    public boolean createResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        return performResourceCreate(onem2mRequest, onem2mResponse, onem2mRequest.getResourceType());
    }

    private boolean performResourceCreate(RequestPrimitive onem2mRequest,
                                          ResponsePrimitive onem2mResponse,
                                          Integer resourceType) {

        JSONObject jsonPrimitiveContent = onem2mRequest.getBaseResource().getInJsonContent();

        Onem2mResource parentOnem2mResource = onem2mRequest.getParentOnem2mResource();
        String parentId = parentOnem2mResource.getResourceId();

        onem2mRequest.setResourceId(generateResourceId(parentId, resourceType));

        /**
         * The resource name should be filled in with the resource-id if the name is blank.
         */
        if (onem2mRequest.getResourceName() == null) {
            onem2mRequest.setResourceName(onem2mRequest.getResourceId());
        }

        // update the lmt of the parent to be the creation time of the child being created
        if (!parentId.equals(Onem2mDb.NULL_RESOURCE_ID)) {
            modifyParentJsonContentForCreate(onem2mRequest, resourceType);
        }

        if (!parentId.equals(NULL_RESOURCE_ID)) {
            JsonUtils.put(jsonPrimitiveContent, BaseResource.PARENT_ID, parentId);
        }

        JsonUtils.put(jsonPrimitiveContent, BaseResource.RESOURCE_ID, onem2mRequest.getResourceId());
        JsonUtils.put(jsonPrimitiveContent, BaseResource.RESOURCE_NAME, onem2mRequest.getResourceName());

        if (onem2mRequest.getParentContentHasBeenModified()) {
            twc.updateJsonResourceContentString(onem2mRequest.getWriterTransaction(),
                    parentId, onem2mRequest.getParentJsonResourceContent().toString());
        }

        onem2mRequest.setJsonResourceContentString(jsonPrimitiveContent.toString());

        // now create the resource with the attributes stored in the onem2mRequest
        Onem2mResource onem2mResource = twc.createResource(onem2mRequest.getWriterTransaction(), onem2mRequest, parentId, resourceType);
        if (onem2mResource == null) return false;

        onem2mRequest.setOnem2mResource(onem2mResource);
        onem2mRequest.setJsonResourceContent(jsonPrimitiveContent);

        return true;
    }

    private void modifyParentJsonContentForCreate(RequestPrimitive onem2mRequest, Integer resourceType) {

        JSONObject jsonPrimitiveResourceContent = onem2mRequest.getBaseResource().getInJsonContent();
        JSONObject parentJsonContent = onem2mRequest.getParentJsonResourceContent();

        // update the lmt of the parent
        JsonUtils.put(parentJsonContent, BaseResource.LAST_MODIFIED_TIME, jsonPrimitiveResourceContent.optString(BaseResource.CREATION_TIME));

        if (resourceType == Onem2m.ResourceType.CONTENT_INSTANCE) {
            Integer newByteSize = jsonPrimitiveResourceContent.getInt(ResourceContentInstance.CONTENT_SIZE);
            ResourceContainer.incrementValuesForThisCreatedContentInstance(parentJsonContent, newByteSize, onem2mRequest.getResourceId());
            ResourceContainer.checkAndFixCurrMaxRules(parentJsonContent);
        } else if (resourceType == Onem2m.ResourceType.SUBSCRIPTION) {
            ResourceSubscription.modifyParentForSubscriptionCreation(parentJsonContent, onem2mRequest.getResourceId());
        }

        onem2mRequest.setParentContentHasBeenModified(true);
    }

    /**
     * Creates resource of AE type.
     * @param onem2mRequest Received request
     * @param onem2mResponse Response to be sent
     * @param resourceLocator The resource locator
     * @return True if success false otherwise.
     */
    public boolean createResourceAe(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse,
                                    Onem2mDb.CseBaseResourceLocator resourceLocator) {

        JSONObject jsonPrimitiveContent = onem2mRequest.getBaseResource().getInJsonContent();

        String resourceName = onem2mRequest.getResourceName();
        // see resourceAE for comments on why AE_IDs have these naming rules
        String from = onem2mRequest.getPrimitiveFrom();
        if(isNull(resourceName)) {
            if(isNull(from)) {
                resourceName = "C" + onem2mRequest.getResourceId();
            }
            else {
                String[] splitStrins = from.split("/");
                resourceName = splitStrins[splitStrins.length - 1];
            }

            if(ResourceAE.resourceNameExists(onem2mRequest.getParentResourceId(), resourceName, onem2mRequest, onem2mResponse)) {
                return false;
            }

            onem2mRequest.setResourceName(resourceName);
        }
        JsonUtils.put(jsonPrimitiveContent, ResourceAE.AE_ID, onem2mRequest.getResourceName());

        if (!performResourceCreate(onem2mRequest, onem2mResponse, Onem2m.ResourceType.AE)) {
            return false;
        }

        // now create record in the AE-ID to resourceID mapping
        String aeId = onem2mRequest.getResourceName();
        String cseBaseName = resourceLocator.getCseBaseName();
        if (isNull(cseBaseName)) {
            LOG.error("Can't get cseBase name from resource locator");
            return false;
        }

        return twc.createAeUnderCse(cseBaseName, aeId, onem2mRequest.getResourceId());
    }

    /**
     * Creates resource of remoteCSE type
     * @param onem2mRequest Received create request
     * @param onem2mResponse Resulting response to be filled
     * @param cseBaseName  Name of the parent cseBase resource
     * @param remoteCseCseId CSE-ID of the remoteCSE resource
     * @return True if passed false otherwise
     */
    public boolean createResourceRemoteCse(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse,
                                           final String cseBaseName, final String remoteCseCseId) {
        if (null == cseBaseName) {
            LOG.error("Can't get cseBase name from resource locator");
            return false;
        }

        if (null == remoteCseCseId) {
            LOG.error("Can't get CSE-ID of the remoteCSE resource");
            return false;
        }

        //cseBaseName is the same as cseId in our implementation
        Integer registeredEntity = trc.isEntityRegistered(remoteCseCseId, cseBaseName);
        if(nonNull(registeredEntity) && registeredEntity == Onem2m.ResourceType.REMOTE_CSE) {
            LOG.error("Resource create operation failed: remoteCSE with given name already exists.");
            return false;
        }

        boolean ret = false;
        ret = performResourceCreate(onem2mRequest, onem2mResponse, Onem2m.ResourceType.REMOTE_CSE);
        if (!ret) {
            LOG.error("Resource create operation failed");
            return false;
        }

        return twc.createRemoteCseUnderCse(cseBaseName, remoteCseCseId, onem2mRequest.getResourceId());
    }

    /**
     * The update resource is carried out by this routine.  The update uses the original JSON content in
     * onem2mRequest.getJSONResourceContent, then it cycles through each value in the new ResourceContent JSON obj
     * to see what has changed, and applies the change to the old resoruce.
     *
     * @param onem2mRequest  request
     * @param onem2mResponse response
     * @return successful update
     */
    public boolean updateResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        JSONObject existingJsonContent = onem2mRequest.getJsonResourceContent();
        JSONObject newJsonContent = onem2mRequest.getBaseResource().getInJsonContent();

        for (String newKey : JSONObject.getNames(newJsonContent)) {
            if (newJsonContent.isNull(newKey)) {
                existingJsonContent.remove(newKey);
            } else {
                JsonUtils.put(existingJsonContent, newKey, newJsonContent.opt(newKey));
            }
        }

        if (onem2mRequest.getResourceType() == Onem2m.ResourceType.CONTAINER) {
            boolean dr = existingJsonContent.optBoolean(ResourceContainer.DISABLE_RETRIEVAL);
            if (dr == true) {
                LOG.error("updateResource: MUST implement disable_retrieval");
            } else {
                ResourceContainer.checkAndFixCurrMaxRules(existingJsonContent);
            }
        }

        if (!twc.updateJsonResourceContentString(
                onem2mRequest.getWriterTransaction(),
                onem2mRequest.getResourceId(),
                onem2mRequest.getJsonResourceContent().toString())) return false;

        Onem2mResource onem2mResource = getResource(onem2mRequest.getResourceId());
        onem2mRequest.setOnem2mResource(onem2mResource);
        return true;
    }

    /**
     * this method is used to update subscription during the process of sending notification
     *
     * @param resourceID        resourceID of the subscription resource
     * @param updatedJsonString the new json String
     * @return true if successfully updated
     */
    public boolean updateSubscriptionResource(String resourceID, String updatedJsonString) {

        return twc.updateJsonResourceContentString(null, resourceID, updatedJsonString);
    }

    public String findChildFromParentAndChildName(String parentResourceId, String resourceName) {

        return trc.retrieveChildResourceIDByName(parentResourceId, resourceName);
    }

    private Onem2mResource checkForLatestOldestContentInstance(Onem2mResource containerResource, String resourceName) {

        String cinResourceId;

        if (resourceName.contentEquals(ResourceContainer.LATEST) || resourceName.contentEquals("latest")) {
            String rt = containerResource.getResourceType();
            if (rt != null && rt.contentEquals(Integer.valueOf(Onem2m.ResourceType.CONTAINER).toString())) {

                JSONObject containerResourceContent;
                try {
                    containerResourceContent = new JSONObject(containerResource.getResourceContentJsonString());
                    cinResourceId = ResourceContainer.getLatestCI(containerResourceContent);
                } catch (JSONException e) {
                    LOG.error("Invalid JSON {}", containerResource.getResourceContentJsonString(), e);
                    return null;
                }
                return cinResourceId != null ? getResource(cinResourceId) : null;
            }
        } else if (resourceName.contentEquals(ResourceContainer.OLDEST) || resourceName.contentEquals("oldest")) {
            String rt = containerResource.getResourceType();
            if (rt != null && rt.contentEquals(Integer.valueOf(Onem2m.ResourceType.CONTAINER).toString())) {

                JSONObject containerResourceContent;
                try {
                    containerResourceContent = new JSONObject(containerResource.getResourceContentJsonString());
                    cinResourceId = ResourceContainer.getOldestCI(containerResourceContent);
                } catch (JSONException e) {
                    LOG.error("Invalid JSON {}", containerResource.getResourceContentJsonString(), e);
                    return null;
                }
                return cinResourceId != null ? getResource(cinResourceId) : null;
            }
        }

        return null;
    }

    private Onem2mResource checkForFanOutPoint(Onem2mResource groupOnem2mResource, String resourceName) {

        if (resourceName.contentEquals(ResourceGroup.FAN_OUT_POINT) || resourceName.equalsIgnoreCase("fanoutpoint")) {
            String rt = groupOnem2mResource.getResourceType();
            if (rt != null && rt.contentEquals(Integer.valueOf(Onem2m.ResourceType.GROUP).toString())) {
                // if parent's resourceType is group
                return groupOnem2mResource;
                // todo : do the group operaion
            }
        }
        return null;
    }

    /**
     * Creates origin locator object from the originatorEntityId for the cseBase identified by
     * cseBaseCseId parameter.
     * @param originatorEntityId The entity id specified in From parameter of received request.
     * @param cseBaseCseId CseBase CSE-ID to which the sender (not originator) of request is authenticated.
     * @return The object of originator locator implementing methods to access resources representing
     * referenced entities.
     * @throws IllegalArgumentException
     */
    public CseBaseOriginatorLocator getOriginLocator(String originatorEntityId, String cseBaseCseId)
            throws IllegalArgumentException {

        return new CseBaseOriginatorLocator(originatorEntityId, cseBaseCseId);
    }

    /**
     * Class provides methods checking From parameter value if is valid entity ID and
     * to gather information about the entity ID and the resource representing the entity.
     */
    public final class CseBaseOriginatorLocator {
        private Onem2mResource resource = null;
        private final String originatorEntityId;
        private final String entityId;
        private final String cseBaseCseId; // ID of the local cseBase which
        private String registrarCseId = null;
        private boolean isCseRelativeAeId = false;

        /**
         * Constructor splits originator entity ID and resolves entity ID type.
         * @param originatorEntityId The entity ID set in From parameter of received request.
         * @throws IllegalArgumentException Throws exception in case of invalid entity ID provided.
         */
        CseBaseOriginatorLocator(final String originatorEntityId,
                                 final String cseBaseCseId)
                throws IllegalArgumentException {

            if (null == originatorEntityId) {
                throw new IllegalArgumentException("No originator entity ID passed");
            }

            // TODO check FQDN and set specific cseBase CSE-ID in case of secure association
            this.originatorEntityId = originatorEntityId;
            this.cseBaseCseId = cseBaseCseId;

            if (this.originatorEntityId.startsWith("//")) { // absolute CSE or AE id

                String[] path = this.originatorEntityId.split("/");
                if (path.length == 4) { // e.g.: //FQDN/entityId
                    this.entityId = path[3];
                } else if (path.length == 5) { // e.g.: //FQDN/registrarCseId/entityId
                    this.entityId = path[4];
                    this.registrarCseId = path[3];
                } else {
                    throw new IllegalArgumentException("Invalid entity identifier: " + originatorEntityId);
                }

            } else if (this.originatorEntityId.startsWith("/")) { // SP relative CSE or AE id

                String[] path = this.originatorEntityId.split("/");
                if (path.length == 3) { // e.g.: /registrarCseId/entityId
                    this.entityId = path[2]; // AE
                    this.registrarCseId = path[1];
                } else if (path.length == 2) { // e.g.: /entityId
                    this.entityId = path[1];
                } else {
                    throw new IllegalArgumentException("Invalid entity identifier: " + originatorEntityId);
                }

            } else { // relative AE id stem
                this.entityId = this.originatorEntityId;
                this.isCseRelativeAeId = true;
            }
        }

        /**
         * Returns SP-relative-CSE-ID or relative AE-ID Stem.
         * @return The entity id.
         */
        public String getEntityId() { return this.entityId; }

        /**
         * Returns CSE-ID of registrar CSE of the AE
         * @return The CSE-ID of the registrar CSE of AE if exists.
         */
        public String getRegistrarCseId() { return this.registrarCseId; }

        /**
         * Checks whether the originator entity ID is just C-type AE-ID Stem
         * (without registrar CSE CSE-ID specified).
         * @return True if the originator entity ID is relative C-type AE-ID Stem, false otherwise.
         */
        public boolean isCseRelativeCtypeAeId() {
            if (! this.isCseRelativeAeId) {
                return false;
            }

            if (this.entityId.startsWith("C")) {
                return true;
            }

            return false;
        }

        /**
         * Checks whether the originator entity is locally registered.
         * @return
         */
        public Integer isRegistered() {
            // TODO return false if FQDN doesn't equal

            if (null != registrarCseId) {
                if (! Onem2mRouterService.getInstance().hasCseBaseCseId(registrarCseId)) {
                    LOG.trace("RegistrarCSE is not any local cseBase, originator is not registered");
                    return null;
                }

                // So the cseBase which received request and registrarCSE of the entity
                // must be the same, otherwise the entity is not registered
                if (null != cseBaseCseId) {
                    if (! registrarCseId.equals(cseBaseCseId)) {
                        LOG.trace("RegistrarCSE doesn't match cseBase which received request so originator" +
                                  "is not registered");
                        return null;
                    }
                }
            }

            return trc.isEntityRegistered(this.entityId, this.cseBaseCseId);
        }
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

        public String getCseBaseCseId() {
            if (null != this.cseBaseCseId) {
                return this.cseBaseCseId;
            }

            // In our implementation the cseBaseName is the same as cseBase CSE-ID
            return this.cseBaseName;
        }

        public String getCseBaseName() {
            if (null != this.cseBaseName) {
                return this.cseBaseName;
            }

            // In our implementation the cseBaseName is the same as cseBase CSE-ID
            return this.cseBaseCseId;
        }

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
                // the resource identified by URI is the cseBase
                cseBase = this.retrieveCseBase();
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
                    resource = trc.retrieveChildResourceByName(resourceId, this.hierarchyPath[hierarchyIndex]);
                    if (resource == null) {
                        // check "/latest" in the URI
                        resource = checkForLatestOldestContentInstance(savedResource, this.hierarchyPath[hierarchyIndex]);
                        if (resource == null) {
                            resource = checkForFanOutPoint(savedResource, this.hierarchyPath[hierarchyIndex]);
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
     * @param targetURI target URI to get
     * @return CSE id
     */
    public String getCSEid(String targetURI) {
        targetURI = trimURI(targetURI); // get rid of leading and following "/"
        String hierarchy[] = targetURI.split("/"); // split the URI into its hierarchy of path component strings

        Onem2mCse cse = trc.retrieveCse(hierarchy[0]);
        return cse.getResourceId();
    }

    /**
     * Creates resource locator object from the target URI.
     * @param targetURI URI set as TO parameter value in received request.
     * @return The resource locator object implementing methods to access resources referenced by the URI.
     * @throws IllegalArgumentException
     */
    public CseBaseResourceLocator createResourceLocator(String targetURI)
            throws IllegalArgumentException {

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

    private String getCseIdFromResource(Onem2mResource cseResource) {
        // get the content as JSON string and create a JSON object
        String jsonString = cseResource.getResourceContentJsonString();
        if (null == jsonString) {
            LOG.error("CSE resource without content");
            return null;
        }
        JSONObject jsonObj = new JSONObject(jsonString);
        if (null == jsonObj) {
            LOG.error("Failed to create Json object from CSE resource content");
            return null;
        }

        if (! jsonObj.has("csi")) {
            LOG.error("CSE resource without CSE-ID set");
            return null;
        }

        return (String) jsonObj.get("csi");
    }

    private String getAeIdFromResource(Onem2mResource aeResource) {
        // get the content as JSON string and create a JSON object
        String jsonString = aeResource.getResourceContentJsonString();
        if (null == jsonString) {
            LOG.error("AE resource without content");
            return null;
        }
        JSONObject jsonObj = new JSONObject(jsonString);
        if (null == jsonObj) {
            LOG.error("Failed to create Json object from AE resource content");
            return null;
        }

        if (! jsonObj.has("aei")) {
            LOG.error("AE resource without AE-ID set");
            return null;
        }

        return (String) jsonObj.get("aei");
    }

    public Onem2mResource findResourceUsingURI(String targetURI) {

        CseBaseResourceLocator locator = null;
        try {
            locator = new CseBaseResourceLocator(targetURI);
        } catch (IllegalArgumentException ex) {
            LOG.error("Invalid URI ({}), {}", targetURI, ex.toString());
            return null;
        }

        Onem2mResource onem2mResource = locator.getResource();

        if (onem2mResource == null) {
            return null; // resource not found
        } else {
            // onem2mresource is not empty, check whether this resource is expired.
            if (!isAlive(onem2mResource)) {
                return null;
            }
        }

        return onem2mResource;
    }



    /**
     * Checks if onem2mResource is expired. Also adds expired elements in TTLGarbageCollector.
     *
     * @param onem2mResource resource to be checked
     * @return true if is not expired
     */
    public Boolean isAlive(Onem2mResource onem2mResource) {
        try {
            JSONObject jsonObject = new JSONObject(onem2mResource.getResourceContentJsonString());
            String exptime = jsonObject.optString(BaseResource.EXPIRATION_TIME);
            boolean aliveFlag = "".equals(exptime) || Onem2mDateTime.isAlive(exptime);
            if (!aliveFlag) {
                Onem2mDb.getInstance().pseudoDeleteOnem2mResource(onem2mResource);
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
     * @param targetURI URI
     * @return resource id
     */
    public String findResourceIdUsingURI(String targetURI) {

        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitiveTo(targetURI);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();
        Onem2mResource onem2mResource = findResourceUsingURI(targetURI);
        return onem2mResource != null ? onem2mResource.getResourceId() : null;
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
     * @param onem2mResource resource
     * @return true if OldestLatest, of onem2mResource.getParentId, with key CONTENT_INSTANCE, latest id is onem2mResource.getResourceId()
     */
    public boolean isLatestCI(Onem2mResource onem2mResource) {

        if (!onem2mResource.getResourceType().contentEquals(((Integer)Onem2m.ResourceType.CONTENT_INSTANCE).toString())) {
            return false;
        }

        Onem2mResource containerResource = getResource(onem2mResource.getParentId());
        JSONObject containerResourceContent;
        String cinLatestResourceId;
        try {
            containerResourceContent = new JSONObject(containerResource.getResourceContentJsonString());
            cinLatestResourceId = ResourceContainer.getLatestCI(containerResourceContent);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", containerResource.getResourceContentJsonString(), e);
            return false;
        }
        return cinLatestResourceId == null ? false : onem2mResource.getResourceId().equals(cinLatestResourceId);
    }

    /**
     * Determine whether the child id is in the targetId's hierarchy.
     *
     * @param targetResourceId key of the target
     * @param childResourceId key of the child
     * @return true if can find resource id in children list of target id
     */
    public boolean isResourceIdUnderTargetId(String targetResourceId, String childResourceId) {

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

    public String findCseForTarget(String targetResourceId) {

        if (targetResourceId == null || targetResourceId.contentEquals(Onem2mDb.NULL_RESOURCE_ID)) {
            return null;
        }

        String cseName = null;
        Onem2mResource onem2mResource = trc.retrieveResourceById(targetResourceId);
        while (onem2mResource != null) {

            cseName = onem2mResource.getName();

            if (onem2mResource.getParentId().contentEquals(NULL_RESOURCE_ID)) {
                return cseName;
            }

            String resourceId = onem2mResource.getParentId();

            onem2mResource = trc.retrieveResourceById(resourceId);
        }

        return null;
    }

    public String getNonHierarchicalNameForResource(String resourceId) {
        Onem2mResource onem2mResource = trc.retrieveResourceById(resourceId);
        return getNonHierarchicalNameForResource(onem2mResource);
    }

    public String getNonHierarchicalNameForResource(Onem2mResource onem2mResource) {
        if (onem2mResource.getParentId().equals(Onem2mDb.NULL_RESOURCE_ID)) {
            // This is cseBase resource, cseBase's resourceName and its CSE-ID
            // are equal in this implementation so we can use resourceName as
            // CSE-ID here
            return "/" + onem2mResource.getName() + "/" + onem2mResource.getResourceId();
        }
        String[] hierarchy = onem2mResource.getParentTargetUri().split("/");
        return "/" + hierarchy[0] + "/" + onem2mResource.getResourceId();
    }

    /**
     * Using the resourceId, traverse up the hierarchy till reach the root building the path.
     *
     * @param resourceId the resource id
     * @return name of the resource in hierarchical format (structured CSE-Relative)
     */
    public String getHierarchicalNameForResource(String resourceId) {
        Onem2mResource onem2mResource = trc.retrieveResourceById(resourceId);
        return getHierarchicalNameForResource(onem2mResource);
    }

    /**
     * Using the resourceId, traverse up the hierarchy till reach the root building the path.
     *
     * @param onem2mResource the resource
     * @return name of the resource in hierarchical format
     */
    public String getHierarchicalNameForResource(Onem2mResource onem2mResource) {
        return onem2mResource.getParentTargetUri() + "/" + onem2mResource.getName();
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
            List<Onem2mParentChild> childResourceList = trc.retrieveParentChildList(resourceList.get(i));
            for (Onem2mParentChild childResource : childResourceList) {
                resourceList.add(childResource.getResourceId());
                if (resourceList.size() >= limit) {
                    return resourceList;
                }
            }
            resourceListLen += childResourceList.size();
        }

        return resourceList;
    }

    public List<Onem2mParentChild> getChildrenForResource(String resourceId, int limit, int offset) {
        return trc.retrieveParentChildList(resourceId, limit, offset);
    }
    /**
     * Get the resource
     *
     * @param resourceId the resource id to get
     * @return the resource info for this id
     */
    public Onem2mResource getResource(String resourceId) {
        return trc.retrieveResourceById(resourceId);
    }

    /**
     * Retrieve child with name and parent's id.
     *
     * @param resourceId resource key
     * @param childName name of the child
     * @return child resource id with parent resource id and child name
     */
    public String getChildResourceID(String resourceId, String childName) {
        return trc.retrieveChildResourceIDByName(resourceId, childName);
    }

    /**
     * Retrieve list of children for a resource
     *
     * @param resourceId resource key
     * @return childList list of children for a resource
     */
    public List<Onem2mParentChild> getParentChildList(String resourceId) {
        return trc.retrieveParentChildList(resourceId);
    }

    /**
     * Retrieve list of children for a resource
     *
     * @param resourceId resource key
     * @param limit number of child resource data items to be returned
     * @param offset number of child resource data items to be skipped
     * @return childList list of children for a resource
     */
    public List<Onem2mParentChild> getParentChildList(String resourceId, int limit, int offset) {
        return trc.retrieveParentChildList(resourceId, limit, offset);
    }

    private boolean handleModifyingParentForDeleteContentInstance(String cinResourceId, Onem2mResource containerResource) {

        JSONObject containerResourceContent;
        try {
            containerResourceContent = new JSONObject(containerResource.getResourceContentJsonString());
            ResourceContainer.decrementValuesForThisDeletedContentInstance(containerResourceContent, cinResourceId);

        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", containerResource.getResourceContentJsonString(), e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }

        if (!twc.updateJsonResourceContentString(null, containerResource.getResourceId(), containerResourceContent.toString())) {
            return false;
        }
        return true;
    }

    private boolean handleModifyingParentForDeleteSubscription(String subResourceId, Onem2mResource parentResource) {

        JSONObject parentResourceContent;
        try {
            parentResourceContent = new JSONObject(parentResource.getResourceContentJsonString());
            ResourceSubscription.modifyParentForSubscriptionDeletion(parentResourceContent, subResourceId);

        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", parentResource.getResourceContentJsonString(), e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }

        if (!twc.updateJsonResourceContentString(null, parentResource.getResourceId(), parentResourceContent.toString())) {
            return false;
        }
        return true;
    }

    /**
     * Deletes can happen in a few ways.
     * 1) A onem2m request DELETE operation.
     *  1a) move this resources' parent-child to the delete_parent, and in the b/g, actually remove the hierarchy
     *  by traversing the tree and deleting the children onem2m resources and parent-child links from the db
     *  1b) for the current resource, run direct side effect of this resource being deleted
     *      example: if it is a contentInstance, update parent container cni, and cbs fields, and child list
     * 2) A resource expires, when isAlive is checked, if the resource is "expired", it is queued to the garbage
     *  list.  This routine should handle the delete op as it might have to run the direct resource side effects
     *
     * @param onem2mResource element
     * @return true if successfully removed
     */
    public boolean pseudoDeleteOnem2mResource(Onem2mResource onem2mResource) {

        // save the parent
        String parentResourceId = onem2mResource.getParentId();
        Onem2mResource parentOnem2mResource = this.getResource(parentResourceId);

        // cache this resource to be deleted
        String thisResourceId = onem2mResource.getResourceId();
        String thisResourceName = onem2mResource.getName();

        Integer resourceType = Integer.valueOf(onem2mResource.getResourceType());

        String cseBaseCseId = null;
        switch (resourceType) {
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                // adjust the curr values in the parent container resource
                if (!handleModifyingParentForDeleteContentInstance(onem2mResource.getResourceId(), parentOnem2mResource)) {
                    return false;
                }
                break;

            case Onem2m.ResourceType.AE:
                // Parent of AE resource can be the cseBase resource only
                cseBaseCseId = getCseIdFromResource(parentOnem2mResource);
                if (null == cseBaseCseId) {
                    LOG.error("Failed to get cseBase CSE-ID of the AE parrent");
                    break;
                }

                String aeId = getAeIdFromResource(onem2mResource);
                if (null == aeId) {
                    LOG.error("Failed to get AE-ID of AE resource: resourceID {}, name {}",
                            onem2mResource.getResourceId(), onem2mResource.getName());
                    break;
                }

                // Delete also mapping of AE-ID to resourceID
                if (!twc.deleteAeIdToResourceIdMapping(cseBaseCseId, aeId)) {
                    LOG.error("Failed to delete AE-ID to resourceID mapping for: cseBaseCseId: {}, aeId: {}",
                            cseBaseCseId, aeId);
                }
                break;

            case Onem2m.ResourceType.REMOTE_CSE:
                // Parent of AE resource can be the cseBase resource only
                cseBaseCseId = getCseIdFromResource(parentOnem2mResource);
                if (null == cseBaseCseId) {
                    LOG.error("Failed to get cseBase CSE-ID of the AE parent");
                    break;
                }

                String remoteCseCseId = getCseIdFromResource(onem2mResource);
                if (null == remoteCseCseId) {
                    LOG.error("Failed to get CSE-ID of remoteCSE resource: resourceID {}, name {}",
                              onem2mResource.getResourceId(), onem2mResource.getName());
                    break;
                }

                // Delete also mapping of CSE-ID to resourceID
                if (!twc.deleteRemoteCseIdToResourceIdMapping(cseBaseCseId, remoteCseCseId)) {
                    LOG.error("Failed to delete CSE-ID to resourceID mapping for: cseBaseCseId: {}, remoteCseCseId: {}",
                              cseBaseCseId, remoteCseCseId);
                }
                break;

            case Onem2m.ResourceType.SUBSCRIPTION:
                handleModifyingParentForDeleteSubscription(onem2mResource.getResourceId(), parentOnem2mResource);
                break;
        }

        if (!parentResourceId.contentEquals(NULL_RESOURCE_ID)) {
            trc.getBgDp().moveResourceToDeleteParent(thisResourceId);
        }

        return true;
    }

    public boolean moveParentChildLinkToDeleteParent(String oldParentResourceId, String childResourceName, String childResourceId) {
        return twc.moveParentChildLinkToDeleteParent(oldParentResourceId, childResourceName, childResourceId);
    }

    /**
     *
     * @param subscriptionID id
     * @return true if delete was successful
     */
    public boolean deleteSubscription(String subscriptionID) {
        Onem2mResource subscriptionResource = getResource(subscriptionID);
        return pseudoDeleteOnem2mResource(subscriptionResource);
    }

    /**
     * This is for notification evenet Type F, it is a Lionel/Canghi special and hunts up the tree in seach of
     * a special subscription of type F ... is essence if this special subscription exists, then all resources
     * underneath it will be candidates for notifications
     *
     * @param requestPrimitive contains the resId
     * @return list of ancestors
     */
    public List<String> findAllAncestorsSubscriptionID(RequestPrimitive requestPrimitive) {

        List<String> subscriptionResourceList = new ArrayList<>();

        Onem2mResource onem2mResource = requestPrimitive.getOnem2mResource();

        while (onem2mResource != null) {

            try {
                JSONObject jsonResourceContent = new JSONObject(onem2mResource.getResourceContentJsonString());

                String arrayJsonKey = "c:" + Onem2m.ResourceType.SUBSCRIPTION;
                JSONArray jArray = jsonResourceContent.optJSONArray(arrayJsonKey);
                if (jArray != null) {
                    for (int i = 0; i < jArray.length(); i++) {
                        String subscriptionResourceId = jArray.getString(i);
                        addSubscriptionTypeFToList(subscriptionResourceId, subscriptionResourceList);
                    }
                }
            } catch (JSONException e) {
                LOG.error("Invalid JSON {}", onem2mResource.getResourceContentJsonString(), e);
                throw new IllegalArgumentException("Invalid JSON", e);
            }

            String parentResourceid = onem2mResource.getParentId();
            if (parentResourceid.equals(Onem2mDb.NULL_RESOURCE_ID)) {
                onem2mResource = null;
            } else {
                onem2mResource = getResource(parentResourceid);
            }
        }

        return subscriptionResourceList;
    }

    private void addSubscriptionTypeFToList(String subscriptionResourceId, List<String> subscriptionResourceList) {

        Onem2mResource onem2mSubResource = getResource(subscriptionResourceId);
        if (onem2mSubResource == null) return;

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
    }

    /**
     * find the direct parents' subscriptions IDs, if not found, return empty list.
     * according to the eventType, find the subscriptionID
     * eventType could be A,B,C,D,E
     *
     * @param requestPrimitive resourceID
     * @param eventType  eventType
     * @return list of direct parent subscriptions
     */
    public List<String> finddirectParentSubscriptionID(RequestPrimitive requestPrimitive, String eventType) {

        List<String> subscriptionResourceList = new ArrayList<>();

        JSONObject parentJsonResourceContent;

        String parentResourceid = requestPrimitive.getOnem2mResource().getParentId();
        if (parentResourceid.equals(Onem2mDb.NULL_RESOURCE_ID)) {
            return subscriptionResourceList;
        } else {
            Onem2mResource parentOnem2mResource = getResource(parentResourceid);
            try {
                parentJsonResourceContent = new JSONObject(parentOnem2mResource.getResourceContentJsonString());
            } catch (JSONException e) {
                LOG.warn("finddirectParentSubscriptionID: {}", e.toString());
                return subscriptionResourceList;
            }
        }

        String arrayJsonKey = "c:" + Onem2m.ResourceType.SUBSCRIPTION;
        JSONArray jArray = parentJsonResourceContent.optJSONArray(arrayJsonKey);
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); i++) {
                String subscriptionResourceId = jArray.getString(i);
                addSubscriptionsToListForResource(subscriptionResourceId, subscriptionResourceList, eventType);
            }
        }

        return subscriptionResourceList;
    }

    private void addSubscriptionsToListForResource(String subscriptionResourceId,
                                                        List<String> subscriptionResourceList,
                                                        String eventType) {

        Onem2mResource onem2mSubResource = getResource(subscriptionResourceId);
        if (onem2mSubResource == null) {
            return;
        }
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
    }
    /**
     *
     * @param onem2mRequest request
     * @param eventType event type
     * @return list of resourceId subscriptions
     */
    public List<String> findSelfSubscriptionID(RequestPrimitive onem2mRequest, String eventType) {

        List<String> subscriptionResourceList = new ArrayList<>();

        JSONObject jsonResourceContent = onem2mRequest.getJsonResourceContent();

        // maintain a child list of resources for these resourceType as need fast access to them later
        String arrayJsonKey = "c:" + Onem2m.ResourceType.SUBSCRIPTION;
        JSONArray jArray = jsonResourceContent.optJSONArray(arrayJsonKey);
        if (jArray != null) {
            for (int i = 0; i < jArray.length(); i++) {
                String subscriptionResourceId = jArray.getString(i);
                addSubscriptionsToListForResource(subscriptionResourceId, subscriptionResourceList, eventType);
            }
        }

        return subscriptionResourceList;
    }

    /**
     * Dump resource info the the karaf log
     *
     * @param resourceId resource to start dumping from
     */
    public void dumpResourceIdLog(String resourceId) {
        trc.dumpRawTreeToLog(resourceId);
    }

    /**
     * Dump resource info the the karaf log
     * @param resourceId all or starting resource id
     */
    public void dumpHResourceIdToLog(String resourceId) {
        trc.dumpHierarchicalTreeToLog(resourceId);
    }

    /**
     * Remove all resources from the datastore
     */
    public void cleanupDataStore() {
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
