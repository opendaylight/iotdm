/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest;

import static java.util.Objects.nonNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.BaseResource;
import org.opendaylight.iotdm.onem2m.core.rest.utils.FilterCriteria;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
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

        JSONObject tempJsonObject;
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
        if (protocol != null && protocol.equals(Onem2m.Protocol.HTTP)) {
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

        //allow attributes filtering for result content ATTRIBUTES and filter usage CONDITIONAL_RETRIEVAL only
        if(onem2mRequest.hasContentAttributeList()) {
            if(rc != Onem2m.ResultContent.ATTRIBUTES) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "ATTRIBUTE_LIST(" + RequestPrimitive.ATTRIBUTE_LIST + ") not permitted for given result content " + rc);
                return;
            }

            if(onem2mRequest.getFUDiscovery()) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "ATTRIBUTE_LIST(" + RequestPrimitive.ATTRIBUTE_LIST + ") not permitted for given filter usage " + onem2mRequest.getPrimitiveFilterCriteriaFilterUsage());
                return;
            }
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
                    if (jsonObject != null) {
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
                if (tempJsonObject != null) jsonObject = tempJsonObject;
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
                    if (tempJsonObject != null) jsonObject = tempJsonObject;
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
                    if (tempJsonObject != null) jsonObject = tempJsonObject;
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

        String h = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource);

        JsonUtils.put(j, BaseResource.MEMBER_URI, h);
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

            JSONObject j = new JSONObject();

            String parentId = onem2mResponse.getJsonResourceContent().optString(BaseResource.PARENT_ID, null);
            if (parentId != null) {
                JsonUtils.put(onem2mResponse.getJsonResourceContent(), BaseResource.PARENT_ID,
                        Onem2mDb.getInstance().getNonHierarchicalNameForResource(parentId));
            }

            // hack to rid the result content from "internal" json keys
//            onem2mResponse.getJsonResourceContent().remove("c:" + Onem2m.ResourceType.SUBSCRIPTION);
//            onem2mResponse.getJsonResourceContent().remove("c:" + Onem2m.ResourceType.CONTENT_INSTANCE);

            if (onem2mRequest.isCreate) {
                // if the create contains NAME in the header, or options, do not return name
                if (onem2mRequest.getPrimitiveName() != null) {
                    onem2mResponse.getJsonResourceContent().remove(BaseResource.RESOURCE_NAME);
                }

                // we start out with all the keys in the content and strip some out if they existed in the create call
                String[] keys = onem2mResponse.getBaseResource().getInJsonCreateKeys();
                if (keys != null) {
                    for (String keyToRemove : keys) {
                        onem2mResponse.getJsonResourceContent().remove(keyToRemove);
                    }
                }
            }
            if (onem2mRequest.isUpdate) {
                return JsonUtils.put(j, "m2m:" + Onem2m.resourceTypeToString.get(resourceType),
                        onem2mRequest.getBaseResource().getInJsonContent());
            }

            //filter attributes according to given ATTRIBUTE_LIST - see TS-0009 6.2.2.2
            if (onem2mRequest.getPrimitiveOperation().equals(Onem2m.Operation.RETRIEVE) && onem2mRequest.hasContentAttributeList()) {
                filterAttributesByAttributeList(onem2mRequest, onem2mResponse);
            }

            return JsonUtils.put(j, "m2m:" + Onem2m.resourceTypeToString.get(resourceType),
                    onem2mResponse.getJsonResourceContent());
        }

        return null;
    }

    /**
     * remove all attributes not contained in given attribute list
     * @param onem2mRequest onem2mRequest
     * @param onem2mResponse onem2mResponse
     */
    private static void filterAttributesByAttributeList(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        JSONArray attributesArray = new JSONObject(onem2mRequest.getPrimitiveContent()).optJSONArray(RequestPrimitive.ATTRIBUTE_LIST);
        if (nonNull(attributesArray)) {
            List<String> attributeList = Lists.newArrayList();
            for (int i = 0; i < attributesArray.length(); i++) {
                attributeList.add(attributesArray.getString(i));
            }

            HashSet attributesNameSet = Sets.newHashSet(onem2mResponse.getJsonResourceContent().keySet());
            for (Object attributeName : attributesNameSet) {
                if (!attributeList.contains(attributeName)) {
                    onem2mResponse.getJsonResourceContent().remove(String.valueOf(attributeName));
                }
            }
        }
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

            JSONObject j = new JSONObject();

            String parentId = onem2mResponse.getJsonResourceContent().optString(BaseResource.PARENT_ID, null);
            if (parentId != null) {
                JsonUtils.put(onem2mResponse.getJsonResourceContent(), BaseResource.PARENT_ID,
                        Onem2mDb.getInstance().getNonHierarchicalNameForResource(parentId));
            }
            // copy the existing attrs to the new json object
            for (String key : JSONObject.getNames(onem2mResponse.getJsonResourceContent())) {
                JsonUtils.put(inJsonObject, key, onem2mResponse.getJsonResourceContent().opt(key));
            }

            return JsonUtils.put(j, "m2m:" + Onem2m.resourceTypeToString.get(resourceType), inJsonObject);
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
        Integer count = 0;
        Integer lim = onem2mRequest.getPrimitiveFilterCriteriaLimit();
        if (lim == -1) lim = Integer.MAX_VALUE;

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        for (String resourceId : resourceIdList) {
            if (count < lim) {

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
        Integer resourceType = Integer.valueOf(onem2mResource.getResourceType());

        if (!FilterCriteria.matches(onem2mRequest, onem2mResource, onem2mResponse)) {
            return false;
        }

        if (onem2mResponse.useHierarchicalAddressing()) {
            h = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource);
        } else {
            h = Onem2mDb.getInstance().getNonHierarchicalNameForResource(resourceId);
        }
        JsonUtils.put(j, BaseResource.MEMBER_URI, h);
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
                                                                  JSONObject j) {

        Integer count = 0;
        Integer lim = onem2mRequest.getPrimitiveFilterCriteriaLimit();
        if (lim == -1) lim = Integer.MAX_VALUE;

        String h = null;
        JSONArray ja = new JSONArray();

        List<Onem2mParentChild> childList = Onem2mDb.getInstance().getParentChildList(onem2mResource
                .getResourceId());
        if (childList.isEmpty()) {
            return;
        }

        childList = checkChildList(onem2mRequest, onem2mResource, onem2mResponse, childList);
        if (childList.isEmpty())
            return;

        for (Onem2mParentChild child : childList) {

            if (count < lim) {
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
        JsonUtils.put(j, BaseResource.CHILD_RESOURCE_REF, ja);
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

        Integer count = 0;
        Integer lim = onem2mRequest.getPrimitiveFilterCriteriaLimit();
        if (lim == -1) lim = Integer.MAX_VALUE;

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        int resourceListLen = resourceIdList.size();
        // skip the first as its the root, just want the children
        for (int i = 0; i < resourceListLen; i++) {
            if (count < lim) {
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

        Integer count = 0;
        Integer lim = onem2mRequest.getPrimitiveFilterCriteriaLimit();
        if (lim == -1) lim = Integer.MAX_VALUE;

        String h = null;
        JSONArray ja = new JSONArray();
        List<Onem2mParentChild> childList =
                Onem2mDb.getInstance().getParentChildList(onem2mResource.getResourceId());

        //  todo: Check Subscription, if there is no subscription, return error?
        // todo: if there is subscription type E, then send Notification, then wait 3 seconds, then check again?
        childList = checkChildList(onem2mRequest, onem2mResource, onem2mResponse, childList);
        if (childList.isEmpty())
            return;

        for (Onem2mParentChild child : childList) {

            if (count < lim) {
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
        JsonUtils.put(j, BaseResource.CHILD_RESOURCE, ja);
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
                                                                    JSONArray ja) {

        Integer count = 0;
        Integer lim = onem2mRequest.getPrimitiveFilterCriteriaLimit();
        if (lim == -1) lim = Integer.MAX_VALUE;

        List<String> resourceIdList =
                Onem2mDb.getInstance().getHierarchicalResourceList(onem2mResource.getResourceId(),
                        Onem2m.MAX_DISCOVERY_LIMIT);
        int resourceListLen = resourceIdList.size();
        // the first resource is the root, show its attrs too
        for (int i = 0; i < resourceListLen; i++) {
            if (count < lim) {
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
