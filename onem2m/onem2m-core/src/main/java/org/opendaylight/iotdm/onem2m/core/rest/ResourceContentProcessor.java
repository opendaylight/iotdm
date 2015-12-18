/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
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
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_AE_CREATE);
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTAINER_CREATE);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTENT_INSTANCE_CREATE);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_SUBSCRIPTION_CREATE);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CSE_BASE_CREATE);
                break;
            case Onem2m.ResourceType.NODE:
                ResourceNode.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_NODE_CREATE);
                break;
            case Onem2m.ResourceType.GROUP:
                ResourceGroup.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_GROUP_CREATE);
                break;
            case Onem2m.ResourceType.ACCESS_CONTROL_POLICY:
                ResourceAccessControlPolicy.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_ACCESS_CONTROL_POLICY_CREATE);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }

    /**
     * Collect stats on retrieve on a per resource type basis.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleRetrieve(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String resourceType = onem2mRequest.getOnem2mResource().getResourceType();
        switch (resourceType) {
            case Onem2m.ResourceType.AE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_AE_RETRIEVE);
                break;
            case Onem2m.ResourceType.CONTAINER:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTAINER_RETRIEVE);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTENT_INSTANCE_RETRIEVE);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_SUBSCRIPTION_RETRIEVE);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CSE_BASE_RETRIEVE);
                break;
            case Onem2m.ResourceType.NODE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_NODE_RETRIEVE);
                break;
            case Onem2m.ResourceType.GROUP:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_GROUP_RETRIEVE);
                break;
            case Onem2m.ResourceType.ACCESS_CONTROL_POLICY:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_ACCESS_CONTROL_POLICY_RETRIEVE);
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

        //String resourceType = Onem2mDb.getInstance().getResourceType(onem2mRequest.getOnem2mResource());
        String resourceType = onem2mRequest.getOnem2mResource().getResourceType();

        onem2mRequest.isCreate = false;

        switch (resourceType) {
            case Onem2m.ResourceType.AE:
                ResourceAE.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_AE_UPDATE);
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTAINER_UPDATE);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTENT_INSTANCE_UPDATE);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_SUBSCRIPTION_UPDATE);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CSE_BASE_UPDATE);
                break;
            case Onem2m.ResourceType.NODE:
                ResourceNode.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_NODE_UPDATE);
                break;
            case Onem2m.ResourceType.GROUP:
                ResourceGroup.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_GROUP_UPDATE);
                break;
            case Onem2m.ResourceType.ACCESS_CONTROL_POLICY:
                ResourceAccessControlPolicy.handleCreateUpdate(onem2mRequest, onem2mResponse);
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_ACCESS_CONTROL_POLICY_UPDATE);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }

    /**
     * Collect stats on retrieve on a per resource type basis.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleDelete(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        String resourceType = onem2mRequest.getOnem2mResource().getResourceType();
        switch (resourceType) {
            case Onem2m.ResourceType.AE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_AE_DELETE);
                break;
            case Onem2m.ResourceType.CONTAINER:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTAINER_DELETE);
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTENT_INSTANCE_DELETE);
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_SUBSCRIPTION_DELETE);
                break;
            case Onem2m.ResourceType.CSE_BASE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CSE_BASE_DELETE);
                break;
            case Onem2m.ResourceType.NODE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_NODE_DELETE);
                break;
            case Onem2m.ResourceType.GROUP:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_GROUP_DELETE);
                break;
            case Onem2m.ResourceType.ACCESS_CONTROL_POLICY:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_ACCESS_CONTROL_POLICY_DELETE);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }
}
