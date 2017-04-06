/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import static java.util.Objects.nonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.BaseResource;
import org.opendaylight.iotdm.onem2m.core.rest.utils.FilterCriteria;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mRequestPrimitiveInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultContentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ResultContentProcessor.class);

    private ResultContentProcessor() {}

    /**
     * This routine uses the result content, and filter criteria to gather information to return in the
     * ResponsePrimitive onem2mResponse.
     * @param onem2mRequest  request
     * @param onem2mResponse response
     */
    public static void handleRetrieve(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        produceJsonResultContent(onem2mRequest, onem2mResponse);
    }

    /**
     * This routine uses the result content, and filter criteria to gather information to return in the
     * ResponsePrimitive onem2mResponse.  See TS0001 Section 8.1.2 Request .. ResultContent
     * @param onem2mRequest  the set of request primitives
     * @param onem2mResponse the set of result primitives
     */
    private static void produceJsonResultContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        JSONObject tempJsonObject = null;
        onem2mResponse.setUseHierarchicalAddressing(true);
        Integer drt = onem2mRequest.getPrimitiveDiscoveryResultType();
        if (drt != -1) {
            if (drt == Onem2m.DiscoveryResultType.NON_HIERARCHICAL) {
                onem2mResponse.setUseHierarchicalAddressing(false);
            }
        }

        Onem2mResource onem2mResource = onem2mRequest.getOnem2mResource();
        onem2mResponse.setJsonResourceContent(onem2mRequest.getJsonResourceContent());

        // protocol specific info for all rcn values is done here
        String protocol = onem2mRequest.getPrimitiveProtocol();
        if (nonNull(protocol) && protocol.equals(Onem2m.Protocol.HTTP)) {
            onem2mResponse.setPrimitiveHttpContentType(
                    Onem2m.ContentType.APP_VND_RES_JSON + ";" + RequestPrimitive.RESOURCE_TYPE + "=" +
                            onem2mResource.getResourceType());
        }

        if (onem2mRequest.isCreate) {
            onem2mResponse.setPrimitiveContentLocation(
                    Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource));
        }

        onem2mResponse.setPrimitiveContentFormat(Onem2m.ContentFormat.JSON);

        JSONObject jsonObject = new JSONObject(); // for non-fu discovery
        JSONArray jsonArray = new JSONArray();   // for fu discovery

        // cache the resourceContent so that create keys can be accessed and filtered
        onem2mResponse.setBaseResource(onem2mRequest.getBaseResource());

        Integer rc = onem2mRequest.getPrimitiveResultContent();
        if (rc == -1) {
            rc = Onem2m.ResultContent.ATTRIBUTES;
        }
        switch (rc) {
            case Onem2m.ResultContent.NOTHING:
                onem2mResponse.setPrimitiveContent(jsonObject.toString());
                return; // that was easy

            case Onem2m.ResultContent.ATTRIBUTES:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jsonArray);
                    onem2mResponse.setPrimitiveContent(jsonArray.toString());
                } else {
                    jsonObject = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse);
                    if (nonNull(jsonObject)) {
                        onem2mResponse.setPrimitiveContent(jsonObject.toString());
                    } else {
                        onem2mResponse.setPrimitiveContent("{}");
                    }
                }
                break;

            case Onem2m.ResultContent.HIERARCHICAL_ADDRESS:
                // todo: update method here
                produceJsonResultContentHierarchicalAddress(onem2mRequest, onem2mResource, onem2mResponse, jsonObject);
                onem2mResponse.setPrimitiveContent(jsonObject.toString());
                break;

            case Onem2m.ResultContent.HIERARCHICAL_ADDRESS_ATTRIBUTES:
                onem2mResponse.setUseHierarchicalAddressing(true);
                // add the address
                produceJsonResultContentHierarchicalAddress(onem2mRequest, onem2mResource, onem2mResponse, jsonObject);
                tempJsonObject = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jsonObject);
                if (nonNull(tempJsonObject)) jsonObject = tempJsonObject;
                onem2mResponse.setPrimitiveContent(jsonObject.toString());
                break;

            case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCES:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentAttributesAndChildResources(onem2mRequest, onem2mResource, onem2mResponse, jsonArray);
                    onem2mResponse.setPrimitiveContent(jsonArray.toString());
                } else {
                    produceJsonResultContentChildResources(onem2mRequest, onem2mResource, onem2mResponse, jsonObject);
                    onem2mResponse.setJsonResourceContent(onem2mRequest.getJsonResourceContent());
                    tempJsonObject = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jsonObject);
                    if (nonNull(tempJsonObject)) jsonObject = tempJsonObject;
                    onem2mResponse.setPrimitiveContent(jsonObject.toString());
                }
                break;

            case Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jsonArray, true);
                    onem2mResponse.setPrimitiveContent(jsonArray.toString());
                } else {
                    produceJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jsonObject);
                    onem2mResponse.setJsonResourceContent(onem2mRequest.getJsonResourceContent());
                    tempJsonObject = produceJsonResultContentAttributes(onem2mRequest, onem2mResource, onem2mResponse, jsonObject);
                    if (nonNull(tempJsonObject)) jsonObject = tempJsonObject;
                    onem2mResponse.setPrimitiveContent(jsonObject.toString());
                }
                break;

            case Onem2m.ResultContent.CHILD_RESOURCE_REFS:
                if (onem2mRequest.getFUDiscovery()) {
                    discoveryJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jsonArray, false);
                    onem2mResponse.setPrimitiveContent(jsonArray.toString());
                } else {
                    produceJsonResultContentChildResourceRefs(onem2mRequest, onem2mResource, onem2mResponse, jsonObject);
                    onem2mResponse.setPrimitiveContent(jsonObject.toString());
                }
                break;

            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "RESULT_CONTENT(" + RequestPrimitive.RESULT_CONTENT + ") invalid option: (" + rc + ")");
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

        JsonUtils.put(j, BaseResource.MEMBER_URI, Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource));
    }

    /**
     * Fill in the attributes of this resource.  Apply any filter criteria, and use the appropriate addressing.
     * @param onem2mResource
     * @param onem2mResponse
     */
    private static JSONObject produceJsonResultContentAttributes(RequestPrimitive onem2mRequest,
                                                                 Onem2mResource onem2mResource,
                                                                 ResponsePrimitive onem2mResponse) {


        Integer resourceType = Integer.valueOf(onem2mResource.getResourceType());

        if (FilterCriteria.matches(onem2mRequest, onem2mResource, onem2mResponse)) {

            JSONObject jsonObject = new JSONObject();

            String parentId = onem2mResponse.getJsonResourceContent().optString(BaseResource.PARENT_ID, null);
            if (nonNull(parentId)) {
                JsonUtils.put(onem2mResponse.getJsonResourceContent(), BaseResource.PARENT_ID,
                        Onem2mDb.getInstance().getNonHierarchicalNameForResource(parentId));
            }

            // hack to rid the result content from "internal" json keys
//            onem2mResponse.getJsonResourceContent().remove("c:" + Onem2m.ResourceType.SUBSCRIPTION);
//            onem2mResponse.getJsonResourceContent().remove("c:" + Onem2m.ResourceType.CONTENT_INSTANCE);

            if (onem2mRequest.isCreate) {
                // if the create contains NAME in the header, or options, do not return name
                if (nonNull(onem2mRequest.getPrimitiveName())) {
                    onem2mResponse.getJsonResourceContent().remove(BaseResource.RESOURCE_NAME);
                }

                // we start out with all the keys in the content and strip some out if they existed in the create call
                String[] keys = onem2mResponse.getBaseResource().getInJsonCreateKeys();
                if (nonNull(keys)) {
                    for (String keyToRemove : keys) {
                        onem2mResponse.getJsonResourceContent().remove(keyToRemove);
                    }
                }
            }
            if (onem2mRequest.isUpdate) {
                return JsonUtils.put(jsonObject, "m2m:" + Onem2m.resourceTypeToString.get(resourceType),
                        onem2mRequest.getBaseResource().getInJsonContent());
            }

            return JsonUtils.put(jsonObject, "m2m:" + Onem2m.resourceTypeToString.get(resourceType),
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

        Integer resourceType = Integer.valueOf(onem2mResource.getResourceType());

        if (FilterCriteria.matches(onem2mRequest, onem2mResource, onem2mResponse)) {

            JSONObject jsonObject = new JSONObject();

            String parentId = onem2mResponse.getJsonResourceContent().optString(BaseResource.PARENT_ID, null);
            if (nonNull(parentId)) {
                JsonUtils.put(onem2mResponse.getJsonResourceContent(), BaseResource.PARENT_ID,
                        Onem2mDb.getInstance().getNonHierarchicalNameForResource(parentId));
            }
            // copy the existing attrs to the new json object
            for (String key : JSONObject.getNames(onem2mResponse.getJsonResourceContent())) {
                JsonUtils.put(inJsonObject, key, onem2mResponse.getJsonResourceContent().opt(key));
            }

            return JsonUtils.put(jsonObject, "m2m:" + Onem2m.resourceTypeToString.get(resourceType), inJsonObject);
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
                                                             JSONArray jsonArray) {
        Integer addedCount = 0;
        Integer skippedCount = 0;
        Integer limit = limitFromRequest(onem2mRequest);
        Integer offset = offsetFromRequest(onem2mRequest);

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        for (String resourceId : resourceIdList) {
            if (addedCount < limit) {

                Onem2mResource resource = Onem2mDb.getInstance().getResource(resourceId);
                onem2mResponse.setJsonResourceContent(resource.getResourceContentJsonString());

                JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, resource, onem2mResponse);
                if (nonNull(jContent)) {
                    if(skipAndIncrementOrAdd(jsonArray, jContent, skippedCount, offset))
                        addedCount++;
                    else
                        skippedCount++;

                    if (Objects.equals(addedCount, limit))
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
        String hierarchicalResourceName;

        String resourceId = onem2mResource.getResourceId();
        Integer resourceType = Integer.valueOf(onem2mResource.getResourceType());

        if (!FilterCriteria.matches(onem2mRequest, onem2mResource, onem2mResponse)) {
            return false;
        }

        if (onem2mResponse.useHierarchicalAddressing()) {
            hierarchicalResourceName = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource);
        } else {
            hierarchicalResourceName = Onem2mDb.getInstance().getNonHierarchicalNameForResource(resourceId);
        }
        JsonUtils.put(j, BaseResource.MEMBER_URI, hierarchicalResourceName);
        JsonUtils.put(j, BaseResource.MEMBER_NAME, onem2mResource.getName());
        JsonUtils.put(j, BaseResource.MEMBER_TYPE, resourceType);

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
                                                                  JSONObject jsonObject) {

        Integer limit = limitFromRequest(onem2mRequest);
        Integer offset = offsetFromRequest(onem2mRequest);

        JSONArray resultJsonArray = new JSONArray();

        List<Onem2mParentChild> childList = Onem2mDb.getInstance().getParentChildListLimitN(onem2mResource
                .getResourceId(), limit, offset);
        if (childList.isEmpty()) {
            return;
        }

        childList = checkChildList(onem2mRequest, onem2mResource, onem2mResponse, childList);
        if (childList.isEmpty())
            return;

        for (Onem2mParentChild child : childList) {

                String resourceId = child.getResourceId();
                Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);
                onem2mResponse.setJsonResourceContent(childResource.getResourceContentJsonString());
                JSONObject jContent = new JSONObject();
                if (produceJsonResultContentChildResourceRef(onem2mRequest, childResource, onem2mResponse, jContent)) {
                    resultJsonArray.put(jContent);
                }

        }
        JsonUtils.put(jsonObject, BaseResource.CHILD_RESOURCE_REF, resultJsonArray);
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
                                                             JSONArray jsonArray,
                                                             boolean showRootAttrs) {

        Integer addedCount = 0;
        Integer skippedCount = 0;
        Integer limit = limitFromRequest(onem2mRequest);
        Integer offset = offsetFromRequest(onem2mRequest);

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        int resourceListLen = resourceIdList.size();
        for (int i = 0; i < resourceListLen; i++) {
            if (addedCount < limit) {
                String resourceId = resourceIdList.get(i);
                Onem2mResource resource = Onem2mDb.getInstance().getResource(resourceId);
                onem2mResponse.setJsonResourceContent(resource.getResourceContentJsonString());
                if (i == 0) {
                    if (showRootAttrs) {
                        JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, resource, onem2mResponse);

                        if (nonNull(jContent)) {
                            if(skipAndIncrementOrAdd(jsonArray, jContent, skippedCount, offset))
                                addedCount++;
                            else
                                skippedCount++;

                            if (Objects.equals(addedCount, limit))
                                break;
                        }
                    }
                } else {
                    // child resources
                    JSONObject jContent = new JSONObject();
                    if (produceJsonResultContentChildResourceRef(onem2mRequest, resource, onem2mResponse, jContent)) {
                        if(skipAndIncrementOrAdd(jsonArray, jContent, skippedCount, offset))
                            addedCount++;
                        else
                            skippedCount++;

                        if (Objects.equals(addedCount, limit))
                            break;
                    }
                }
            }
        }
    }

    private static Integer limitFromRequest(RequestPrimitive onem2mRequest) {
        Integer limit = onem2mRequest.getPrimitiveFilterCriteriaLimit();
        if (limit == -1)
            return Integer.MAX_VALUE;
        else
            return limit;
    }

    private static Integer offsetFromRequest(RequestPrimitive onem2mRequest) {
        Integer offset = onem2mRequest.getPrimitiveFilterCriteriaOffset();
        if (offset == -1)
            return 0;
        else
            return offset;
    }

    /**
     * @return true if added false otherwise
     */
    private static boolean skipAndIncrementOrAdd(JSONArray jsonArray, JSONObject jContent, Integer skippedCount, Integer offset) {
        if(skippedCount >= offset) {
            jsonArray.put(jContent);
            return true;
        }
        else {
            return false;
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
                                                               JSONObject jsonObject) {

        Integer limit = limitFromRequest(onem2mRequest);
        Integer offset = offsetFromRequest(onem2mRequest);

        JSONArray jsonArray = new JSONArray();
        List<Onem2mParentChild> childList =
                Onem2mDb.getInstance().getParentChildListLimitN(onem2mResource.getResourceId(), limit, offset);

        //  todo: Check Subscription, if there is no subscription, return error?
        // todo: if there is subscription type E, then send Notification, then wait 3 seconds, then check again?
        childList = checkChildList(onem2mRequest, onem2mResource, onem2mResponse, childList);
        if (childList.isEmpty())
            return;

        for (Onem2mParentChild child : childList) {
            Onem2mResource childResource = Onem2mDb.getInstance().getResource(child.getResourceId());
            onem2mResponse.setJsonResourceContent(childResource.getResourceContentJsonString());
            JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, childResource, onem2mResponse);
            if (nonNull(jContent)) {
                jsonArray.put(jContent);
            }

        }
        JsonUtils.put(jsonObject, BaseResource.CHILD_RESOURCE, jsonArray);
    }

    private static List<Onem2mParentChild> checkChildList(RequestPrimitive onem2mRequest,
                                                          Onem2mResource onem2mResource,
                                                          ResponsePrimitive onem2mResponse,
                                                          List<Onem2mParentChild> childList) {
        if (!childList.isEmpty()) {
            // if there are several children, need to check whether they are expired

            Iterator<Onem2mParentChild> iterator = childList.iterator();
            while (iterator.hasNext()) {
                Onem2mParentChild child = iterator.next();
                String resourceId = child.getResourceId();
                Onem2mResource childResource = Onem2mDb.getInstance().getResource(resourceId);
                if (!Onem2mDb.getInstance().isAlive(childResource)) {
                    iterator.remove();
                }
            }
        }
        if (childList.isEmpty()) {
            List<String> subscriptionResourceIdList = Onem2mDb.getInstance().findSelfSubscriptionID(onem2mRequest, Onem2m.EventType.RETRIEVE_NECHILD);
            if (!subscriptionResourceIdList.isEmpty()) {
                NotificationProcessor.getInstance().handleEventTypeE(onem2mRequest, subscriptionResourceIdList);
                //todo: do we have another thread to create resources?
                // todo: what is the correct method to wait?
                try {
                    Thread.currentThread().wait(2000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

                childList = Onem2mDb.getInstance().getParentChildList(onem2mResource.getResourceId());
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
                                                                    JSONArray jsonArray) {

        Integer addedCount = 0;
        Integer skippedCount = 0;
        Integer limit = limitFromRequest(onem2mRequest);
        Integer offset = offsetFromRequest(onem2mRequest);

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        // the first resource is the root, show its attrs too
        for (String aResourceIdList : resourceIdList) {
            if (addedCount < limit) {
                Onem2mResource resource = Onem2mDb.getInstance()
                                                  .getResource(aResourceIdList);
                onem2mResponse.setJsonResourceContent(resource.getResourceContentJsonString());
                JSONObject jContent = produceJsonResultContentAttributes(onem2mRequest, resource, onem2mResponse);
                if (nonNull(jContent)) {
                    if(skipAndIncrementOrAdd(jsonArray, jContent, skippedCount, offset))
                        addedCount++;
                    else
                        skippedCount++;

                    if (Objects.equals(addedCount, limit))
                        break;
                }
            }
        }
    }

    /**
     * The results of the create now must be put in the response.  The result content is used to decide how the
     * results should be formatted.
     * @param onem2mRequest  request
     * @param onem2mResponse response
     */
    public static void handleCreate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        produceJsonResultContent(onem2mRequest, onem2mResponse);
    }

    /**
     * The results of the create now must be put in the response.  The result content is used to decide how the
     * results should be formatted.
     * @param onem2mRequest  request
     * @param onem2mResponse response
     */
    public static void handleUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        produceJsonResultContent(onem2mRequest, onem2mResponse);
    }

    /**
     * The results of the delete now must be put in the response.  The result content is used to decide how the
     * results should be formatted.
     * @param onem2mRequest  request
     * @param onem2mResponse response
     */
    public static void handleDelete(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        produceJsonResultContent(onem2mRequest, onem2mResponse);
    }
}
