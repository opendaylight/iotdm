/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContentInstance  {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContentInstance.class);
    private ResourceContentInstance() {}

    // taken from CDT-contentInstance-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: ts0001 9.6.7-1

    public static final String CONTENT_INFO = "cnf";
    public static final String CONTENT_SIZE = "cs";
    public static final String CONTENT = "con";
    public static final String ONTOLOGY_REF = "or";
    public static final String CREATOR = "cr";

    private static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String tempStr;
        Integer tempInt;
        Integer newByteSize;

        // no need to handle update in this file as update is not allowed for content instances

        // verify this resource can be created under the target resource
        DbAttr containerDbAttrs = onem2mRequest.getDbAttrs();
        String rt = containerDbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
        if (rt == null || !rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Cannot create ContentInstance under this resource type: " + rt);
            return;
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        tempStr = resourceContent.getDbAttr(CONTENT_SIZE);
        if (tempStr != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CONTENT_SIZE read-only parameter");
            return;
        }

        tempStr = resourceContent.getDbAttr(CONTENT);
        if (tempStr == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CONTENT missing parameter");
            return;
        } else {
            // record the size of the content in the CONTENT_SIZE attribute
            newByteSize = tempStr.length();
            resourceContent.setDbAttr(CONTENT_SIZE, newByteSize.toString());
        }

        // state tag is read only
        tempStr = resourceContent.getDbAttr(ResourceContent.STATE_TAG);
        if (tempStr != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "STATE_TAG read-only parameter");
            return;
        }

        // the state tag of the ci is the incremented value of its parent container
        tempStr = containerDbAttrs.getAttr(ResourceContent.STATE_TAG);
        Integer containerStateTagValue = Integer.valueOf(tempStr);
        containerStateTagValue++;
        resourceContent.setDbAttr(ResourceContent.STATE_TAG, containerStateTagValue.toString());

        // verify this content instance does not exceed the container's max byte size
        tempStr = containerDbAttrs.getAttr(ResourceContainer.MAX_BYTE_SIZE);
        if (tempStr != null) {
            Integer mbs = Integer.valueOf(tempStr);
            if (newByteSize > mbs) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "content size: " + newByteSize + " exceeds its containers max byte size: " + mbs);
                return;
            }

        }

        // special case: max_nr_instances == 0 --> seems like a way to block content instances from creation
        tempStr = containerDbAttrs.getAttr(ResourceContainer.MAX_NR_INSTANCES);
        if (tempStr != null) {
            Integer mni = Integer.valueOf(tempStr);
            if (mni < 1) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Exceeds containers max instances: " + mni);
                return;
            }

        }

        ResourceContainer.setCurrValuesForThisCreatedContentInstance(onem2mRequest, containerDbAttrs, newByteSize);

        if (onem2mRequest.isCreate) {
            if (!Onem2mDb.getInstance().createResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot write to data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        } else {
            if (!Onem2mDb.getInstance().updateResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot write to data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        }

        // may have to remove content instances to make room for this latest instance
        ResourceContainer.checkAndFixCurrMaxRules(onem2mRequest.getPrimitive(RequestPrimitive.TO));
    }

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

                case CONTENT_INFO:
                case ONTOLOGY_REF:
                case CONTENT:
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
                case CREATOR:
                    if (!resourceContent.getJsonContent().isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
                    } else {
                        resourceContent.setDbAttr(key, onem2mRequest.getPrimitive(RequestPrimitive.FROM));
                    }
                    break;
                case ResourceContent.LABELS:
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
     * Parse the CONTENT resource representation.
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(onem2mRequest, onem2mResponse);
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

        ResourceContentInstance.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);
    }

    /**
     * Generate JSON for this resource
     * @param onem2mResource this resource
     * @param j JSON obj
     */
    public static void produceJsonForResource(Onem2mResource onem2mResource, JSONObject j) {

        for (Attr attr : onem2mResource.getAttr()) {
            switch (attr.getName()) {
                case CONTENT:
                case ONTOLOGY_REF:
                case CONTENT_INFO:
                    j.put(attr.getName(), attr.getValue());
                    break;
                case CONTENT_SIZE:
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
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     *
     * This routine enforces the mandatory and option parameters
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void processJsonRetrieveContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getJsonContent().keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();

            Object o = resourceContent.getJsonContent().get(key);

            switch (key) {

                case ONTOLOGY_REF:
                case CONTENT_INFO:
                case CONTENT:
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                    resourceContent.setDbAttr(key, o.toString());
                    break;

                case CONTENT_SIZE:

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

        resourceContent.parse(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null) {
            return;
        }

        if (resourceContent.isJson()) {
            processJsonRetrieveContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null) {
                return;
            }
        }
    }
}