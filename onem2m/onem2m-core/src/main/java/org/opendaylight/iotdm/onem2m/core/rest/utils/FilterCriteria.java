/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.rest.utils;

import java.util.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterCriteria {

    private static final Logger LOG = LoggerFactory.getLogger(FilterCriteria.class);

    public enum FCEnum {
        CREATED_BEFORE(1),
        CREATED_AFTER(2),
        MODIFIED_SINCE(3),
        UNMODIFIED_SINCE(4),
        STATE_TAG_SMALLER(5),
        STATE_TAG_BIGGER(6),
        EXPIRE_BEFORE(7),
        EXPIRE_AFTER(8),
        LABELS(9),
        RESOURCE_TYPE(10),
        SIZE_ABOVE(11),
        SIZE_BELOW(12),
        CONTENT_TYPE(13),
        ATTRIBUTE(14),
        FILTER_USAGE(15),
        LIMIT(16);

        private int value;

        private FCEnum(int value) {
            this.value = value;
        }
    }

    DbAttr dbAttrs;
    // hard code set of acceptable primitive attributes, short and long name
    public static final Map<String,FCEnum> validAttributes = new HashMap<String,FCEnum>() {{
        // short; long
        put("crb", FCEnum.CREATED_BEFORE); put("createdBefore", FCEnum.CREATED_BEFORE);
    }};

    public FilterCriteria() {
        dbAttrs = new DbAttr();
    }

    private JSONObject jsonContent;
    public void setContentJSONObj(JSONObject fc) {
        this.jsonContent = fc;
    }
    public JSONObject getContentJSONObj() {
        return this.jsonContent;
    }
    public FilterCriteria(JSONObject jsonContent) {
        this.jsonContent = jsonContent;
    }


    /**
     * It pulls the json/xml formatted data out of the content string
     * and puts it into the request.  It calls an abstract method so that each resource pulls out the data
     * specific to that resource.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public static void parseRequestContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        String cf = onem2mRequest.getPrimitive(RequestPrimitive.CONTENT_FORMAT);
        switch (cf) {
            case Onem2m.ContentFormat.JSON:
                parseJsonRequestContent(onem2mRequest, onem2mResponse);
                break;
            case Onem2m.ContentFormat.XML:
                parseXmlRequestContent(onem2mRequest, onem2mResponse);
                break;
            default:
                assert(false); // this is a bug if it reaches here as it is prechecked alot earlier
                break;
        }
    }

    /**
     * Parse the JSON content, put it into the set of RequestPrimitive attrs by calling the extractJsonRequestContent
     * which is implemented by each of the resource specific classes as it is an abstract method.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void parseJsonRequestContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        JSONObject jsonContent = null;
        String jsonContentString = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA);
        if (jsonContentString == null) {
            // TS0004: 7.2.3.2
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT(" + RequestPrimitive.FILTER_CRITERIA + ") not specified");
            return;
        }
        try {
            jsonContent = new JSONObject(jsonContentString.trim());
        } catch (JSONException e) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "CONTENT(" + RequestPrimitive.FILTER_CRITERIA + ") parser error (" + e + ")");
            return;
        }

        // put the content into the content primitives so resource specific methods can process it.
        FilterCriteria fc = new FilterCriteria(jsonContent);
        //onem2mResponse.setFilterCriteria(fc);
        fc.processJsonPrimitives(onem2mRequest, onem2mResponse);
    }

    public void processJsonPrimitives(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        /**
         * Take each json attribute and validate it against the valid attributes set in the request
         */
        JSONObject j = this.getContentJSONObj();
        Iterator<?> keys = j.keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();
            Object o = j.get(key);
            if ( o instanceof JSONObject ) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") JSONObject key: " + key);
                return;
            } else if (o instanceof String) {
                LOG.info("key: {}, val: {}", key, o.toString());
                if (validAttributes.containsKey(key)) {
                    this.dbAttrs.setAttr(key, o.toString());
                } else {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: {}" + key);
                    return;
                }
            } else if (o instanceof JSONArray) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") JSONArray key: " + key);
                return;
            }
        }
    }

    /**
     * As soon as we start supporting xml content, each resource will have to implement a method similar to extractJsonRequestContent
     * and it will be called something like extractXmlRequestContent and will be called from parseXmlContent.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    private static void parseXmlRequestContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                "CONTENT(" + RequestPrimitive.CONTENT + ") xml not supported yet");
    }

    public static boolean resourcePassesFilterCriteria(String resourceId) {

        /*
        ResourceContent resultContent = new ResourceContent(resourceId);

        for (Attr attr : dbAttrs.getAttrList()) {
            switch (attr.getName()) {
                case CREATED_BEFORE:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.CREATION_TIME))
                        return false;
                    break;
                case CREATED_AFTER:
                    if (attr.getValue() > resultContent.getDbAttr(ResourceContent.CREATION_TIME))
                        return false;
                    break;
                case MODIFIED_SINCE:
                    if (attr.getValue() > resultContent.getDbAttr(ResourceContent.LAST_MODIFIED_TIME))
                        return false;
                    break;
                case UNMODIFIED_SINCE:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.LAST_MODIFIED_TIME))
                        return false;
                    break;
                case STATE_TAG_SMALLER:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case STATE_TAG_BIGGER:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case EXPIRE_BEFORE:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case EXPIRE_AFTER:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case RESOURCE_TYPE:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case SIZE_ABOVE:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case SIZE_BELOW:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case CONTENT_TYPE:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case ATTRIBUTE:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case FILTER_USAGE:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
                case LIMIT:
                    if (attr.getValue() < resultContent.getDbAttr(ResourceContent.STATE_TAG))
                        return false;
                    break;
            }
        }
        */
        return true;
    }

    public static boolean attributePassesFilterCriteria(String resoruceId) {
        /*
        CREATED_BEFORE(1),
                CREATED_AFTER(2),
                MODIFIED_SINCE(3),
                UNMODIFIED_SINCE(4),
                STATE_TAG_SMALLER(5),
                STATE_TAG_BIGGER(6),
                EXPIRE_BEFORE(7),
                EXPIRE_AFTER(8),
                LABELS(9),
                RESOURCE_TYPE(10),
                SIZE_ABOVE(11),
                SIZE_BELOW(12),
                CONTENT_TYPE(13),
                ATTRIBUTE(14),
                FILTER_USAGE(15),
                LIMIT(16);
                */
        return true;
    }
}

