/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.database.DbAttrSet;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.MemberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.MemberKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the resource content that was supplied in the RequestPrimitive.CONTENT parameter.  It is
 * formatted according to the CONTENT_TYPE.   It is parsed and the parameter are put in the DbAttr list.  Resource
 * specific methods are called based on the resourceType that is being created.
 */
public class ResourceContent {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContent.class);

    public static final String RESOURCE_TYPE = "ty";
    public static final String RESOURCE_ID = "ri";
    public static final String RESOURCE_NAME = "rn";
    public static final String PARENT_ID = "pi";
    public static final String CREATION_TIME = "ct";
    public static final String EXPIRATION_TIME = "et";
    public static final String LAST_MODIFIED_TIME = "lt";
    public static final String LABELS = "lbl";
    public static final String STATE_TAG = "st";
    public static final String CHILD_RESOURCE = "ch";
    public static final String CHILD_RESOURCE_REF = "ch";
    public static final String MEMBER_URI = "val";
    public static final String MEMBER_NAME = "nm";
    public static final String MEMBER_TYPE = "typ";

    private DbAttr dbAttrs;
    private DbAttrSet dbAttrSets;
    private JSONObject jsonContent;
    private String xmlContent;

    public ResourceContent() {
        dbAttrs = new DbAttr();
        dbAttrSets = new DbAttrSet();
        jsonContent = null;
        xmlContent = null;
    }

    public boolean isJson() { return jsonContent != null; }
    public JSONObject getJsonContent() { return jsonContent; }
    public boolean isXml() { return xmlContent != null; }

    /**
     * Pulls the json/xml formatted data out of the RequestPrimitive.CONTENT string
     * and put it into the request.  It calls an abstract method so that each resource pulls out the data
     * specific to that resource.
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public void parse(String resourceType, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        String cf = onem2mRequest.getPrimitive(RequestPrimitive.CONTENT_FORMAT);
        switch (cf) {
            case Onem2m.ContentFormat.JSON:
                jsonContent = parseJson(resourceType, onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ContentFormat.XML:
                xmlContent = parseXml(resourceType, onem2mRequest, onem2mResponse);
                break;
        }
    }

    /**
     * Parse the JSON content, put it into the set of RequestPrimitive attrs by calling the extractJsonRequestContent
     * which is implemented by each of the resource specific classes as it is an abstract method.
     * @param onem2mRequest request
     * @param onem2mResponse response
     * @return the json obj
     */
    private JSONObject parseJson(String resourceType,
                                 RequestPrimitive onem2mRequest,
                                 ResponsePrimitive onem2mResponse) {

        jsonContent = null;
        String jsonContentString = onem2mRequest.getPrimitive(RequestPrimitive.CONTENT);
        if (jsonContentString == null) {
            // TS0004: 7.2.3.2
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") not specified");
            return null;
        }
        try {
            jsonContent = new JSONObject(jsonContentString.trim());
        } catch (JSONException e) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") parser error (" + e + ")");
            return null;
        }

        /**
         * Each resource in the CONTENT has a JSON key of "ae" or "m2m:ae" if the reource is an AE.  This
         * routine plucks off the "resourceType" and returns its contents
         */
        return proceessResourceSpecificContent(resourceType, jsonContent, onem2mResponse);
    }

    private JSONObject proceessResourceSpecificContent(String resourceType,
                                                       JSONObject jsonContent,
                                                       ResponsePrimitive onem2mResponse) {

        JSONObject resourceJsonObject = null;

        Iterator<?> keys = jsonContent.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            Object o = jsonContent.get(key);

            if (!(o instanceof JSONObject)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") JSON object expected for json key: " + key);
                return null;
            }
            if (resourceJsonObject != null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") too many json keys");
                return null;
            }

            switch (key) {
                case Onem2m.ResourceTypeString.AE:
                case "m2m:" + Onem2m.ResourceTypeString.AE:
                case Onem2m.ResourceTypeString.CONTAINER:
                case "m2m:" + Onem2m.ResourceTypeString.CONTAINER:
                case Onem2m.ResourceTypeString.CONTENT_INSTANCE:
                case "m2m:" + Onem2m.ResourceTypeString.CONTENT_INSTANCE:
                case Onem2m.ResourceTypeString.SUBSCRIPTION:
                case "m2m:" + Onem2m.ResourceTypeString.SUBSCRIPTION:
                case Onem2m.ResourceTypeString.CSE_BASE:
                case "m2m:" + Onem2m.ResourceTypeString.CSE_BASE:

                    if (resourceType.contentEquals(key)) {
                        resourceJsonObject = (JSONObject) o;
                        onem2mResponse.setUseM2MPrefix(false);
                    } else if (key.contentEquals("m2m:"+ resourceType)) {
                        resourceJsonObject = (JSONObject) o;
                        onem2mResponse.setUseM2MPrefix(true);
                    } else {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") resource type mismatch");
                        return null;
                    }
                    break;

                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return null;
            }
        }
        return resourceJsonObject;
    }

    /**
     * Ensure the create/updates follow the rules
     * @param onem2mRequest request
     * @param onem2mResponse response
     */
    public void processCommonCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        if (onem2mRequest.isCreate) {
            this.setDbAttr(ResourceContent.RESOURCE_TYPE,
                    onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE));
        } else {
            this.setDbAttr(ResourceContent.RESOURCE_TYPE,
                    onem2mRequest.getDbAttrs().getAttr(ResourceContent.RESOURCE_TYPE));
        }

        // resourceId, resourceName, parentId is filled in by the Onem2mDb.createResource method

        String currDateTime = Onem2mDateTime.getCurrDateTime();

        String ct = this.getDbAttr(ResourceContent.CREATION_TIME);
        if (ct != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CREATION_TIME: read-only parameter");
            return;
        }

        if (onem2mRequest.isCreate) {
            this.setDbAttr(ResourceContent.CREATION_TIME, currDateTime);
        }

        // always update lmt at create or update
        String lmt = this.getDbAttr(ResourceContent.LAST_MODIFIED_TIME);
        if (lmt != null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "LAST_MODIFIED_TIME: read-only parameter");
            return;
        }
        this.setDbAttr(ResourceContent.LAST_MODIFIED_TIME, currDateTime);

        // validate expiration time
        String et = this.getDbAttr(ResourceContent.EXPIRATION_TIME);
        if (et != null) {
            /* for now avoid checking format of et, when we find out actual time format
            if (Onem2mDateTime.dateCompare(et, currDateTime) < 0) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "EXPIRATION_TIME: cannot be less than current time");
                return;
            }
            */
        }
    }

    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     * @param key json content key
     * @param resourceContent fill in with parsed json values
     * @param onem2mResponse response
     * @return valid content
     */
    public static boolean processJsonCommonCreateUpdateContent(String key,
                                                           ResourceContent resourceContent,
                                                           ResponsePrimitive onem2mResponse) {

        Object o = resourceContent.getJsonContent().get(key);

        switch (key) {

            case LABELS:
                if (!resourceContent.getJsonContent().isNull(key)) {
                    if (!(o instanceof JSONArray)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                        return false;
                    }
                    JSONArray array = (JSONArray) o;
                    List<Member> memberList = new ArrayList<Member>(array.length());
                    for (int i = 0; i < array.length(); i++) {
                        if (!(array.get(i) instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json array: " + key);
                            return false;
                        }
                        String memberName = array.get(i).toString();
                        memberList.add(new MemberBuilder()
                                .setKey(new MemberKey(memberName))
                                .setMember(memberName)
                                .build());
                    }
                    resourceContent.setDbAttrSet(key, memberList);

                } else {
                    resourceContent.setDbAttrSet(key, null);
                }
                break;

            case EXPIRATION_TIME:
                if (!resourceContent.getJsonContent().isNull(key)) {
                    if (!(o instanceof String) /*|| !Onem2mDateTime.isValidDateTime(o.toString())*/) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") DATE (YYYYMMDDTHHMMSSZ) string expected for expiration time: " + key);
                        return false;
                    }
                    resourceContent.setDbAttr(key, o.toString());

                } else {
                    resourceContent.setDbAttr(key, null);
                }
                break;

            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                return false;
        }
        return true;
    }

    /**
     * Generate JSON for this attr
     * @param attr this attr
     * @param j the obj
     */
    public static void produceJsonForCommonAttributes(Attr attr, JSONObject j) {

        switch (attr.getName()) {
            case ResourceContent.CREATION_TIME:
            case ResourceContent.LAST_MODIFIED_TIME:
            case ResourceContent.EXPIRATION_TIME:
                j.put(attr.getName(), attr.getValue());
                break;
            case ResourceContent.RESOURCE_TYPE:
                j.put(attr.getName(), Integer.valueOf(attr.getValue()));
                break;
            case ResourceContent.STATE_TAG:
                j.put(attr.getName(), Integer.valueOf(attr.getValue()));
                break;
        }
    }

    /**
     * Generate JSON for this attr for Create Only
     * @param attr this attr
     * @param j the obj
     */
    public static void produceJsonForCommonAttributesCreate(Attr attr, JSONObject j) {

        switch (attr.getName()) {
            case ResourceContent.CREATION_TIME:
            case ResourceContent.LAST_MODIFIED_TIME:
            case ResourceContent.EXPIRATION_TIME:
                j.put(attr.getName(), attr.getValue());
                break;
//            case ResourceContent.RESOURCE_TYPE:
//                j.put(attr.getName(), Integer.valueOf(attr.getValue()));
//                break;
            case ResourceContent.STATE_TAG:
                j.put(attr.getName(), Integer.valueOf(attr.getValue()));
                break;
        }
    }
    /**
     * Generate JSON for this attribute set
     * @param attrSet attr set
     * @param j json obj
     */
    public static void produceJsonForCommonAttributeSets(AttrSet attrSet, JSONObject j) {

        switch (attrSet.getName()) {
            case ResourceContent.LABELS:
                JSONArray a = new JSONArray();
                for (Member member : attrSet.getMember()) {
                    a.put(member.getMember());
                }
                j.put(attrSet.getName(), a);
                break;
        }
    }

    /**
     * Generate resource specific JSON
     * @param resourceType input resource type
     * @param onem2mResource the resource info
     * @param j json obj
     */
    public static JSONObject produceJsonForResource(String resourceType,
                                                    Onem2mResource onem2mResource,
                                                    Boolean needM2MPrefix,
                                                    JSONObject j) {

        JSONObject resourceJsonObject = new JSONObject();

        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                ResourceAE.produceJsonForResource(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.AE, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.AE, j);
                }
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.produceJsonForResource(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.CONTAINER, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.CONTAINER, j);
                }
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.produceJsonForResource(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.CONTENT_INSTANCE, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.CONTENT_INSTANCE, j);
                }
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.produceJsonForResource(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.SUBSCRIPTION, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.SUBSCRIPTION, j);
                }
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.produceJsonForResource(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.CSE_BASE, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.CSE_BASE, j);
                }
                break;
        }

        return resourceJsonObject;
    }

    /**
     * Generate resource specific JSON for creation only
     * @param resourceType input resource type
     * @param onem2mResource the resource info
     * @param j json obj
     */
    public static JSONObject produceJsonForResourceCreate(String resourceType,
                                                          Onem2mResource onem2mResource,
                                                          Boolean needM2MPrefix,
                                                          JSONObject j) {

        JSONObject resourceJsonObject = new JSONObject();

        switch (resourceType) {

            case Onem2m.ResourceType.AE:
                ResourceAE.produceJsonForResourceCreate(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.AE, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.AE, j);
                }
                break;
            case Onem2m.ResourceType.CONTAINER:
                ResourceContainer.produceJsonForResourceCreate(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.CONTAINER, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.CONTAINER, j);
                }
                break;
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                ResourceContentInstance.produceJsonForResourceCreate(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.CONTENT_INSTANCE, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.CONTENT_INSTANCE, j);
                }
                break;
            case Onem2m.ResourceType.SUBSCRIPTION:
                ResourceSubscription.produceJsonForResourceCreate(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.SUBSCRIPTION, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.SUBSCRIPTION, j);
                }
                break;
            case Onem2m.ResourceType.CSE_BASE:
                ResourceCse.produceJsonForResource(onem2mResource, j);
                if (needM2MPrefix) {
                    resourceJsonObject.put("m2m:" + Onem2m.ResourceTypeString.CSE_BASE, j);
                } else {
                    resourceJsonObject.put(Onem2m.ResourceTypeString.CSE_BASE, j);
                }
                break;
        }

        return resourceJsonObject;
    }
    /**
     * This routine processes the JSON content for this resource representation.  Ideally, a json schema file would
     * be used so that each json key could be looked up in the json schema to find out what type it is, and so forth.
     * Maybe the next iteration of code, I'll create json files for each resource.
     * @param key json content key
     * @param resourceContent fill in with parsed json values
     * @param onem2mResponse response
     * @return valid content
     */
    public static boolean processJsonCommonRetrieveContent(String key,
                                                            ResourceContent resourceContent,
                                                            ResponsePrimitive onem2mResponse) {

        Object o = resourceContent.getJsonContent().get(key);

        switch (key) {

            case CREATION_TIME:
            case LAST_MODIFIED_TIME:
            case EXPIRATION_TIME:
            case RESOURCE_ID:
            case RESOURCE_NAME:
            case PARENT_ID:
                if (!(o instanceof String)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                    return false;
                }
                resourceContent.setDbAttr(key, o.toString());
                break;
            case RESOURCE_TYPE:
            case STATE_TAG:
                if (!(o instanceof Integer)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") integer expected for json key: " + key);
                    return false;
                }
                resourceContent.setDbAttr(key, o.toString());
                break;
            case LABELS:
                if (!(o instanceof JSONArray)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                    return false;
                }
                JSONArray array = (JSONArray) o;
                for (int i = 0; i < array.length(); i++) {
                    if (!(array.get(i) instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json array: " + key);
                        return false;
                    }
                    //resourceContent.setDbAttr(key, array.get(i));
                }
                break;
            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                return false;
        }
        return true;
    }

    /**
     * As soon as we start supporting xml content, each resource will have to implement a method similar to extractJsonRequestContent
     * and it will be called something like extractXmlRequestContent and will be called from parseXmlContent.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private String parseXml(String resourceType, RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                "CONTENT(" + RequestPrimitive.CONTENT + ") xml not supported yet");
        return null;
    }

    public String getDbAttr(String name) {
        return this.dbAttrs.getAttr(name);
    }

    public void setDbAttr(String name, String value) {
        this.dbAttrs.setAttr(name, value);
    }

    public void replaceDbAttr(String name, String value) {
        this.dbAttrs.replaceAttr(name, value);
    }

    public List<Attr> getAttrList() {
        return this.dbAttrs.getAttrList();
    }

    public List<Member> getDbAttrSet(String name) {
        return this.dbAttrSets.getAttrSet(name);
    }

    public void setDbAttrSet(String name, List<Member> memberList) {
        this.dbAttrSets.setAttrSet(name, memberList);
    }

    public List<AttrSet> getAttrSetList() {
        return this.dbAttrSets.getAttrSetsList();
    }
}

