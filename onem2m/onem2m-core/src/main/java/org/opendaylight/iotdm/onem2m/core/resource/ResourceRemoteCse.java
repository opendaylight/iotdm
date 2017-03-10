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

public class ResourceRemoteCse extends BaseResource {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceCse.class);
    public ResourceRemoteCse(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        super(onem2mRequest, onem2mResponse);
    }
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
    
    private void parseJsonCreateUpdateContent() {
        
        Iterator<?> keys = jsonPrimitiveContent.keys();
        while( keys.hasNext() ) {

            String key = (String)keys.next();

            Object o = jsonPrimitiveContent.get(key);

            switch (key) {

                case BaseResource.RESOURCE_ID:
                     if (!jsonPrimitiveContent.isNull(key)) {
                       if (!(o instanceof String)) {
                         onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                         "CONTENT(" + RequestPrimitive.CONTENT + ") Numeric string value expected for json key: " + key);
                         return;
                        }
                      }
                     break;
                case BaseResource.PARENT_ID:
                    if (!jsonPrimitiveContent.isNull(key)) {
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
                    if (!jsonPrimitiveContent.isNull(key)) {
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
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                          onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                          "CONTENT(" + RequestPrimitive.CONTENT + ") remoteCSE ID expected for json key: " + key);
                          return;
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
                case BaseResource.ACCESS_CONTROL_POLICY_IDS:
                case BaseResource.EXPIRATION_TIME:
                case BaseResource.LABELS:
                case BaseResource.RESOURCE_NAME:
                    if (!parseJsonCommonCreateUpdateContent(key)) {
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

    private void processCreateUpdateAttributes() {

        String cseBaseName = null;
        // ensure resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            Integer prt = onem2mRequest.getParentResourceType();
            if (prt != Onem2m.ResourceType.CSE_BASE) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create remoteCSE under this resource type: " + prt);
                return;
           }
           cseBaseName = onem2mRequest.getParentOnem2mResource().getName();
        }

        /**
         * CSE_ID is checked at parse time ... no need to check for it here
         */

        String csi = jsonPrimitiveContent.optString(CSE_ID, null);
        if (csi == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE_ID missing parameter");
            return;
        } else if (csi != null && !onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CSE_ID cannot be updated");
            return;
        }

        String rr = jsonPrimitiveContent.optString(REQUEST_REACHABILITY, null);
        if (rr == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "REQUEST_REACHABILITY missing parameter");
            return;
        }

        String at = jsonPrimitiveContent.optString(ANNOUNCE_TO, null);
        if (at != null && onem2mRequest.isCreate) {
            // store the default ACPID info into this place.
            String CSEid = onem2mRequest.getOnem2mResource().getResourceId();
            // to should be an CSE
            String defaultAT = Onem2mDb.getInstance().getChildResourceID(CSEid,"_defaultACP");
            jsonPrimitiveContent.append(ANNOUNCE_TO,defaultAT);
        }

        /**
         * The resource has been filled in with any attributes that need to be written to the database
         */
        if (onem2mRequest.isCreate) {

            if (!Onem2mDb.getInstance().createResourceRemoteCse(onem2mRequest, onem2mResponse,
                                                                cseBaseName, csi)) {
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

        /*
         * Update routing table with the changes
         */
        Onem2mRouterService.getInstance().updateRoutingTable(onem2mRequest);
    }

    public void handleCreateUpdate() {

        parse(Onem2m.ResourceTypeString.REMOTE_CSE);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        if (isJson()) {
            parseJsonCreateUpdateContent();
            if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
                return;
        }
        //CheckAccessControlProcessor.handleCreateUpdate(trc, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;
        processCommonCreateUpdateAttributes();
        if (onem2mResponse.getPrimitiveResponseStatusCode() != null)
            return;

        processCreateUpdateAttributes();
    }
}
