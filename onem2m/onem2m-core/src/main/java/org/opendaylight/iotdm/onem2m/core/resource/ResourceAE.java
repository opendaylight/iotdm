/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.MemberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.MemberKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class services the AE resource CRUD operations.  It processes the onem2m request primitive.  It also
 * has to extract the AE attributes from the RequestPrimitive.CONTENT attr.  It is encoded in a string.
 * Based on the CONTENT_FORMAT, it is sent to the appropriate parser.  The PrimitiveUtils class helps with
 * some of this parsing effort.  The CRUD/N operations ensure the validation of the parameters is done.
 */
public class ResourceAE {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAE.class);

    private ResourceAE() {
    }

    // TODO: TS0004_1.0.1  7.3.5 CRUD/N procedures for AE
    // TODO: TS0001_1.5.0 9.6.5 attribute definitions
    // TODO: CDT-AE-v1_0_0.xsd / TS0004_1.0.1 Section 8.2.2 Short Names

    public static final String APP_NAME = "apn";
    public static final String APP_ID = "api";
    public static final String AE_ID = "aei";
    public static final String POINT_OF_ACCESS = "apn";
    public static final String ONTOLOGY_REF = "or";
    public static final String NODE_LINK = "nl"; // do not support node resource yet

    private static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // ensure resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            DbAttr parentDbAttrs = onem2mRequest.getDbAttrs();
            String rt = parentDbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
            if (rt == null || !rt.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create AE under this resource type: " + rt);
                return;
            }
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        String appId = resourceContent.getDbAttr(APP_ID);
        if (appId == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "APP_ID missing parameter");
            return;
        } else if (appId != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "APP_ID cannot be updated");
            return;
        }

        /**
         * Construct the AE_ID field ... using some rules
         * 1) FROM field is null --> generate an aei using either the resource name which must be unique, or
         * if the resource name is not provided, gerate one via generate, then apply it to the aei and res name.
         * 2) if the FROM field has something, then use it in the aei, and the resource name if the res name is
         * null.
         *
         * This logic will runs in the createResource as it can generate unique id's
         */

        /**
         * The resource has been filled in with any attributes that need to be written to the database
         */
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
    }

    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     *
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void processJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getJsonContent().keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            Object o = resourceContent.getJsonContent().get(key);

            switch (key) {

                case APP_NAME:
                case ONTOLOGY_REF:
                case APP_ID:
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
                case AE_ID: // ignore its value as it is not settable
                    break;
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
        }
    }

    /**
     * Parse the CONTENT resource representation for create and update
     *
     * @param onem2mRequest  request
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

        ResourceAE.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);
    }


    /**
     * Generate JSON data for this resource
     *
     */
    public static void produceJsonForAttr(Attr attr, JSONObject j) {

        switch (attr.getName()) {
            case APP_NAME:
            case APP_ID:
            case AE_ID:
            case ONTOLOGY_REF:
                j.put(attr.getName(), attr.getValue());
                break;
            default:
                ResourceContent.produceJsonForCommonAttributes(attr, j);
                break;
        }
    }

    /**
     * Generate JSON data for this attr set
     */
    public static void produceJsonForAttrSet(AttrSet attrSet, JSONObject j) {

        switch (attrSet.getName()) {
            default:
                ResourceContent.produceJsonForCommonAttributeSets(attrSet, j);
                break;
        }
    }

    /**
     * Generate JSON data for this resource
     * @param onem2mResource this resource
     * @param j the object to put the json into
     */
    public static void produceJsonForResource(Onem2mResource onem2mResource, JSONObject j) {

        for (Attr attr : onem2mResource.getAttr()) {
            produceJsonForAttr(attr, j);
        }
        for (AttrSet attrSet : onem2mResource.getAttrSet()) {
            produceJsonForAttrSet(attrSet, j);
        }
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

                case APP_NAME:
                case AE_ID:
                case ONTOLOGY_REF:
                case APP_ID:
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
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
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            processJsonRetrieveContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
    }
}
