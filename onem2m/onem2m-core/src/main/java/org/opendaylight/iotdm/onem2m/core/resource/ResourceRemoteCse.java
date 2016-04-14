/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;

import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                case ANNOUNCE_TO:
                    // TODO rework this, list is expected
//                	if (!resourceContent.getInJsonContent().isNull(key)) {
//                        if (!(o instanceof String)) {
//                          onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
//                          "CONTENT(" + RequestPrimitive.CONTENT + ") CSE name expected for json key: " + key);
//                          return;
//                         }
//                       }
                	break;
                case ANNOUNCED_ATTRIBUTE:
                    // TODO rework this, list is expected
//                	if (!resourceContent.getInJsonContent().isNull(key)) {
//                        if (!(o instanceof String)) {
//                          onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
//                          "CONTENT(" + RequestPrimitive.CONTENT + ") remoteCSE name expected for json key: " + key);
//                          return;
//                         }
//                       }
                	break;
                case CSE_TYPE:
                case POINT_OF_ACCESS:
                case CSE_BASE:
                case CSE_ID:
                	if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                          onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                          "CONTENT(" + RequestPrimitive.CONTENT + ") remoteCSE ID expected for json key: " + key);
                          return;
                         }
                       }
                	break;
                case M2M_EXT_ID:
                case TRIGGER_RECIPIENT_ID:
                case NODE_LINK:
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
                    if (!ResourceContent.parseJsonCommonCreateUpdateContent(key,
                            resourceContent,
                            onem2mResponse)) {
                        return;
                    }
                    break;
              
                case ResourceContent.CREATION_TIME:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case ResourceContent.LABELS:
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
    
    private static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // ensure resource can be created under the target resource
        if (onem2mRequest.isCreate) {
            String parentResourceType = onem2mRequest.getOnem2mResource().getResourceType();
            if (parentResourceType == null || !parentResourceType.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create remoteCSE under this resource type: " + parentResourceType);
                return;
           }
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
            String defaultAT = Onem2mDb.getInstance().getChildResourceID(CSEid,"_defaultACP");
            resourceContent.getInJsonContent().append(ANNOUNCE_TO,defaultAT);

        }

        // TODO remove this, aa is not mandatory
//        String aa = resourceContent.getInJsonContent().optString(ANNOUNCED_ATTRIBUTE, null);
//        if (aa == null && onem2mRequest.isCreate) {
//            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Announced_Attribute  missing parameter");
//            return;
//        }

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
     * Parse the CONTENT resource representation for create and update
     *
     * @param onem2mRequest  request
     * @param onem2mResponse response
     */
    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.parse(Onem2m.ResourceTypeString.REMOTE_CSE, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            parseJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }

        resourceContent.processCommonCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        ResourceRemoteCse.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        
    }
   
	

}
