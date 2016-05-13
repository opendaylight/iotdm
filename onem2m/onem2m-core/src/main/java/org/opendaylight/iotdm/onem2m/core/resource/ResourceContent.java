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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;

import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.CheckAccessControlProcessor;

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
    // todo: we may edit "val" "rn" in the future
    public static final String MEMBER_NAME = "rn";
    public static final String MEMBER_TYPE = "typ";
    public static final String ACCESS_CONTROL_POLICY_IDS = "acpi";


    private JSONObject inJsonContent;
    private String xmlContent;
    protected ArrayList<String> jsonCreateKeys;

    public ResourceContent() {
        inJsonContent = null;
        jsonCreateKeys = null;
        xmlContent = null;
    }

    public boolean isJson() { return inJsonContent != null; }
    public JSONObject getInJsonContent() { return inJsonContent; }
    public ArrayList<String> getInJsonCreateKeys() { return jsonCreateKeys; }


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
                inJsonContent = parseJson(resourceType, onem2mRequest, onem2mResponse);
                if (inJsonContent != null) {
                    jsonCreateKeys = new ArrayList<String>(inJsonContent.length());
                }
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

        JSONObject jsonContent = null;

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
         * Each resource in the CONTENT primitive has a JSON key of its resource type in string format.  For example:
         * if the resource is an AE, the key would be "ae" or "m2m:ae".  This routine plucks off the "resourceType"
         * and returns its resulting JSON object.
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

            Object o = jsonContent.opt(key);

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

            if (resourceType.contentEquals(key)) {
                resourceJsonObject = (JSONObject) o;
                onem2mResponse.setUseM2MPrefix(false);
            } else if (key.contentEquals("m2m:" + resourceType)) {
                resourceJsonObject = (JSONObject) o;
                onem2mResponse.setUseM2MPrefix(true);
            } else {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") resource type mismatch");
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
        boolean isCSECreation = false;
        if (onem2mRequest.isCreate) {

            String resourceType = onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE);
            JsonUtils.put(this.inJsonContent, ResourceContent.RESOURCE_TYPE, Integer.valueOf(resourceType));


            // lookup the resource ... this will be the parent where the new resource will be created

            if (!resourceType.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                String to = onem2mRequest.getPrimitive(RequestPrimitive.TO);
                if (!Onem2mDb.getInstance().findResourceUsingURI(to, onem2mRequest, onem2mResponse)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                            "Resource target URI not found: " + to);
                    return;
                }

                // the Onem2mResource is now stored in the onem2mRequest ... as it has been read in from the data store

                // special case for AE resources ... where resource name is derived from FROM parameter
                String resourceName = this.getInJsonContent().optString(ResourceContent.RESOURCE_NAME, null);

                // if the a name is provided, ensure it is valid and unique at this hierarchical level
                if (resourceName != null) {
                    // using the parent, see if this new resource name already exists under this parent resource
                    if (Onem2mDb.getInstance().findResourceUsingIdAndName(onem2mRequest.getOnem2mResource().getResourceId(), resourceName)) {
                        // TS0004: 7.2.3.2
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONFLICT,
                                "Resource already exists: " + onem2mRequest.getPrimitive(RequestPrimitive.TO) + "/" + resourceName);
                        return;
                    }
                    onem2mRequest.setResourceName(resourceName);
                }
            } else {
                // set default expirationTime for CSE
                isCSECreation = true;
            // todo: update this part once resourceName is supported for CSE
            }


        }

        if (onem2mRequest.isUpdate) {
            // nm cannot be updated
            String resourceName = this.getInJsonContent().optString(ResourceContent.RESOURCE_NAME, null);
            if (resourceName != null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "Resource Name cannot be updated: " + onem2mRequest.getPrimitive(RequestPrimitive.TO) + "/" + resourceName);
                return;
            }

        }
        // resourceId, resourceName, parentId is filled in by the Onem2mDb.createResource method

        String currDateTime = Onem2mDateTime.getCurrDateTime();

        if (onem2mRequest.isCreate) {
            JsonUtils.put(this.inJsonContent, ResourceContent.CREATION_TIME, currDateTime);
        }

        // always update lmt at create or update
        JsonUtils.put(this.inJsonContent, ResourceContent.LAST_MODIFIED_TIME, currDateTime);

        // validate expiration time
        String et = this.getInJsonContent().optString(ResourceContent.EXPIRATION_TIME, null);
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
                    this.inJsonContent.put(ResourceContent.EXPIRATION_TIME, Onem2mDateTime.FOREVER);
                    return;
                }
                // et is not given, if parent is CSE, put FOREVER, otherwise copy parent's et
                // creation's parent resource is stored in onem2mrequest.onem2mreosurce, update's self resource is stored there

                Onem2mResource parentResource = getParentResource(onem2mRequest);

                if (parentResource.getResourceType().contentEquals(Onem2m.ResourceType.CSE_BASE)) {
                    this.inJsonContent.put(ResourceContent.EXPIRATION_TIME, Onem2mDateTime.FOREVER);
                } else {
                    this.inJsonContent.put(ResourceContent.EXPIRATION_TIME, getParentExpTime(onem2mRequest));
                }
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
        return parentJson.optString(ResourceContent.EXPIRATION_TIME);
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
    public static boolean parseJsonCommonCreateUpdateContent(String key,
                                                             ResourceContent resourceContent,
                                                             ResponsePrimitive onem2mResponse) {

        Object o = resourceContent.getInJsonContent().opt(key);

        switch (key) {

            case LABELS:
            case ACCESS_CONTROL_POLICY_IDS:
                if (!resourceContent.getInJsonContent().isNull(key)) {
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
                    }
                }
                break;

            case EXPIRATION_TIME:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String) || !Onem2mDateTime.isValidDateTime(o.toString())) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") DATE (YYYYMMDDTHHMMSS) string expected for expiration time: " + key);
                        return false;
                    }

                }
                break;
            case RESOURCE_NAME:
                if (!resourceContent.getInJsonContent().isNull(key)) {
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
}

