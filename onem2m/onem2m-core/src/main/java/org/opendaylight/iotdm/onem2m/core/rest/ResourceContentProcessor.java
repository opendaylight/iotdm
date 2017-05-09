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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceContentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContentProcessor.class);

    private ResourceContentProcessor() {}

    private static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse,
                                    Onem2mDb.CseBaseResourceLocator resourceLocator, Integer resourceType) {
        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                ResourceAE resourceAE = new ResourceAE(onem2mRequest, onem2mResponse, resourceLocator);
                onem2mRequest.setBaseResource(resourceAE);
                resourceAE.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_AE_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_AE_UPDATE);
                }
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer resourceContainer = new ResourceContainer(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceContainer);
                resourceContainer.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTAINER_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTAINER_UPDATE);
                }
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance resourceContentInstance = new ResourceContentInstance(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceContentInstance);
                resourceContentInstance.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTENT_INSTANCE_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CONTENT_INSTANCE_UPDATE);
                }
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription resourceSubscription = new ResourceSubscription(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceSubscription);
                resourceSubscription.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_SUBSCRIPTION_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_SUBSCRIPTION_UPDATE);
                }
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse resourceCse = new ResourceCse(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceCse);
                resourceCse.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CSE_BASE_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_CSE_BASE_UPDATE);
                }
                break;
            case Onem2m.ResourceType.NODE:
                ResourceNode resourceNode = new ResourceNode(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceNode);
                resourceNode.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_NODE_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_NODE_UPDATE);
                }
                break;
            case Onem2m.ResourceType.GROUP:
                ResourceGroup resourceGroup = new ResourceGroup(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceGroup);
                resourceGroup.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_GROUP_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_GROUP_UPDATE);
                }
                break;
            case Onem2m.ResourceType.ACCESS_CONTROL_POLICY:
                ResourceAccessControlPolicy resourceAccessControlPolicy = new ResourceAccessControlPolicy(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceAccessControlPolicy);
                resourceAccessControlPolicy.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_ACCESS_CONTROL_POLICY_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_ACCESS_CONTROL_POLICY_UPDATE);
                }
                break;
            case Onem2m.ResourceType.REMOTE_CSE:
                ResourceRemoteCse resourceRemoteCse = new ResourceRemoteCse(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceRemoteCse);
                resourceRemoteCse.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_REMOTE_CSE_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_REMOTE_CSE_UPDATE);
                }
                break;
            case Onem2m.ResourceType.MGMT_OBJECT:
                ResourceMgmtObject resourceMgmtObject = new ResourceMgmtObject(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceMgmtObject);
                resourceMgmtObject.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_MGMT_OBJECT_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_MGMT_OBJECT_UPDATE);
                }
                break;
            case Onem2m.ResourceType.FLEX_CONTAINER:
                ResourceFlexContainer resourceFlexContainer = new ResourceFlexContainer(onem2mRequest, onem2mResponse);
                onem2mRequest.setBaseResource(resourceFlexContainer);
                resourceFlexContainer.handleCreateUpdate();
                if (onem2mRequest.isCreate) {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_FLEX_CONTAINER_CREATE);
                } else {
                    Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_FLEX_CONTAINER_UPDATE);
                }
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }

    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse,
                                    Onem2mDb.CseBaseResourceLocator resourceLocator) {
        onem2mRequest.isCreate = true;
        onem2mRequest.isUpdate = false;
        Integer resourceType = onem2mRequest.getPrimitiveResourceType();
        handleCreateUpdate(onem2mRequest, onem2mResponse, resourceLocator, resourceType);
    }

    /**
     * Collect stats on retrieve on a per resource type basis.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleRetrieveStats(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        Integer resourceType = onem2mRequest.getResourceType();
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
            case Onem2m.ResourceType.REMOTE_CSE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_REMOTE_CSE_RETRIEVE);
                break;
            case Onem2m.ResourceType.MGMT_OBJECT:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_MGMT_OBJECT_RETRIEVE);
                break;
            case Onem2m.ResourceType.FLEX_CONTAINER:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_FLEX_CONTAINER_RETRIEVE);
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
     * @param onem2mRequest  request
     * @param onem2mResponse response
     */
    public static void handleUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        Integer resourceType = onem2mRequest.getResourceType();
        onem2mRequest.isCreate = false;
        onem2mRequest.isUpdate = true;
        handleCreateUpdate(onem2mRequest, onem2mResponse, null, resourceType);
    }

    /**
     * Collect stats on retrieve on a per resource type basis.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleDeleteStats(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        Integer resourceType = onem2mRequest.getResourceType();
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
            case Onem2m.ResourceType.REMOTE_CSE:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_REMOTE_CSE_DELETE);
                break;
            case Onem2m.ResourceType.MGMT_OBJECT:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_MGMT_OBJECT_DELETE);
                break;
            case Onem2m.ResourceType.FLEX_CONTAINER:
                Onem2mStats.getInstance().inc(Onem2mStats.RESOURCE_FLEX_CONTAINER_DELETE);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                        "RESOURCE_TYPE(" + RequestPrimitive.RESOURCE_TYPE + ") not implemented (" + resourceType + ")");
                break;
        }
    }
}
