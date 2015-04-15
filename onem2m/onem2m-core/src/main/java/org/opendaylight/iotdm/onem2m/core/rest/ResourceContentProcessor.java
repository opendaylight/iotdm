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
     * This routine parses the ResourceContent.  The RequestPrimitive contains a resource specific representation
     * of a particular resource encoded in some format.  Each of the resource specific handlers is reponsible for
     * parsing its own content.
     *
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        String resourceType = onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE);
        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                ResourceAE.handleCreate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.handleCreate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.handleCreate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.handleCreate(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.handleCreate(onem2mRequest, onem2mResponse);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }
}
