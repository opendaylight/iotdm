/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.FilterCriteria;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Child;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultContentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ResultContentProcessor.class);

    private ResultContentProcessor() {}

    /**
     * This routine uses the result content, and filter criteria to gather information to return in the
     * ResponsePrimitive onem2mResponse.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleRetrieve(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        produceJsonResultContent(onem2mRequest, onem2mResponse);
    }

    /**
     * This routine uses the result content, and filter criteria to gather information to return in the
     * ResponsePrimitive onem2mResponse.  See TS0001 Section 8.1.2 Request .. ResultContent
     *
     * @param onem2mRequest the set of request primitives
     * @param onem2mResponse the set of result primitives
     */
    private static void produceJsonResultContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        onem2mResponse.setUseHierarchicalAddressing(true);
        String drt = onem2mRequest.getPrimitive(RequestPrimitive.DISCOVERY_RESULT_TYPE);
        if (drt != null) {
            if (drt.contentEquals(Onem2m.DiscoveryResultType.NON_HIERARCHICAL)) {
                onem2mResponse.setUseHierarchicalAddressing(false);
            }
        }

        JSONObject jContent = new JSONObject();
        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();

        // cache the resourceContent so resultContent options can be restricted
        onem2mResponse.setResourceContent(onem2mRequest.getResourceContent());

        String rc = onem2mRequest.getPrimitive(RequestPrimitive.RESULT_CONTENT);
        if (rc == null) {
            rc = Onem2m.ResultContent.ATTRIBUTES;
        }
        switch (rc) {
            case Onem2m.ResultContent.NOTHING:
                return; // that was easy
            case Onem2m.ResultContent.ATTRIBUTES:
                produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jContent);
                break;
            case Onem2m.ResultContent.HIERARCHICAL_ADDRESS:
                produceJsonResultContentHierarchicalAddress(onem2mRequest, onem2mResource, onem2mResponse, jContent);
                break;
            case Onem2m.ResultContent.HIERARCHICAL_ADDRESS_ATTRIBUTES:
                produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jContent);
                break;
            case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCES:
                produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jContent);
                produceJsonResultContentChildResources(onem2mRequest, onem2mResource, onem2mResponse, jContent);
                break;
            case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS:
                produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jContent);
                produceJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jContent);
                break;
            case Onem2m.ResultContent.CHILD_RESOURCE_REFS:
                produceJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jContent);
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") invalid option: (" + rc + ")");
                return;
        }

        if (onem2mResponse.useJsonAnySyntax()) {
            // now we need to create primitiveContent for this internal content
            JSONObject j = new JSONObject();
            JSONArray anyArray = new JSONArray();
            anyArray.put(jContent);
            j.put("any", anyArray);

            onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, j.toString());
        } else {
            onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jContent.toString());

        }
    }

    /**
     * This routine uses the result content, and filter criteria to gather information to return in the
     * ResponsePrimitive onem2mResponse.
     *
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void createXmlResultContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // this request has been pre checked so this is really a placeholder for when we support XML
        LOG.error("createXmlResultContent not implemented!");
    }

    /**
     * Find the hierarchical name for this resource and return it.
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static void produceJsonResultContentHierarchicalAddress(RequestPrimitive onem2mRequest,
                                                                    Onem2mResource onem2mResource,
                                                                    ResponsePrimitive onem2mResponse,
                                                                    JSONObject j) {

        String h = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId());

        j.put(ResourceContent.RESOURCE_NAME, h);
    }

    /**
     * Fill in the attributes of this resource.  Apply any filter criteria, and use the appropriate addressing.
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static void produceJsonResultContentAttributes(RequestPrimitive onem2mRequest,
                                                           Onem2mResource onem2mResource,
                                                           ResponsePrimitive onem2mResponse,
                                                           JSONObject j) {

        String resourceType = Onem2mDb.getInstance().getResourceType(onem2mResource);

        if (FilterCriteria.matches(onem2mRequest, onem2mResource, resourceType)) {

            String id;

            id = Onem2mDb.getInstance().getNonHierarchicalNameForResource(onem2mResource.getParentId());
            if (id != null) {
                j.put(ResourceContent.PARENT_ID, id);
            }

            id = Onem2mDb.getInstance().getNonHierarchicalNameForResource(onem2mResource.getResourceId());
            j.put(ResourceContent.RESOURCE_ID, id);

            String name;
            if (onem2mResponse.useHierarchicalAddressing()) {
                name = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId());
            } else {
                name = Onem2mDb.getInstance().getNonHierarchicalNameForResource(onem2mResource.getResourceId());
            }
            j.put(ResourceContent.RESOURCE_NAME, name);

            // TODO: might have to filter attributes based on CONTENT (eg get can specify which attrs)

            ResourceContent.produceJsonForResource(resourceType, onem2mResource, j);
        }
    }

    /**
     * Format a list of the child references.  A child reference is either the non-h or hierarchical version of the
     * reference to the resourceId.  This conceivable could be a lot of references so TODO I think I need a system
     * variable with a MAX_LIMIT.
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static void produceJsonResultContentChildResourceRefs(RequestPrimitive onem2mRequest,
                                                                  Onem2mResource onem2mResource,
                                                                  ResponsePrimitive onem2mResponse,
                                                                  JSONObject j) {

        String h = null;
        JSONArray ja = new JSONArray();
        List<Child> childList = onem2mResource.getChild();
        for (Child child : childList) {

            String resourceId = child.getResourceId();

            Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);
            String resourceType = Onem2mDb.getInstance().getResourceType(childResource);
            if (!FilterCriteria.matches(onem2mRequest, childResource, resourceType)) {
                continue;
            }

            JSONObject childRef = new JSONObject();
            if (onem2mResponse.useHierarchicalAddressing()) {
                h = Onem2mDb.getInstance().getHierarchicalNameForResource(resourceId);
            } else {
                h = Onem2mDb.getInstance().getNonHierarchicalNameForResource(resourceId);
            }
            childRef.put(ResourceContent.RESOURCE_NAME, h);
            childRef.put(ResourceContent.RESOURCE_TYPE, Integer.valueOf(resourceType));
            ja.put(childRef);

        }
        j.put(ResourceContent.CHILD_RESOURCE_REF, ja);
    }

    /**
     * Format a list of the child references.  A child reference is either the non-h or hierarchical version of the
     * reference to the resourceId.  This conceivable could be a lot of references so TODO I think I need a system
     * variable with a MAX_LIMIT.
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static void produceJsonResultContentChildResources(RequestPrimitive onem2mRequest,
                                                               Onem2mResource onem2mResource,
                                                               ResponsePrimitive onem2mResponse,
                                                               JSONObject j) {

        String h = null;
        JSONArray ja = new JSONArray();
        List<Child> childList = onem2mResource.getChild();
        for (Child child : childList) {

            String resourceId = child.getResourceId();
            Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);

            // only include the resources that pass filter criteria
            String resourceType = Onem2mDb.getInstance().getResourceType(childResource);
            if (!FilterCriteria.matches(onem2mRequest, childResource, resourceType)) {
                continue;
            }

            JSONObject attrs = new JSONObject();
            onem2mResource = Onem2mDb.getInstance().getResource(resourceId);
            produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, attrs);
            ja.put(attrs);

        }
        j.put(ResourceContent.CHILD_RESOURCE, ja);
    }


    /**
     * The results of the create now must be put in the response.  The result content is used to decide how the
     * results should be formatted.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        produceJsonResultContent(onem2mRequest, onem2mResponse);
    }

    /**
     * The results of the create now must be put in the response.  The result content is used to decide how the
     * results should be formatted.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        produceJsonResultContent(onem2mRequest, onem2mResponse);
    }

    /**
     * The results of the delete now must be put in the response.  The result content is used to decide how the
     * results should be formatted.
     *
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public static void handleDelete(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        produceJsonResultContent(onem2mRequest, onem2mResponse);
    }
}
