/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class ResourceNode {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceNode.class);
    private ResourceNode() {}

    /**
     * These attributes are "specific" attributes only belong to ResourceNode.
     * Some other common attributes can be found in file "ResourceContent" at the same folder.
     */
    public static final String NODE_ID = "ni";
    public static final String HOSTED_CSEID = "hci";


    /**
     * Parse the Json input, the Restful json payload
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

            resourceContent.jsonCreateKeys.add(key);  // this line is new

            Object o = resourceContent.getInJsonContent().opt(key);

            switch (key) {

                case NODE_ID:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case HOSTED_CSEID:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case ResourceContent.LABELS:
                case ResourceContent.EXPIRATION_TIME:
                case ResourceContent.RESOURCE_NAME:
                    if (!ResourceContent.parseJsonCommonCreateUpdateContent(key,
                            resourceContent,
                            onem2mResponse)) {
                        return;
                    }
                    break;

                //todo: will need to add "announceTo" "announceAttribute" later, currently we do not support that

                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
        }
    }


    /**
     * Ensure the create/update parameters follow the rules
     * Check the logic in this step. After the json check, code goes here, if other attributes in the "ResourceContent"
     * or other resources are affected, add logic codes here
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {


        // verify this resource can be created under the target resource
        ResourceContent resourceContent = onem2mRequest.getResourceContent();
        /**
         * see other resources example to see how to check more complicated case
         */
        if (onem2mRequest.isCreate) {
            // target resourceType
            String rt = onem2mRequest.getOnem2mResource().getResourceType();
            if (rt == null || !(rt.contentEquals(Onem2m.ResourceType.CSE_BASE) )) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create Container under this resource type: " + rt);
                return;
            }
        }

        /**
         * Check the mandotory attribtue's value
         */
        String ni = resourceContent.getInJsonContent().optString(NODE_ID, null);
        if (ni == null && onem2mRequest.isCreate) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "NODE_ID missing parameter");
            return;
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
//            // may have to remove content instances as mbs and mni may have been reduced
//            ResourceContainer.checkAndFixCurrMaxRules(onem2mRequest.getPrimitive(RequestPrimitive.TO));
//            Onem2mResource containerResource = Onem2mDb.getInstance().getResource(onem2mRequest.getResourceId());
//            onem2mRequest.setOnem2mResource(containerResource);
//            onem2mRequest.setDbAttrs(new DbAttr(onem2mRequest.getOnem2mResource().getAttr()));
//            onem2mRequest.setDbAttrSets(new DbAttrSet(onem2mRequest.getOnem2mResource().getAttrSet()));
        }
    }

    /**
     * Parse the CONTENT resource representation.
     * This is the entrance of this resource
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();
        /**
         * Need to add a new resource in the "Onem2m.ResourceTypeString";
         */
        resourceContent.parse(Onem2m.ResourceTypeString.NODE, onem2mRequest, onem2mResponse);
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
        ResourceNode.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }

}