/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.*;

import org.json.JSONArray;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;

public class ResourceRemoteCse {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceCse.class);
    private ResourceRemoteCse() {}

    // taken from CDT-cseBase-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: TS0001 9.6.3 ResourceType RemoteCSE


    public static final String ANNOUNCE_TO = "at";
    public static final String ANNOUNCED_ATTRIBUTE = "aa";
    public static final String CSE_TYPE = "cst";
    public static final String POINT_OF_ACCESS = "poa";
    public static final String CSE_BASE = "cb";
    public static final String CSE_ID = "csi";
    public static final String M2M_EXT_ID = "mei";
    public static final String TRIGGER_RECIPIENT_ID = "tri";
    public static final String NODE_LINK = "nl"; // do not support node resource yet
    public static final String REQUEST_REACHABILITY = "rr";

    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void parseJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getInJsonContent().keys();
        while( keys.hasNext() ) {

            String key = (String)keys.next();

            resourceContent.jsonCreateKeys.add(key);

            Object o = resourceContent.getInJsonContent().get(key);

            switch (key) {

                case ResourceContent.RESOURCE_ID:
                     if (!resourceContent.getInJsonContent().isNull(key)) {
                       if (!(o instanceof String)) {
                         onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                         "CONTENT(" + RequestPrimitive.CONTENT + ") Numeric string value expected for json key: " + key);
                         return;
                        }
                      }
                     break;
                case ResourceContent.PARENT_ID:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                          onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                          "CONTENT(" + RequestPrimitive.CONTENT + ") ParentID path expected for json key: " + key);
                          return;
                         }
                       }
                    break;

                case ANNOUNCED_ATTRIBUTE:
                case ANNOUNCE_TO:
                case POINT_OF_ACCESS:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof JSONArray)) {
                          onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                          "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                          return;
                         }
                    }
                    break;

                case CSE_TYPE:
                case CSE_BASE:
                case CSE_ID:
                case M2M_EXT_ID:
                case TRIGGER_RECIPIENT_ID:
                case NODE_LINK:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                          onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                          "CONTENT(" + RequestPrimitive.CONTENT + ") remoteCSE ID expected for json key: " + key);
                          return;
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
                case ResourceContent.ACCESS_CONTROL_POLICY_IDS:
                case ResourceContent.EXPIRATION_TIME:
                case ResourceContent.LABELS:
                case ResourceContent.RESOURCE_NAME:
                    if (!ResourceContent.parseJsonCommonCreateUpdateContent(key,
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

    private static void processCreateUpdateAttributes(ResourceTreeWriter twc, ResourceTreeReader trc,
                                                      RequestPrimitive onem2mRequest,
                                                      ResponsePrimitive onem2mResponse) {

        String cseBaseName = null;
        // ensure resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            String parentResourceType = onem2mRequest.getOnem2mResource().getResourceType();
            if (parentResourceType == null || !parentResourceType.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create remoteCSE under this resource type: " + parentResourceType);
                return;
           }
           cseBaseName = onem2mRequest.getOnem2mResource().getName();
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        /**
         * CSE_ID is checked at parse time ... no need to check for it here
         */

        String csi = resourceContent.getInJsonContent().optString(CSE_ID, null);
        if (csi == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE_ID missing parameter");
            return;
        } else if (csi != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE_ID cannot be updated");
            return;
        }

        String rr = resourceContent.getInJsonContent().optString(REQUEST_REACHABILITY, null);
        if (rr == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "REQUEST_REACHABILITY missing parameter");
            return;
        }

        String at = resourceContent.getInJsonContent().optString(ANNOUNCE_TO, null);
        if (at != null && onem2mRequest.isCreate) {
            // store the default ACPID info into this place.
            String CSEid = onem2mRequest.getOnem2mResource().getResourceId();
            // to should be an CSE
            String defaultAT = Onem2mDb.getInstance().getChildResourceID(trc, CSEid,"_defaultACP");
            resourceContent.getInJsonContent().append(ANNOUNCE_TO,defaultAT);
        }

        /**
         * The resource has been filled in with any attributes that need to be written to the database
         */
        if (onem2mRequest.isCreate) {

            if (!Onem2mDb.getInstance().createResourceRemoteCse(twc, trc, onem2mRequest, onem2mResponse,
                                                                cseBaseName, csi)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot write to data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }

        } else {
            if (!Onem2mDb.getInstance().updateResource(twc, trc, onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot write to data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        }

        /*
         * Update routing table with the changes
         */
        Onem2mRouterService.getInstance().updateRoutingTable(trc, onem2mRequest);
    }

    /**
     * Parse the CONTENT resource representation for create and update
     * @param twc database writer interface
     * @param trc database reader interface
     * @param onem2mRequest  request primitive
     * @param onem2mResponse response primitive
     */
    public static void handleCreateUpdate(ResourceTreeWriter twc, ResourceTreeReader trc, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(Onem2m.ResourceTypeString.REMOTE_CSE, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            parseJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }
        CheckAccessControlProcessor.handleCreateUpdate(trc, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;
        resourceContent.processCommonCreateUpdateAttributes(trc, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        ResourceRemoteCse.processCreateUpdateAttributes(twc, trc, onem2mRequest, onem2mResponse);
    }
}
