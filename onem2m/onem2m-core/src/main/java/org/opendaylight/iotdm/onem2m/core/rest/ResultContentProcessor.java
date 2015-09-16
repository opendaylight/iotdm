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
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
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

        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();

        // protocol specific info for all rcn values is done here
        String protocol = onem2mRequest.getPrimitive(RequestPrimitive.PROTOCOL);
        if (protocol != null && protocol.contentEquals(Onem2m.Protocol.HTTP)) {
            if (onem2mRequest.getPrimitive(RequestPrimitive.OPERATION).contentEquals(Onem2m.Operation.CREATE)) {
                onem2mResponse.setPrimitive(ResponsePrimitive.HTTP_CONTENT_LOCATION,
                        Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId()));
            }
            onem2mResponse.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE,
                    Onem2m.ContentType.APP_VND_RES_JSON + ";" + RequestPrimitive.RESOURCE_TYPE + "=" +
                            Onem2mDb.getInstance().getResourceType(onem2mResource));
        }

        JSONObject jo = new JSONObject(); // for non-fu discovery
        JSONArray ja = new JSONArray();   // for fu discovery

        // cache the resourceContent so resultContent options can be restricted
        onem2mResponse.setResourceContent(onem2mRequest.getResourceContent());

        String rc = onem2mRequest.getPrimitive(RequestPrimitive.RESULT_CONTENT);
        if (rc == null) {
            if (onem2mRequest.getPrimitive(RequestPrimitive.OPERATION).contentEquals(Onem2m.Operation.RETRIEVE)) {
                rc = Onem2m.ResultContent.ATTRIBUTES;
            } else {
                rc = Onem2m.ResultContent.ATTRIBUTES;
            }
        }
        switch (rc) {
            case Onem2m.ResultContent.NOTHING:
                onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                return; // that was easy

            case Onem2m.ResultContent.ATTRIBUTES:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, ja);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, ja.toString());
                } else {
                    jo = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                }
                break;

            case Onem2m.ResultContent.HIERARCHICAL_ADDRESS:
                produceJsonResultContentHierarchicalAddress(onem2mRequest, onem2mResource, onem2mResponse, jo);
                onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                break;

            case Onem2m.ResultContent.HIERARCHICAL_ADDRESS_ATTRIBUTES:
                onem2mResponse.setUseHierarchicalAddressing(true);
                jo = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse);
                // add the address
                produceJsonResultContentHierarchicalAddress(onem2mRequest, onem2mResource, onem2mResponse, jo);
                onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                break;

            case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCES:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentAttributesAndChildResources(onem2mRequest, onem2mResource, onem2mResponse, ja);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, ja.toString());
                } else {
                    jo = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse);
                    produceJsonResultContentChildResources(onem2mRequest, onem2mResource, onem2mResponse, jo);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                }
                break;

            case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, ja, true);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, ja.toString());
                } else {
                    jo = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse);
                    produceJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jo);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                }
                break;

            case Onem2m.ResultContent.CHILD_RESOURCE_REFS:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, ja, false);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, ja.toString());
                } else {
                    produceJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jo);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                }
                break;

            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") invalid option: (" + rc + ")");
                return;
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

        j.put(ResourceContent.MEMBER_URI, h);
    }

    /**
     * Fill in the attributes of this resource.  Apply any filter criteria, and use the appropriate addressing.
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static JSONObject produceJsonResultContentAttributes(RequestPrimitive onem2mRequest,
                                                           Onem2mResource onem2mResource,
                                                           ResponsePrimitive onem2mResponse) {

        String resourceType = Onem2mDb.getInstance().getResourceType(onem2mResource);

        if (FilterCriteria.matches(onem2mRequest, onem2mResource, resourceType)) {

            String id;
            JSONObject j = new JSONObject();

            id = Onem2mDb.getInstance().getNonHierarchicalNameForResource(onem2mResource.getParentId());
            if (id != null) {
                j.put(ResourceContent.PARENT_ID, id);
            }

//            id = Onem2mDb.getInstance().getNonHierarchicalNameForResource(onem2mResource.getResourceId());
            id = onem2mResource.getResourceId();
            j.put(ResourceContent.RESOURCE_ID, id);

            String name;
//            if (onem2mResponse.useHierarchicalAddressing()) {
//                name = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId());
//            } else {
//                name = Onem2mDb.getInstance().getNonHierarchicalNameForResource(onem2mResource.getResourceId());
//            }
//            if (onem2mRequest.isCreate && onem2mRequest.NAME)


            // TODO: might have to filter attributes based on CONTENT (eg get can specify which attrs)

            if (onem2mRequest.isCreate) {
                // if the create contains NAME in the header, do not return name
                if (onem2mRequest.getPrimitive(RequestPrimitive.NAME) == null) {
                    name = onem2mResource.getName();
                    j.put(ResourceContent.RESOURCE_NAME, name);
                }
                j = ResourceContent.produceJsonForResourceCreate(resourceType, onem2mResource, onem2mResponse.useM2MPrefix(), j);
            } else {
                // TODO: if need to support update return less attribute, need to add "isUpdate"
                name = onem2mResource.getName();
                j.put(ResourceContent.RESOURCE_NAME, name);
                j = ResourceContent.produceJsonForResource(resourceType, onem2mResource, onem2mResponse.useM2MPrefix(), j);
            }
            return j;
        }

        return null;
    }

    /**
     * Start at the root resource and find a hierarchical set of resources then generate the attributes for each of those
     * resources in an "any" array of json objects where each json object is the set of resource specific attrs
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static void discoveryJsonResultContentAttributes(RequestPrimitive onem2mRequest,
                                                             Onem2mResource onem2mResource,
                                                             ResponsePrimitive onem2mResponse,
                                                             JSONArray ja) {
        Integer lim = 0;
        Integer count = 0;
        String limStr = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT);
        if (limStr != null) {
            lim = Integer.parseInt(onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT));
        }

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        for (String resourceId : resourceIdList) {
            if (limStr == null || count < lim) {
                Onem2mResource resource = Onem2mDb.getInstance().getResource(resourceId);
                JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, resource, onem2mResponse);
                if (jContent != null) {
                    ja.put(jContent);
                    if (++count == lim)
                        break;
                }
            }
        }
    }

    /**
     * Format a list of the child references.  A child reference is either the non-h or hierarchical version of the
     * reference to the resourceId.  This, conceivably, could be a lot of references so TODO I think I need a system
     * variable with a MAX_LIMIT.
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static boolean produceJsonResultContentChildResourceRef(RequestPrimitive onem2mRequest,
                                                                  Onem2mResource onem2mResource,
                                                                  ResponsePrimitive onem2mResponse,
                                                                  JSONObject j) {
        String h = null;

        String resourceId = onem2mResource.getResourceId();
        String resourceType = Onem2mDb.getInstance().getResourceType(onem2mResource);
        if (!FilterCriteria.matches(onem2mRequest, onem2mResource, resourceType)) {
            return false;
        }

        if (onem2mResponse.useHierarchicalAddressing()) {
            h = Onem2mDb.getInstance().getHierarchicalNameForResource(resourceId);
        } else {
            h = Onem2mDb.getInstance().getNonHierarchicalNameForResource(resourceId);
        }
        j.put(ResourceContent.MEMBER_URI, h);
        j.put(ResourceContent.MEMBER_NAME,onem2mResource.getName());
        j.put(ResourceContent.MEMBER_TYPE, Integer.valueOf(resourceType));

        return true;
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

        Integer lim = 0;
        Integer count = 0;
        String limStr = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT);
        if (limStr != null) {
            lim = Integer.parseInt(onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT));
        }

        String h = null;
        JSONArray ja = new JSONArray();

        List<Child> childList = onem2mResource.getChild();

        for (Child child : childList) {

            if (limStr == null || count < lim) {
                String resourceId = child.getResourceId();
                Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);
                JSONObject jContent = new JSONObject();
                if (produceJsonResultContentChildResourceRef(onem2mRequest, childResource, onem2mResponse, jContent)) {
                    ja.put(jContent);
                    if (++count == lim)
                        break;
                }
            }

        }
        j.put(ResourceContent.CHILD_RESOURCE_REF, ja);
    }

    /**
     * Start at the root resource and find a hierarchical set of resources then generate the attributes for each of those
     * resources in an "any" array of json objects where each json object is the set of resource specific attrs
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static void discoveryJsonResultContentChildResourceRefs(RequestPrimitive onem2mRequest,
                                                             Onem2mResource onem2mResource,
                                                             ResponsePrimitive onem2mResponse,
                                                             JSONArray ja,
                                                             boolean showRootAttrs) {

        Integer lim = 0;
        Integer count = 0;
        String limStr = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT);
        if (limStr != null) {
            lim = Integer.parseInt(onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT));
        }

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        int resourceListLen = resourceIdList.size();
        // skip the first as its the root, just want the children
        for (int i = 0; i < resourceListLen; i++) {
            if (limStr == null || count < lim) {
                String resourceId = resourceIdList.get(i);
                Onem2mResource resource = Onem2mDb.getInstance().getResource(resourceId);
                if (i == 0) {
                    if (showRootAttrs) {
                        JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, resource, onem2mResponse);

                        if (jContent != null) {
                            ja.put(jContent);
                            if (++count == lim)
                                break;
                        }
                    }
                } else {
                    // child resources
                    JSONObject jContent = new JSONObject();
                    if (produceJsonResultContentChildResourceRef(onem2mRequest, resource, onem2mResponse, jContent)) {
                        ja.put(jContent);
                        if (++count == lim)
                            break;
                    }
                }
            }
        }
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

        Integer lim = 0;
        Integer count = 0;
        String limStr = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT);
        if (limStr != null) {
            lim = Integer.parseInt(onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT));
        }

        String h = null;
        JSONArray ja = new JSONArray();
        List<Child> childList = onem2mResource.getChild();
        for (Child child : childList) {

            if (limStr == null || count < lim) {
                String resourceId = child.getResourceId();

                Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);

                JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, childResource, onem2mResponse);
                if (jContent != null) {
                    ja.put(jContent);
                    if (++count == lim)
                        break;
                }
            }

        }
        j.put(ResourceContent.CHILD_RESOURCE, ja);
    }

    /**
     * Start at the root resource and find a hierarchical set of resources then generate the attributes for each of those
     * resources in an "any" array of json objects where each json object is the set of resource specific attrs
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static void discoveryJsonResultContentAttributesAndChildResources(RequestPrimitive onem2mRequest,
                                                                    Onem2mResource onem2mResource,
                                                                    ResponsePrimitive onem2mResponse,
                                                                    JSONArray ja) {

        Integer lim = 0;
        Integer count = 0;
        String limStr = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT);
        if (limStr != null) {
            lim = Integer.parseInt(onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_LIMIT));
        }

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        int resourceListLen = resourceIdList.size();
        // the first resource is the root, show its attrs too
        for (int i = 0; i < resourceListLen; i++) {
            if (limStr == null || count < lim) {
                String resourceId = resourceIdList.get(i);
                Onem2mResource resource = Onem2mDb.getInstance().getResource(resourceId);
                JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, resource, onem2mResponse);
                if (jContent != null) {
                    ja.put(jContent);
                    if (++count == lim)
                    break;
                }
            }
        }
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
