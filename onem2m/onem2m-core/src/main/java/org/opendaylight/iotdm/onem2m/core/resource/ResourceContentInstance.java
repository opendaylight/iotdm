/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
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

        // no need to handle update requests in this file as update is not allowed for content instances

        // verify this resource can be created under the target resource
        String rt = onem2mRequest.getOnem2mResource().getResourceType();
        if (rt == null || !rt.contentEquals(Onem2m.ResourceType.CONTAINER)) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Cannot create ContentInstance under this resource type: " + rt);
            return;
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        tempStr = resourceContent.getInJsonContent().optString(CONTENT, null);
        if (tempStr == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CONTENT: missing parameter");
            return;
        } else {
            // record the size of the content in the CONTENT_SIZE attribute
            newByteSize = tempStr.length();
            JsonUtils.put(resourceContent.getInJsonContent(), CONTENT_SIZE, newByteSize);
        }

        JSONObject containerResourceContent = onem2mRequest.getJsonResourceContent();

        // the state tag of the cin is the incremented value of its parent container resource
        Integer containerStateTag = containerResourceContent.optInt(ResourceContent.STATE_TAG);
        containerStateTag++;
        JsonUtils.put(resourceContent.getInJsonContent(), ResourceContent.STATE_TAG, containerStateTag);

        // verify this content instance does not exceed the container's max byte size
        Integer mbs = containerResourceContent.optInt(ResourceContainer.MAX_BYTE_SIZE, -1);
        if (mbs != -1) {
            if (newByteSize > mbs) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "content size: " + newByteSize + " exceeds its containers max byte size: " + mbs);
                return;
            }

        }

        // special case: max_nr_instances == 0 --> seems like a way to block content instances from creation
        Integer mni = containerResourceContent.optInt(ResourceContainer.MAX_NR_INSTANCES, -1);
        if (mni != -1) {
            if (mni < 1) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Exceeds containers max instances: " + mni);
                return;
            }

        }

        // set the ExpirationTime to the minimum value, curruntTime + parent container's maxInstanceAge
        Integer mia = containerResourceContent.optInt(ResourceContainer.MAX_INSTANCE_AGE, -1);
        if (mia != -1) {
            String minExpTime = Onem2mDateTime.addAgeToCurTime(mia);
            String cinExpTime = onem2mRequest.getResourceContent().getInJsonContent().optString(ResourceContent.EXPIRATION_TIME);
            if ( cinExpTime!= null && Onem2mDateTime.dateCompare(minExpTime, cinExpTime) < 0) {
                JsonUtils.put(resourceContent.getInJsonContent(), ResourceContent.EXPIRATION_TIME, minExpTime);
            }
        }

        ResourceContainer.setCurrValuesForThisCreatedContentInstance(onem2mRequest, containerResourceContent,
                newByteSize);

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
    private static void parseJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getInJsonContent().keys();
        while( keys.hasNext() ) {

            String key = (String)keys.next();

            resourceContent.jsonCreateKeys.add(key);

            Object o = resourceContent.getInJsonContent().opt(key);

            switch (key) {

                case CONTENT_SIZE:
                case ResourceContent.STATE_TAG:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, key + ": read-only parameter");
                    return;

                case CONTENT_INFO:
                case ONTOLOGY_REF:
                case CONTENT:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case CREATOR:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
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

                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
            if (resourceContent.jsonCreateKeys.contains(CREATOR)) {
                JsonUtils.put(resourceContent.getInJsonContent(), CREATOR, onem2mRequest.getPrimitive(RequestPrimitive.FROM));
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

        resourceContent.parse(Onem2m.ResourceTypeString.CONTENT_INSTANCE, onem2mRequest, onem2mResponse);
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

        ResourceContentInstance.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);
    }
}