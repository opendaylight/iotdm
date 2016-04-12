/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.FilterCriteria;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
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

        JSONObject temp = null;
        onem2mResponse.setUseHierarchicalAddressing(true);
        String drt = onem2mRequest.getPrimitive(RequestPrimitive.DISCOVERY_RESULT_TYPE);
        if (drt != null) {
            if (drt.contentEquals(Onem2m.DiscoveryResultType.NON_HIERARCHICAL)) {
                onem2mResponse.setUseHierarchicalAddressing(false);
            }
        }

        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();
        onem2mResponse.setJsonResourceContent(onem2mRequest.getJsonResourceContent());

        // protocol specific info for all rcn values is done here
        String protocol = onem2mRequest.getPrimitive(RequestPrimitive.PROTOCOL);
        if (protocol != null && protocol.contentEquals(Onem2m.Protocol.HTTP)) {
            if (onem2mRequest.getPrimitive(RequestPrimitive.OPERATION).contentEquals(Onem2m.Operation.CREATE)) {
                onem2mResponse.setPrimitive(ResponsePrimitive.HTTP_CONTENT_LOCATION,
                        Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource.getResourceId()));
            }
            onem2mResponse.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE,
                    Onem2m.ContentType.APP_VND_RES_JSON + ";" + RequestPrimitive.RESOURCE_TYPE + "=" +
                            onem2mResource.getResourceType());
        }

        JSONObject jo = new JSONObject(); // for non-fu discovery
        JSONArray ja = new JSONArray();   // for fu discovery

        // cache the resourceContent so that create keys can be accessed and filtered
        onem2mResponse.setResourceContent(onem2mRequest.getResourceContent());

        String rc = onem2mRequest.getPrimitive(RequestPrimitive.RESULT_CONTENT);
        if (rc == null) {
            rc = Onem2m.ResultContent.ATTRIBUTES;
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
                // todo: update method here
                produceJsonResultContentHierarchicalAddress(onem2mRequest, onem2mResource, onem2mResponse, jo);
                onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                break;

            case Onem2m.ResultContent.HIERARCHICAL_ADDRESS_ATTRIBUTES:
                onem2mResponse.setUseHierarchicalAddressing(true);
                // add the address
                produceJsonResultContentHierarchicalAddress(onem2mRequest, onem2mResource, onem2mResponse, jo);
                 temp = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jo);
                if (temp != null) jo = temp;
                onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                break;

            case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCES:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentAttributesAndChildResources(onem2mRequest, onem2mResource, onem2mResponse, ja);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, ja.toString());
                } else {
                    produceJsonResultContentChildResources(onem2mRequest, onem2mResource, onem2mResponse, jo);
                    onem2mResponse.setJsonResourceContent(onem2mRequest.getJsonResourceContent());
                    temp = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jo);
                    if (temp != null) jo = temp;
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, jo.toString());
                }
                break;

            case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, ja, true);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, ja.toString());
                } else {
                    produceJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jo);
                    onem2mResponse.setJsonResourceContent(onem2mRequest.getJsonResourceContent());
                    temp = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jo);
                    if (temp != null) jo = temp;
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

        JsonUtils.put(j, ResourceContent.MEMBER_URI, h);
    }

    /**
     * Fill in the attributes of this resource.  Apply any filter criteria, and use the appropriate addressing.
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static JSONObject produceJsonResultContentAttributes(RequestPrimitive onem2mRequest,
                                                           Onem2mResource onem2mResource,
                                                           ResponsePrimitive onem2mResponse) {


        String resourceType = onem2mResource.getResourceType();

        if (FilterCriteria.matches(onem2mRequest, onem2mResource, onem2mResponse)) {

            JSONObject j = new JSONObject();

            String m2mPrefixString = onem2mResponse.useM2MPrefix() ? "m2m:" : "";

            String parentId = onem2mResponse.getJsonResourceContent().optString(ResourceContent.PARENT_ID, null);
            if (parentId != null) {
                JsonUtils.put(onem2mResponse.getJsonResourceContent(), ResourceContent.PARENT_ID,
                        Onem2mDb.getInstance().getNonHierarchicalNameForResource(parentId));
            }

            if (onem2mRequest.isCreate) {
                // if the create contains NAME in the header, or options, do not return name
                if (onem2mRequest.getPrimitive(RequestPrimitive.NAME) != null) {
                    onem2mResponse.getJsonResourceContent().remove(ResourceContent.RESOURCE_NAME);
                }
                // we start out with all the keys in the content and strip some out if they existed in the create call
                for (String keyToRemove : onem2mResponse.getResourceContent().getInJsonCreateKeys()) {
                    onem2mResponse.getJsonResourceContent().remove(keyToRemove);
                }
            }
            if (onem2mRequest.isUpdate) {
                return JsonUtils.put(j, m2mPrefixString + Onem2m.resourceTypeToString.get(resourceType),
                        onem2mRequest.getResourceContent().getInJsonContent());
            }

            return JsonUtils.put(j, m2mPrefixString + Onem2m.resourceTypeToString.get(resourceType),
                    onem2mResponse.getJsonResourceContent());
        }

        return null;
    }

    /**
     * another choice for produce json result. For putting ch into the resource.
     * @param onem2mRequest
     * @param onem2mResource
     * @param onem2mResponse
     * @param inJsonObject
     * @return
     */
    private static JSONObject produceJsonResultContentAttributes(RequestPrimitive onem2mRequest,
                                                                 Onem2mResource onem2mResource,
                                                                 ResponsePrimitive onem2mResponse,
                                                                 JSONObject inJsonObject) {

        String resourceType = onem2mResource.getResourceType();

        if (FilterCriteria.matches(onem2mRequest, onem2mResource, onem2mResponse)) {

            JSONObject j = new JSONObject();
            String m2mPrefixString = onem2mResponse.useM2MPrefix() ? "m2m:" : "";

            String parentId = onem2mResponse.getJsonResourceContent().optString(ResourceContent.PARENT_ID, null);
            if (parentId != null) {
                JsonUtils.put(onem2mResponse.getJsonResourceContent(), ResourceContent.PARENT_ID,
                        Onem2mDb.getInstance().getNonHierarchicalNameForResource(parentId));
            }
            // copy the existing attrs to the new json object
            for (String key : JSONObject.getNames(onem2mResponse.getJsonResourceContent())) {
                JsonUtils.put(inJsonObject, key, onem2mResponse.getJsonResourceContent().opt(key));
            }
            return JsonUtils.put(j, m2mPrefixString + Onem2m.resourceTypeToString.get(resourceType), inJsonObject);
        }

        return null;
        //return inJsonObject;
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
                onem2mResponse.setJsonResourceContent(resource.getResourceContentJsonString());

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
        String resourceType = onem2mResource.getResourceType();
        if (!FilterCriteria.matches(onem2mRequest, onem2mResource, onem2mResponse)) {
            return false;
        }

        if (onem2mResponse.useHierarchicalAddressing()) {
            h = Onem2mDb.getInstance().getHierarchicalNameForResource(resourceId);
        } else {
            h = Onem2mDb.getInstance().getNonHierarchicalNameForResource(resourceId);
        }
        JsonUtils.put(j, ResourceContent.MEMBER_URI, h);
        JsonUtils.put(j, ResourceContent.MEMBER_NAME,onem2mResource.getName());
        JsonUtils.put(j, ResourceContent.MEMBER_TYPE, Integer.valueOf(resourceType));

        return true;
    }

    /**
     * Format a list of the child references.  A child reference is either the non-h or hierarchical version of the
     * reference to the resourceId.  This conceivable could be a lot of references so ...
     * TODO I think I need a system variable with a MAX_LIMIT.
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

        childList = checkChildList(onem2mRequest, onem2mResource, onem2mResponse, childList);
        if (childList.isEmpty())
            return;

        for (Child child : childList) {

            if (limStr == null || count < lim) {
                String resourceId = child.getResourceId();
                Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);
                onem2mResponse.setJsonResourceContent(childResource.getResourceContentJsonString());
                JSONObject jContent = new JSONObject();
                if (produceJsonResultContentChildResourceRef(onem2mRequest, childResource, onem2mResponse, jContent)) {
                    ja.put(jContent);
                    if (++count == lim)
                        break;
                }
            }

        }
        JsonUtils.put(j, ResourceContent.CHILD_RESOURCE_REF, ja);
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
                onem2mResponse.setJsonResourceContent(resource.getResourceContentJsonString());
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
     * Generate the "ch" attribute for the response payload, if ch = null, add Subscription Check.
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

        //  todo: Check Subscription, if there is no subscription, return error?
        // todo: if there is subscription type E, then send Notification, then wait 3 seconds, then check again?
        childList = checkChildList(onem2mRequest, onem2mResource, onem2mResponse, childList);
        if (childList.isEmpty())
            return;

        for (Child child : childList) {

            if (limStr == null || count < lim) {
                String resourceId = child.getResourceId();

                Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);
                onem2mResponse.setJsonResourceContent(childResource.getResourceContentJsonString());
                JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, childResource, onem2mResponse);
                if (jContent != null) {
                    ja.put(jContent);
                    if (++count == lim)
                        break;
                }
            }

        }
        JsonUtils.put(j, ResourceContent.CHILD_RESOURCE, ja);
    }

    private static List<Child> checkChildList(RequestPrimitive onem2mRequest, Onem2mResource onem2mResource, ResponsePrimitive onem2mResponse, List<Child> childList) {
        if (!childList.isEmpty()) {
            // if there are several children, need to check whether they are expired

            Iterator<Child> iterator = childList.iterator();
            while (iterator.hasNext()) {
                Child child = iterator.next();
                String resourceId = child.getResourceId();
                Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);
                if (!Onem2mDb.getInstance().isAlive(childResource)) {
                    iterator.remove();
                }
            }
        }
        if (childList.isEmpty()) {
            List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findSelfSubscriptionID(onem2mResource.getResourceId(), Onem2m.EventType.RETRIEVE_NECHILD);
            if (!subscriptionResourceIdList.isEmpty()) {
                NotificationProcessor.handleEventTypeE(onem2mRequest, subscriptionResourceIdList);
                //todo: do we have another thread to create resources?
                // todo: what is the correct method to wait?
                try {
                    Thread.currentThread().wait(2000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                onem2mResource = Onem2mDb.getInstance().getResource(onem2mResource.getResourceId());
                childList = onem2mResource.getChild();
                if (childList.isEmpty()) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") invalid option: empty child");
                }
            }
            else{
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") invalid option: empty child");
            }
        }
        return childList;

    }

    /**
     * Start at the root resource and find a hierarchical set of resources then generate the attributes for each of those
     * resources in an "any" array of json objects where each json object is the set of resource specific attrs
     * default drt, Hierarchical addressing method.
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
                onem2mResponse.setJsonResourceContent(resource.getResourceContentJsonString());
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
