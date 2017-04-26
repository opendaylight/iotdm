/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;

import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;

import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the resource content that was supplied in the RequestPrimitive.CONTENT parameter.  It is
 * formatted according to the CONTENT_TYPE.   It is parsed and the parameter are put in the DbAttr list.  Resource
 * specific methods are called based on the resourceType that is being created.
 */
public class BaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(BaseResource.class);

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
    // todo: we may edit "val" "rn" in the future
    public static final String MEMBER_NAME = "rn";
    public static final String MEMBER_TYPE = "typ";
    public static final String ACCESS_CONTROL_POLICY_IDS = "acpi";
    //announceTo, announcedAttribute, cseType, pointOfAccess, CSEBase, CSE-ID, M2M-Ext-ID, Trigger-Recipient-ID, requestReachability, nodeLink
    
    RequestPrimitive onem2mRequest;
    ResponsePrimitive onem2mResponse;

    protected JSONObject jsonPrimitiveContent;
    private String xmlContent;
    //protected Set<String> jsonCreateKeys;
    protected String[] jsonCreateKeys;
    protected List<String> acpiArray;


    public BaseResource(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        this.onem2mRequest = onem2mRequest;
        this.onem2mResponse = onem2mResponse;

        jsonPrimitiveContent = null;
        jsonCreateKeys = null;
        xmlContent = null;
    }

    public boolean isJson() { return jsonPrimitiveContent != null; }
    public JSONObject getInJsonContent() { return jsonPrimitiveContent; }
    public String[] getInJsonCreateKeys() { return jsonCreateKeys; }
    public List<String> getACPIArray() { return acpiArray; }


    public boolean isXml() { return xmlContent != null; }

    public void parse(String resourceType) {
        String cf = onem2mRequest.getPrimitiveContentFormat();
        switch (cf) {
            case Onem2m.ContentFormat.JSON:
                jsonPrimitiveContent = parseJson(resourceType);
                if (jsonPrimitiveContent != null) {
                    jsonCreateKeys = JSONObject.getNames(jsonPrimitiveContent);
                }
                break;
            case Onem2m.ContentFormat.XML:
                xmlContent = parseXml(resourceType);
                break;
        }
    }

    private JSONObject parseJson(String resourceType) {

        JSONObject jsonContent = null;

        String jsonContentString = onem2mRequest.getPrimitiveContent();
        if (jsonContentString == null) {
            // TS0004: 7.2.3.2
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") not specified");
            return null;
        }
        try {
            jsonContent = new JSONObject(jsonContentString);
        } catch (JSONException e) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") parser error (" + e + ")");
            return null;
        }

        JSONObject resSpecificJsonObject = jsonContent.optJSONObject("m2m:" + resourceType);
        if (resSpecificJsonObject == null) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") m2m:" + resourceType + " resource type expected but "
                            + jsonContent.keys().next().toString() + " was found");
            return null;
        }
        return resSpecificJsonObject;
    }

    public void processCommonCreateUpdateAttributes() {

        boolean isCSECreation = false;

        if (onem2mRequest.isCreate) {

            Integer resourceType = onem2mRequest.getPrimitiveResourceType();
            JsonUtils.put(jsonPrimitiveContent, BaseResource.RESOURCE_TYPE, resourceType);

            if (resourceType == Onem2m.ResourceType.CSE_BASE) {
                // set default expirationTime for CSE
                isCSECreation = true;
            // todo: update this part once resourceName is supported for CSE
            } else {
                // if name was in header, use it, otherwise use the name in the resource
                if (onem2mRequest.getPrimitiveName() == null) {
                    String resourceName = jsonPrimitiveContent.optString(BaseResource.RESOURCE_NAME, null);
                    if (resourceName != null) {
                        // using the parent, see if this new resource name already exists under this parent resource
                        if (Onem2mDb.getInstance().findChildFromParentAndChildName(
                                onem2mRequest.getParentResourceId(), resourceName) != null) {
                            // TS0004: 7.2.3.2
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONFLICT,
                                    "Resource already exists: " + onem2mRequest.getPrimitiveTo() + "/" + resourceName);
                            return;
                        }
                        onem2mRequest.setResourceName(resourceName);
                        onem2mRequest.setPrimitiveName(resourceName);
                    }
                }
            }

        } else if (onem2mRequest.isUpdate) {
            // nm cannot be updated
            String resourceName = jsonPrimitiveContent.optString(BaseResource.RESOURCE_NAME, null);
            if (resourceName != null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Resource Name cannot be updated: " + onem2mRequest.getPrimitiveTo() + "/" + resourceName);
                return;
            }

        }
        // resourceId, resourceName, parentId is filled in by the Onem2mDb.createResource method

        String currDateTime = Onem2mDateTime.getCurrDateTime();

        if (onem2mRequest.isCreate) {
            JsonUtils.put(jsonPrimitiveContent, BaseResource.CREATION_TIME, currDateTime);
        }

        // always update lmt at create or update
        JsonUtils.put(jsonPrimitiveContent, BaseResource.LAST_MODIFIED_TIME, currDateTime);

        // validate expiration time
        String et = jsonPrimitiveContent.optString(BaseResource.EXPIRATION_TIME, null);
        if (!isCSECreation) {
            if (et != null) {
                // need to compare nowTime and parentExpTime
                if (Onem2mDateTime.dateCompare(et, currDateTime) < 0) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                            "EXPIRATION_TIME: cannot be less than current time");
                    return;
                }
                if (!isValidExpTime(et, onem2mRequest)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                            "EXPIRATION_TIME: cannot be later than parent Expiration Time");
                    return;
                }
            } else if (onem2mRequest.isCreate){
                // default ACP is the special case, onem2mRequest does not contain onem2mResource
                if (onem2mRequest.getResourceName() != null && onem2mRequest.getResourceName().contentEquals("_defaultACP")) {
                    jsonPrimitiveContent.put(BaseResource.EXPIRATION_TIME, Onem2mDateTime.FOREVER);
                    return;
                }
                // et is not given, if parent is CSE, put FOREVER, otherwise copy parent's et
                // creation's parent resource is stored in onem2mrequest.onem2mreosurce, update's self resource is stored there


//                Onem2mResource parentResource = getParentResource(trc, onem2mRequest);
//
//                if (parentResource.getResourceType().contentEquals(Onem2m.ResourceType.CSE_BASE)) {
//                    this.inJsonContent.put(ResourceContent.EXPIRATION_TIME, Onem2mDateTime.FOREVER);
//                } else {
//                    this.inJsonContent.put(ResourceContent.EXPIRATION_TIME, getParentExpTime(trc, onem2mRequest));
//                }
            }
        }

    }

    private Onem2mResource getParentResource(RequestPrimitive onem2mRequest) {
        Onem2mResource parentResource;
        if (onem2mRequest.isCreate) {
            parentResource = onem2mRequest.getOnem2mResource();
        } else {
            String parentID = onem2mRequest.getOnem2mResource().getParentId();
            parentResource = Onem2mDb.getInstance().getResource(parentID);
        }
        return parentResource;
    }

    private Boolean isValidExpTime(String et, RequestPrimitive onem2mRequest) {
        String parentExpirationTime = getParentExpTime(onem2mRequest);
        if (parentExpirationTime == null || parentExpirationTime.contentEquals(Onem2mDateTime.FOREVER)) {
            //CSE does not have ET
            return true;
        }
        return (Onem2mDateTime.dateCompare(et, parentExpirationTime) <= 0);

    }

    private String getParentExpTime(RequestPrimitive onem2mRequest) {
        Onem2mResource parentResource = getParentResource(onem2mRequest);
        JSONObject parentJson = new JSONObject(parentResource.getResourceContentJsonString());
        return parentJson.optString(BaseResource.EXPIRATION_TIME);
    }

    public boolean parseJsonCommonCreateUpdateContent(String key) {

        Object o = jsonPrimitiveContent.opt(key);

        switch (key) {

            case LABELS:
            case ACCESS_CONTROL_POLICY_IDS:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof JSONArray)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                        return false;
                    }
                    JSONArray array = (JSONArray) o;
                    for (int i = 0; i < array.length(); i++) {

                        if (!(array.opt(i) instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json array: " + key);
                            return false;
                        }
                        if (key.contentEquals(ACCESS_CONTROL_POLICY_IDS)) {
                            if (acpiArray == null) {
                                acpiArray = new ArrayList<>();
                            }
                            acpiArray.add(array.opt(i).toString());
                        }
                    }
                }
                break;

            case EXPIRATION_TIME:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String) || !Onem2mDateTime.isValidDateTime(o.toString())) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") DATE (YYYYMMDDTHHMMSS) string expected for expiration time: " + key);
                        return false;
                    }

                }
                break;

            case RESOURCE_NAME:
                if (!jsonPrimitiveContent.isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for: " + key);
                        return false;
                    }

                }
                break;

            case CREATION_TIME:
            case LAST_MODIFIED_TIME:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, key + ": read-only parameter");
                return false;

            default:
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                return false;
        }
        return true;
    }

    private String parseXml(String resourceType) {
        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                "CONTENT(" + RequestPrimitive.CONTENT + ") xml not supported yet");
        return null;
    }

    public static void incrementParentStateTagIfPresent(JSONObject parentResourceContent) {
        if(parentResourceContent.has(ResourceContainer.STATE_TAG)) {
            JsonUtils.inc(parentResourceContent, ResourceContainer.STATE_TAG);
        }
    }
}

