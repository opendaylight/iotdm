/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.database.DbAttrSet;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.RequestPrimitiveProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContainer {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContainer.class);
    private ResourceContainer() {}

    // taken from CDT-container-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: ts0001 9.6.6-2


    public static final String CREATOR = "cr";
    public static final String MAX_NR_INSTANCES = "mni";
    public static final String MAX_BYTE_SIZE = "mbs";
    public static final String CURR_NR_INSTANCES = "cni";
    public static final String CURR_BYTE_SIZE = "cbs";
    public static final String ONTOLOGY_REF = "or";
    public static final String LATEST = "la";
    public static final String OLDEST = "ol";

    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void processJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getJsonContent().keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();

            Object o = resourceContent.getJsonContent().get(key);

            switch (key) {
                case MAX_NR_INSTANCES:
                case MAX_BYTE_SIZE:
                    if (!resourceContent.getJsonContent().isNull(key)) {
                        if (!(o instanceof Integer)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") number expected for json key: " + key);
                            return;
                        } else if (((Integer) o).intValue() < 0) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") integer must be non-negative: " + key);
                            return;
                        }
                        resourceContent.setDbAttr(key, o.toString());

                    } else {
                        resourceContent.setDbAttr(key, null);
                    }
                    break;

                case CREATOR:
                    if (!resourceContent.getJsonContent().isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
                    } else {
                        resourceContent.setDbAttr(key, onem2mRequest.getPrimitive(RequestPrimitive.FROM));
                    }
                    break;

                case ONTOLOGY_REF:
                    if (!resourceContent.getJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                        resourceContent.setDbAttr(key, o.toString());
                    } else {
                        resourceContent.setDbAttr(key, null);
                    }
                    break;
                case ResourceContent.LABELS:
                case ResourceContent.EXPIRATION_TIME:
                    if (!ResourceContent.processJsonCommonCreateUpdateContent(key,
                            resourceContent,
                            onem2mResponse)) {
                        return;
                    }
                    break;
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
        }
    }

    /**
     * When create a container, add two special names as children, one is latest, the other is oldest, and
     * the values will initially be "".  When the first contentInstance is added under the container, then
     * point the oldest and latest to this new resource, and the new contentInstance, add two new attrs,
     * prev, next with values "".  These 4 special attrs will be head, tail, prev, next for a doubly LL.
     */

    /**
     * Ensure the create/update parameters follow the rules
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String tempStr;
        Integer tempInt;

        // verify this resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            DbAttr parentDbAttrs = onem2mRequest.getDbAttrs();
            String rt = parentDbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
            if (rt == null || !(rt.contentEquals(Onem2m.ResourceType.CSE_BASE) ||
                    rt.contentEquals(Onem2m.ResourceType.CONTAINER) ||
                    rt.contentEquals(Onem2m.ResourceType.AE))) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create Container under this resource type: " + rt);
                return;
            }
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        tempStr = resourceContent.getDbAttr(CREATOR);
        if (tempStr != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CREATOR cannot be updated");
            return;
        }

        // state tag is read only
        tempStr = resourceContent.getDbAttr(ResourceContent.STATE_TAG);
        if (tempStr != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "STATE_TAG read-only parameter");
            return;
        }
        if (onem2mRequest.isCreate) {
            // initialize state tag to 0
            tempInt = 0;
            resourceContent.setDbAttr(ResourceContent.STATE_TAG, tempInt.toString());
        } else {
            // update the existing state tag as the resource is being updated
            tempStr = onem2mRequest.getDbAttrs().getAttr(ResourceContent.STATE_TAG);
            if (tempStr != null) {
                tempInt = Integer.valueOf(tempStr);
                tempInt++;
                resourceContent.setDbAttr(ResourceContent.STATE_TAG, tempInt.toString());
            }
        }

        if (onem2mRequest.isCreate) {
            // initialize currNrOfInstances to 0
            tempStr = resourceContent.getDbAttr(CURR_NR_INSTANCES);
            if (tempStr != null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CURR_NR_INSTANCES read-only parameter");
                return;
            }
            tempInt = 0;
            resourceContent.setDbAttr(CURR_NR_INSTANCES, tempInt.toString());

            // initialize currByteSize to 0
            tempStr = resourceContent.getDbAttr(CURR_BYTE_SIZE);
            if (tempStr != null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CURR_BYTE_SIZE read-only parameter");
                return;
            }
            tempInt = 0;
            resourceContent.setDbAttr(CURR_BYTE_SIZE, tempInt.toString());
        }

        /**
         * The resource has been filled in with any attributes that need to be written to the database
         */
        if (onem2mRequest.isCreate) {
            if (!Onem2mDb.getInstance().createResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot create in data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        } else {
            if (!Onem2mDb.getInstance().updateResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot update the data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
            // may have to remove content instances as mbs and mni may have been reduced
            ResourceContainer.checkAndFixCurrMaxRules(onem2mRequest.getPrimitive(RequestPrimitive.TO));
            Onem2mResource containerResource = Onem2mDb.getInstance().getResource(onem2mRequest.getResourceId());
            onem2mRequest.setOnem2mResource(containerResource);
            onem2mRequest.setDbAttrs(new DbAttr(onem2mRequest.getOnem2mResource().getAttr()));
            onem2mRequest.setDbAttrSets(new DbAttrSet(onem2mRequest.getOnem2mResource().getAttrSet()));
        }
    }

    /**
     * Parse the CONTENT resource representation.
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(Onem2m.ResourceTypeString.CONTAINER, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            processJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
        resourceContent.processCommonCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
        ResourceContainer.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }

    /**
     * Generate JSON for this resource
     * @param onem2mResource this resource
     * @param j JSON obj to put teh formatted json into
     */
    public static void produceJsonForResource(Onem2mResource onem2mResource, JSONObject j) {

        for (Attr attr : onem2mResource.getAttr()) {
            switch (attr.getName()) {
                case CREATOR:
                case ONTOLOGY_REF:
                    j.put(attr.getName(), attr.getValue());
                    break;
                case MAX_NR_INSTANCES:
                case MAX_BYTE_SIZE:
                case CURR_NR_INSTANCES:
                case CURR_BYTE_SIZE:
                    j.put(attr.getName(), Integer.valueOf(attr.getValue()));
                    break;
                default:
                    ResourceContent.produceJsonForCommonAttributes(attr, j);
                    break;
            }
        }
        for (AttrSet attrSet : onem2mResource.getAttrSet()) {
            switch (attrSet.getName()) {
                default:
                    ResourceContent.produceJsonForCommonAttributeSets(attrSet, j);
                    break;
            }
        }
    }

    /**
     * Generate JSON for this resource Creation Only
     * @param onem2mResource this resource
     * @param j JSON obj to put teh formatted json into
     */
    public static void produceJsonForResourceCreate(Onem2mResource onem2mResource, JSONObject j) {

        for (Attr attr : onem2mResource.getAttr()) {
            switch (attr.getName()) {
                case CREATOR:
                    //case ONTOLOGY_REF:
                    j.put(attr.getName(), attr.getValue());
                    break;
                case MAX_NR_INSTANCES:
                case MAX_BYTE_SIZE:
                case CURR_NR_INSTANCES:
                case CURR_BYTE_SIZE:
                    j.put(attr.getName(), Integer.valueOf(attr.getValue()));
                    break;
                default:
                    ResourceContent.produceJsonForCommonAttributesCreate(attr, j);
                    break;
            }
        }
//        for (AttrSet attrSet : onem2mResource.getAttrSet()) {
//            switch (attrSet.getName()) {
//                default:
//                    ResourceContent.produceJsonForCommonAttributeSets(attrSet, j);
//                    break;
//            }
//        }
    }

    private static boolean removeOldestContentInstance(String containerUri) {
        String oldestContentInstanceUri = containerUri + "/oldest";
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, oldestContentInstanceUri);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        if (!Onem2mDb.getInstance().findResourceUsingURI(oldestContentInstanceUri, onem2mRequest, onem2mResponse)) {
            return false;
        }
        if (Onem2mDb.getInstance().deleteResourceUsingURI(onem2mRequest, onem2mResponse) == false) {
            return false;
        }
        return true;
    }

    public static void checkAndFixCurrMaxRules(String containerUri) {
        RequestPrimitiveProcessor onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
            LOG.error("Somehow the resource container is gone! : " + containerUri);
            return;
        }

        boolean stillMoreToDelete = true;
        while (stillMoreToDelete) {
            String maxNrInstancesString = onem2mRequest.getDbAttrs().getAttr(ResourceContainer.MAX_NR_INSTANCES);
            if (maxNrInstancesString != null) {
                String currNrInstancesString = onem2mRequest.getDbAttrs().getAttr(ResourceContainer.CURR_NR_INSTANCES);
                if (currNrInstancesString != null) {
                    Integer maxNrInstanceValue = Integer.valueOf(maxNrInstancesString);
                    Integer currNrInstanceValue = Integer.valueOf(currNrInstancesString);
                    if (currNrInstanceValue <= maxNrInstanceValue) {
                        stillMoreToDelete = false;
                    } else {
                        if (!removeOldestContentInstance(containerUri)) {
                            stillMoreToDelete = false;
                        }
                        onem2mRequest = new RequestPrimitiveProcessor();
                        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
                        onem2mResponse = new ResponsePrimitive();
                        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
                            LOG.error("Somehow the resource container is gone! : " + containerUri);
                            return;
                        }
                    }
                } else {
                    stillMoreToDelete = false;
                }
            } else {
                stillMoreToDelete = false;
            }
        }

        onem2mRequest = new RequestPrimitiveProcessor();
        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
        onem2mResponse = new ResponsePrimitive();
        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
            LOG.error("Somehow the resource container is gone! : " + containerUri);
            return;
        }

        stillMoreToDelete = true;
        while (stillMoreToDelete) {
            String maxByteSizeString = onem2mRequest.getDbAttrs().getAttr(ResourceContainer.MAX_BYTE_SIZE);
            if (maxByteSizeString != null) {
                String currByteSizeString = onem2mRequest.getDbAttrs().getAttr(ResourceContainer.CURR_BYTE_SIZE);
                if (currByteSizeString != null) {
                    Integer maxByteSizeValue = Integer.valueOf(maxByteSizeString);
                    Integer currByteSizeValue = Integer.valueOf(currByteSizeString);
                    if (currByteSizeValue <= maxByteSizeValue) {
                        stillMoreToDelete = false;
                    } else {
                        if (!removeOldestContentInstance(containerUri)) {
                            stillMoreToDelete = false;
                        }
                        onem2mRequest = new RequestPrimitiveProcessor();
                        onem2mRequest.setPrimitive(RequestPrimitive.TO, containerUri);
                        onem2mResponse = new ResponsePrimitive();
                        if (!Onem2mDb.getInstance().findResourceUsingURI(containerUri, onem2mRequest, onem2mResponse)) {
                            LOG.error("Somehow the resource container is gone! : " + containerUri);
                            return;
                        }
                    }
                } else {
                    stillMoreToDelete = false;
                }
            } else {
                stillMoreToDelete = false;
            }
        }
    }

    /**
     * A side effect of creating a content instance is that its parent container curr_byte_size,
     * curr_nr_instances, and state tag must also be updated.
     * @param onem2mRequest
     * @param containerDbAttrs
     * @param newByteSize
     */
    public static void setCurrValuesForThisCreatedContentInstance(RequestPrimitive onem2mRequest,
                                                                 DbAttr containerDbAttrs,
                                                                 Integer newByteSize) {
        String tempStr;

        String currNrInstancesString = containerDbAttrs.getAttr(ResourceContainer.CURR_NR_INSTANCES);
        String currByteSizeString = containerDbAttrs.getAttr(ResourceContainer.CURR_BYTE_SIZE);
        String currStateTagString = containerDbAttrs.getAttr(ResourceContent.STATE_TAG);

        Integer cni = Integer.valueOf(currNrInstancesString);
        Integer cbs = Integer.valueOf(currByteSizeString);
        Integer st = Integer.valueOf(currStateTagString);

        cni++;
        cbs += newByteSize;
        st++;

        onem2mRequest.setCurrContainerValues(cbs, cni, st);
    }

    /**
     * A side effect of deleting a content instance is that its parent container curr_byte_size,
     * curr_nr_instances, and state tag must also be updated.
     * @param onem2mRequest
     * @param containerDbAttrs
     * @param delByteSize
     */
    public static void setCurrValuesForThisDeletedContentInstance(RequestPrimitive onem2mRequest,
                                                                  DbAttr containerDbAttrs,
                                                                  Integer delByteSize) {
        String tempStr;

        String currNrInstancesString = containerDbAttrs.getAttr(ResourceContainer.CURR_NR_INSTANCES);
        String currByteSizeString = containerDbAttrs.getAttr(ResourceContainer.CURR_BYTE_SIZE);
        String currStateTagString = containerDbAttrs.getAttr(ResourceContent.STATE_TAG);

        Integer cni = Integer.valueOf(currNrInstancesString);
        Integer cbs = Integer.valueOf(currByteSizeString);
        Integer st = Integer.valueOf(currStateTagString);

        cni--;
        cbs -= delByteSize;
        st++;

        onem2mRequest.setCurrContainerValues(cbs, cni, st);
    }

    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     *
     * This routine enforces the mandatory and option parameters
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    private static void processJsonRetrieveContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getJsonContent().keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();

            Object o = resourceContent.getJsonContent().get(key);

            switch (key) {

                case ONTOLOGY_REF:
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                    resourceContent.setDbAttr(key, o.toString());
                    break;

                case MAX_NR_INSTANCES:
                case MAX_BYTE_SIZE:
                case CURR_NR_INSTANCES:
                case CURR_BYTE_SIZE:
                    if (!(o instanceof Integer)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") integer expected for json key: " + key);
                        return;
                    }
                    resourceContent.setDbAttr(key, o.toString());
                    break;
                default:
                    if (!ResourceContent.processJsonCommonRetrieveContent(key, resourceContent, onem2mResponse)) {
                        return;
                    }
                    break;
            }
        }
    }

    /**
     * Parse the CONTENT resource representation.
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleRetrieve(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(Onem2m.ResourceTypeString.CONTAINER, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            processJsonRetrieveContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
    }
}