/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.Iterator;

import org.json.JSONArray;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
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
    public static final String POINT_OF_ACCESS = "poa";
    public static final String ONTOLOGY_REF = "or";
    public static final String NODE_LINK = "nl"; // do not support node resource yet
    public static final String REQUEST_REACHABILITY = "rr";
    public static final String CONTENT_SERIALIZATION = "csz";


    private static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // ensure resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            String parentResourceType = onem2mRequest.getOnem2mResource().getResourceType();
            if (parentResourceType == null || !parentResourceType.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create AE under this resource type: " + parentResourceType);
                return;
            }
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        /**
         * AE_ID is checked at parse time ... no need to check for it here
         */

        String appId = resourceContent.getInJsonContent().optString(APP_ID, null);
        if (appId == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "APP_ID missing parameter");
            return;
        } else if (appId != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "APP_ID cannot be updated");
            return;
        }

        String rr = resourceContent.getInJsonContent().optString(REQUEST_REACHABILITY, null);
        if (rr == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "REQUEST_REACHABILITY missing parameter");
            return;
        }


        /**
         * Check the From, if
         * (1) the From is xxx:// yyyy, remove xxx://
         * (2) if yyyy still contains / return error.
         */
        String from = onem2mRequest.getPrimitive(RequestPrimitive.FROM);
        String[] splitStrins = from.split("//");
        // does not need to concern 2 //, we will check valid URI in the following steps
        if (splitStrins.length == 2) {
            String removedHead = splitStrins[1];
            if (removedHead.contains("/")) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "From cannot contain / ");
                return;
            }
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
    private static void parseJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getInJsonContent().keys();
        while (keys.hasNext()) {

            String key = (String) keys.next();

            resourceContent.jsonCreateKeys.add(key);

            Object o = resourceContent.getInJsonContent().opt(key);

            switch (key) {

                case APP_NAME:
                case ONTOLOGY_REF:
                case APP_ID:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case NODE_LINK:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        } else {
                            // if it is a String, check whether it is a valid node
                            String nodeid = (String)o;
                            Onem2mResource nodeResource = Onem2mDb.getInstance().getResource(nodeid);
                            if (nodeResource == null || !nodeResource.getResourceType().contentEquals(Onem2m.ResourceType.NODE)) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                        "CONTENT(" + RequestPrimitive.CONTENT + ") nodelink is not a valid node's resourceID");
                                return;
                            }
                        }
                    }
                    break;
                case REQUEST_REACHABILITY:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof Boolean)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") boolean expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case CONTENT_SERIALIZATION:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof JSONArray)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                            return;
                        }
                        JSONArray array = (JSONArray) o;
                        for (int i = 0; i < array.length(); i++) {
                            if (!(array.opt(i) instanceof String)) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                        "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json array: " + key);
                                return;
                            } else {
                                // check all the string belong to json/xml
                                if (!array.optString(i).equalsIgnoreCase("XML") && !array.optString(i).equalsIgnoreCase("JSON")) {
                                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                            "CONTENT(" + RequestPrimitive.CONTENT + ") only accept word JSON or XML for attribute " + key);
                                    return;
                                }
                            }
                        }
                    }
                    break;
                case POINT_OF_ACCESS:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof JSONArray)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                            return;
                        }
                        JSONArray array = (JSONArray) o;
                        for (int i = 0; i < array.length(); i++) {
                            if (!(array.opt(i) instanceof String)) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                        "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json array: " + key);
                                return;
                            }
                        }
                    }
                    break;
                case ResourceContent.LABELS:

                case ResourceContent.ACCESS_CONTROL_POLICY_IDS:

                case ResourceContent.EXPIRATION_TIME:
                case ResourceContent.RESOURCE_NAME:
                    if (!ResourceContent.parseJsonCommonCreateUpdateContent(key,
                            resourceContent,
                            onem2mResponse)) {
                        return;
                    }
                    break;
                case AE_ID: // return error message if detect this attribute
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") AE_ID should be assigned by the system, please do not include " + key);
                    return;
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

        resourceContent.parse(Onem2m.ResourceTypeString.AE, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            parseJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
        CheckAccessControlProcessor.handleCreateUpdate(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        resourceContent.processCommonCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;


        ResourceAE.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);
    }
}
