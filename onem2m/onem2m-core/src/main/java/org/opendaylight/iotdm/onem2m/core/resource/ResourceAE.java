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
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
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
public class ResourceAE extends BaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAE.class);

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

    Onem2mDb.CseBaseResourceLocator resourceLocator;

    public ResourceAE(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse,
                      Onem2mDb.CseBaseResourceLocator resourceLocator) {

        super(onem2mRequest, onem2mResponse);
        this.resourceLocator = resourceLocator;
    }

    private void processCreateUpdateAttributes() {

        // ensure resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            Integer prt = onem2mRequest.getParentResourceType();
            if (prt != Onem2m.ResourceType.CSE_BASE) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create AE under this resource type: " + prt);
                return;
            }
        }

        /**
         * AE_ID is checked at parse time ... no need to check for it here
         */

        String appId = jsonPrimitiveContent.optString(APP_ID, null);
        if (appId == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "APP_ID missing parameter");
            return;
        } else if (appId != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "APP_ID cannot be updated");
            return;
        }

        String rr = jsonPrimitiveContent.optString(REQUEST_REACHABILITY, null);
        if (rr == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "REQUEST_REACHABILITY missing parameter");
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
            if (!Onem2mDb.getInstance().createResourceAe(onem2mRequest, onem2mResponse, resourceLocator)) {
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

    private void parseJsonCreateUpdateContent() {

        Iterator<?> keys = jsonPrimitiveContent.keys();
        while (keys.hasNext()) {

            String key = (String) keys.next();

            Object o = jsonPrimitiveContent.opt(key);

            switch (key) {

                case APP_NAME:
                case ONTOLOGY_REF:
                case APP_ID:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case NODE_LINK:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        } else {
                            // if it is a String, check whether it is a valid node
                            String nodeid = (String)o;
                            Onem2mResource nodeResource = Onem2mDb.getInstance().getResource(nodeid);
                            if (nodeResource == null ||
                                    !nodeResource.getResourceType().contentEquals(((Integer)Onem2m.ResourceType.NODE).toString())) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                        "CONTENT(" + RequestPrimitive.CONTENT + ") nodelink is not a valid node's resourceID");
                                return;
                            }
                        }
                    }
                    break;
                case REQUEST_REACHABILITY:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof Boolean)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") boolean expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case CONTENT_SERIALIZATION:
                    if (!jsonPrimitiveContent.isNull(key)) {
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
                    if (!jsonPrimitiveContent.isNull(key)) {
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
                case BaseResource.LABELS:
                case BaseResource.ACCESS_CONTROL_POLICY_IDS:
                case BaseResource.EXPIRATION_TIME:
                case BaseResource.RESOURCE_NAME:
                    if (!parseJsonCommonCreateUpdateContent(key)) {
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

    public void handleCreateUpdate() {

        parse(Onem2m.ResourceTypeString.AE);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        if (isJson()) {
            parseJsonCreateUpdateContent();
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
                return;
        }
        CheckAccessControlProcessor.handleCreateUpdate(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        processCommonCreateUpdateAttributes();
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        processCreateUpdateAttributes();
    }
}
