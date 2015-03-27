/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.*;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContentProcessor.class);

    public ResourceContentProcessor() {}

    /**
     * This routine parses the RequestContent.  It may call resource specific classes.  This
     * data is parsed on
     *
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        String resourceType = onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE);
        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                onem2mRequest.setValidAttributes(ResourceAE.createAttributes);
                resourceContent.parseRequestContent(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                handleCommonCreateAttributes(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                ResourceAE.handleCreate(onem2mRequest, onem2mResponse);
                break;

            case Onem2m.ResourceType.CONTAINER:
                onem2mRequest.setValidAttributes(ResourceContainer.createAttributes);
                resourceContent.parseRequestContent(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                handleCommonCreateAttributes(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                ResourceContainer.handleCreate(onem2mRequest, onem2mResponse);
                break;

            case Onem2m.ResourceType.CONTENT_INSTANCE:
                onem2mRequest.setValidAttributes(ResourceContentInstance.createAttributes);
                resourceContent.parseRequestContent(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                handleCommonCreateAttributes(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                ResourceContentInstance.handleCreate(onem2mRequest, onem2mResponse);
                break;

            case Onem2m.ResourceType.SUBSCRIPTION:
                onem2mRequest.setValidAttributes(ResourceSubscription.createAttributes);
                resourceContent.parseRequestContent(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                handleCommonCreateAttributes(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                ResourceSubscription.handleCreate(onem2mRequest, onem2mResponse);
                break;

            case Onem2m.ResourceType.CSE_BASE:
                onem2mRequest.setValidAttributes(ResourceSubscription.createAttributes);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                handleCommonCreateAttributes(onem2mRequest, onem2mResponse);
                if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                    return;
                ResourceCse.handleCreate(onem2mRequest, onem2mResponse);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }

    private static void handleCommonCreateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        resourceContent.setDbAttr(ResourceContent.RESOURCE_TYPE, onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE));

        // resourceId, resourceName, parentId is filled in by the Onem2mDb.createResource method

        String currDateTime = Onem2mDateTime.getCurrDateTime();

        String ct = resourceContent.getDbAttr(ResourceContent.CREATION_TIME);
        if (ct != null) {
            if (!Onem2mDateTime.isValidDateTime(ct)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Invalid ISO8601 date/time format: (YYYY-MM-DD'T'HH:MM:SSZZ) " + ct);
                return;
            }
        } else {
            resourceContent.setDbAttr(ResourceContent.CREATION_TIME, currDateTime);
        }

        // if expiration time not provided, make one
        String et = resourceContent.getDbAttr(ResourceContent.EXPIRATION_TIME);
        if (et != null) {
            if (!Onem2mDateTime.isValidDateTime(et)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Invalid ISO8601 date/time format: (YYYY-MM-DD'T'HH:MM:SSZZ) " + et);
                return;
            }
        } else {
            resourceContent.setDbAttr(ResourceContent.EXPIRATION_TIME, currDateTime);
        }

        String lmt = resourceContent.getDbAttr(ResourceContent.LAST_MODIFIED_TIME);
        if (lmt != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "LAST_MODIFIED_TIME: read-only parameter");
            return;
        }
        resourceContent.setDbAttr(ResourceContent.LAST_MODIFIED_TIME, currDateTime);
    }
}
