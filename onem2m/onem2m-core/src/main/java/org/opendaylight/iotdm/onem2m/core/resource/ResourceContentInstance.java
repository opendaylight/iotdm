/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.Iterator;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContentInstance extends BaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContentInstance.class);

    private Integer newByteSize;

    public ResourceContentInstance(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        super(onem2mRequest, onem2mResponse);
    }

    // taken from CDT-contentInstance-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
    // TODO: ts0001 9.6.7-1

    public static final String CONTENT_INFO = "cnf";
    public static final String CONTENT_SIZE = "cs";
    public static final String CONTENT = "con";
    public static final String ONTOLOGY_REF = "or";
    public static final String CREATOR = "cr";
    public static final String LABELS = "lbl";

    private void processCreateUpdateAttributes() {

        String tempStr;

        // no need to handle update requests in this file as update is not allowed for content instances

        // verify this resource can be created under the target resource
        Integer prt = onem2mRequest.getParentResourceType();
        if (prt != Onem2m.ResourceType.CONTAINER) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                    "Cannot create ContentInstance under this resource type: " + prt);
            return;
        }

        tempStr = jsonPrimitiveContent.optString(CONTENT, null);
        if (tempStr == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "CONTENT: missing parameter");
            return;
        } else {
            // record the size of the content in the CONTENT_SIZE attribute
            newByteSize = tempStr.length();
            JsonUtils.put(jsonPrimitiveContent, CONTENT_SIZE, newByteSize);
        }

        JSONObject containerResourceContent = onem2mRequest.getParentJsonResourceContent();

        // the state tag of the cin is the incremented value of its parent container resource
        Integer containerStateTag = containerResourceContent.optInt(BaseResource.STATE_TAG);
        containerStateTag++;
        JsonUtils.put(jsonPrimitiveContent, BaseResource.STATE_TAG, containerStateTag);

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

        // set the ExpirationTime to the minimum value, currentTime + parent container's maxInstanceAge
        Integer mia = containerResourceContent.optInt(ResourceContainer.MAX_INSTANCE_AGE, -1);
        if (mia != -1) {
            String minExpTime = Onem2mDateTime.addAgeToCurTime(mia);
            String cinExpTime = jsonPrimitiveContent.optString(BaseResource.EXPIRATION_TIME);
            if (cinExpTime!= null && Onem2mDateTime.dateCompare(minExpTime, cinExpTime) < 0) {
                JsonUtils.put(jsonPrimitiveContent, BaseResource.EXPIRATION_TIME, minExpTime);
            }
        }

        onem2mRequest.setParentContentHasBeenModified(true);

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

    private void parseJsonCreateUpdateContent() {

        boolean creatorPresent = false;

        Iterator<?> keys = jsonPrimitiveContent.keys();
        while( keys.hasNext() ) {

            String key = (String)keys.next();

            Object o = jsonPrimitiveContent.opt(key);

            switch (key) {

                case CONTENT_SIZE:
                case BaseResource.STATE_TAG:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, key + ": read-only parameter");
                    return;

                case CONTENT_INFO:
                case ONTOLOGY_REF:
                case CONTENT:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;
                case CREATOR:
                    if (!jsonPrimitiveContent.isNull(key)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") CREATOR must be null");
                        return;
                    }
                    creatorPresent = true;
                    break;
                case BaseResource.LABELS:
                case BaseResource.EXPIRATION_TIME:
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
        if (creatorPresent) {
            JsonUtils.put(jsonPrimitiveContent, CREATOR, onem2mRequest.getPrimitiveFrom());
        }
    }

    public void handleCreateUpdate() {

        parse(Onem2m.ResourceTypeString.CONTENT_INSTANCE);
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
