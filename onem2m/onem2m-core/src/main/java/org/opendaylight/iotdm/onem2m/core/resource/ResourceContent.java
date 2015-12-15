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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
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
    public static final String MEMBER_NAME = "nm";
    public static final String MEMBER_TYPE = "typ";

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

        if (onem2mRequest.isCreate) {

            String resourceType = onem2mRequest.getPrimitive(RequestPrimitive.RESOURCE_TYPE);
            this.inJsonContent.put(ResourceContent.RESOURCE_TYPE, Integer.valueOf(resourceType));
        }

        // resourceId, resourceName, parentId is filled in by the Onem2mDb.createResource method

        String currDateTime = Onem2mDateTime.getCurrDateTime();

        if (onem2mRequest.isCreate) {
            this.inJsonContent.put(ResourceContent.CREATION_TIME, currDateTime);
        }

        // always update lmt at create or update
        this.inJsonContent.put(ResourceContent.LAST_MODIFIED_TIME, currDateTime);

        // validate expiration time
        String et = this.getInJsonContent().optString(ResourceContent.EXPIRATION_TIME, null);
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
    public static boolean parseJsonCommonCreateUpdateContent(String key,
                                                           ResourceContent resourceContent,
                                                           ResponsePrimitive onem2mResponse) {

        Object o = resourceContent.getInJsonContent().get(key);

        switch (key) {

            case LABELS:
                if (!resourceContent.getInJsonContent().isNull(key)) {
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
                    }
                }
                break;

            case EXPIRATION_TIME:
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String) /*|| !Onem2mDateTime.isValidDateTime(o.toString())*/) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                "CONTENT(" + RequestPrimitive.CONTENT + ") DATE (YYYYMMDDTHHMMSSZ) string expected for expiration time: " + key);
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

