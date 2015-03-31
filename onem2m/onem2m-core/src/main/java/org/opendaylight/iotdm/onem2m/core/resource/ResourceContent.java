/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the resource content that was supplied in the RequestPrimitive.CONTENT parameter.  It is
 * formatted according to the CONTENT_TYPE.   It is parsed and the parameter are put in the DbAttr list.  Resource
 * specific methods are called based on the resourceType that is being created.
 */
public class ResourceContent {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceContent.class);

    public static final String RESOURCE_TYPE = "rty";
    public static final String RESOURCE_ID = "ri";
    public static final String RESOURCE_NAME = "rn";
    public static final String PARENT_ID = "pi";
    public static final String EXPIRATION_TIME = "et";
    public static final String CREATION_TIME = "ct";
    public static final String LAST_MODIFIED_TIME = "lt";
    public static final String LABELS = "lbl";
    public static final String STATE_TAG = "stateTag";
    public static final String CHILD_RESOURCE = "childResource";
    public static final String CHILD_RESOURCE_REF = "childResourceRef";

    private DbAttr dbAttrs;

    public ResourceContent() {
        dbAttrs = new DbAttr();
    }

    /**
     * Pulls the json/xml formatted data out of the RequestPrimitive.CONTENT string
     * and put it into the request.  It calls an abstract method so that each resource pulls out the data
     * specific to that resource.
     * @param onem2mRequest
     * @param onem2mResponse
     */
    public void parseRequestContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
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
    private void parseJsonRequestContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        JSONObject jsonContent = null;
        String jsonContentString = onem2mRequest.getPrimitive(RequestPrimitive.CONTENT);
        if (jsonContentString == null) {
            // TS0004: 7.2.3.2
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") not specified");
            return;
        }
        try {
            jsonContent = new JSONObject(jsonContentString.trim());
        } catch (JSONException e) {
            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                    "CONTENT(" + RequestPrimitive.CONTENT + ") parser error (" + e + ")");
            return;
        }

        processJsonContent(onem2mRequest, onem2mResponse, jsonContent);
    }

    private void processJsonContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse, JSONObject jc) {

        /**
         * Take each json attribute and validate it against the valid attributes set in the request
         */
        Set<String> validAttributes = onem2mRequest.getValidAttributes();
        Iterator<?> keys = jc.keys();
        while( keys.hasNext() ) {
            String key = (String)keys.next();
            Object o = jc.get(key);
            if ( o instanceof JSONObject ) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") JSONObject found: " + key);
                return;
            } else if (o instanceof String) {
                //LOG.info("key: {}, val: {}", key, o.toString());
                if (validAttributes.contains(key)) {
                    this.setDbAttr(key, o.toString());
                } else {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                            "CONTENT(" + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
                }
            } else if (o instanceof JSONArray) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                        "CONTENT(" + RequestPrimitive.CONTENT + ") JSONArray found: " + key);
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
    private void parseXmlRequestContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {
        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                "CONTENT(" + RequestPrimitive.CONTENT + ") xml not supported yet");
    }

    public String getDbAttr(String name) {
        return this.dbAttrs.getAttr(name);
    }

    public void setDbAttr(String name, String value) {
        this.dbAttrs.setAttr(name, value);
    }

    public DbAttr getDbAttrList() {
        return this.dbAttrs;
    }

    public List<Attr> getAttrList() {
        return this.dbAttrs.getAttrList();
    }

    public void setAttrList(List<Attr> attrList) {
        this.dbAttrs = new DbAttr(attrList);
    }
}

