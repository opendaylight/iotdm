/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
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
     * This routine parses the ResourceContent.  The RequestPrimitive contains a resource specific representation
     * of a particular resource encoded in some format.  Each of the resource specific handlers is responsible for
     * parsing its own content.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        onem2mRequest.isCreate = true;

        String resourceType = onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE);
        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                ResourceAE.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }

    /**
     * This routine parses the ResourceContent.  The RequestPrimitive contains a resource specific representation
     * of a particular resource encoded in some format.  Each of the resource specific handlers is responsible for
     * parsing its own content.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleRetrieve(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String pc = onem2mRequest.getPrimitive(RequestPrimitive.CONTENT);
        if (pc == null) {
            return;
        }
        String resourceType = Onem2mDb.getInstance().getResourceType(onem2mRequest.getOnem2mResource());

        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                ResourceAE.handleRetrieve(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.handleRetrieve(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.handleRetrieve(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.handleRetrieve(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.handleRetrieve(onem2mRequest, onem2mResponse);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }

    /**
     * This routine parses the ResourceContent.  The RequestPrimitive contains a resource specific representation
     * of a particular resource encoded in some format.  Each of the resource specific handlers is responsible for
     * parsing its own content.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String resourceType = Onem2mDb.getInstance().getResourceType(onem2mRequest.getOnem2mResource());

        onem2mRequest.isCreate = false;

        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                ResourceAE.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.handleCreateUpdate(onem2mRequest, onem2mResponse);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }
}
