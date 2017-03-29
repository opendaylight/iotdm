/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest.utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.BaseResource;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponsePrimitive extends BasePrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(ResponsePrimitive.class);

    /**
     * This is the onem2m response primitive.  When the onem2m core processes the input requestPrimitive parms,
     * it must send back a one m2m responsePrimitive.  Restconf will get these in the output parameters.  As far
     * as the onenm2m protocols, each protocol must take this response and adapt it for that protocol.  For
     * example: the onem2m status code must be mapped to the appropriate coap status code.
     */

    // taken from CDT-responsePrimitive-v1_0_0.xsd / TS0004_v_1-0_1 Section 8.2.2 Short Names
            // TODO: TS0004: table 7.1.1.2 1, what goes into CONTENT
    public static final String RESPONSE_STATUS_CODE = "rsc";
    public static final String REQUEST_IDENTIFIER = "rqi";
    public static final String CONTENT = "pc";
    public static final String TO = "to";
    public static final String FROM = "fr";
    public static final String CONTENT_FORMAT = "content_format";
    public static final String HTTP_CONTENT_TYPE = "http_content_type";
    public static final String CONTENT_LOCATION = "onem2m_content_location";

    private String primitiveResponseStatusCode;
    public String getPrimitiveResponseStatusCode() { return primitiveResponseStatusCode; }
    public void setPrimitiveResponseStatusCode(String primitiveResponseStatusCode) {
        this.primitiveResponseStatusCode = primitiveResponseStatusCode;
        setPrimitive(RESPONSE_STATUS_CODE, primitiveResponseStatusCode);
    }
    private String primitiveContent;
    public String getPrimitiveContent() { return primitiveContent; }
    public void setPrimitiveContent(String primitiveContent) {
        this.primitiveContent = primitiveContent;
        setPrimitive(CONTENT, primitiveContent);
    }

    protected String primitiveContentFormat;
    public String getPrimitiveContentFormat() { return primitiveContentFormat; }
    public void setPrimitiveContentFormat(String primitiveContentFormat) {
        this.primitiveContentFormat = primitiveContentFormat;
        setPrimitive(CONTENT_FORMAT, primitiveContentFormat);
    }

    private String primitiveHttpContentType;
    public String getPrimitiveHttpContentType() { return primitiveHttpContentType; }
    public void setPrimitiveHttpContentType(String primitiveHttpContentType) {
        this.primitiveHttpContentType = primitiveHttpContentType;
        setPrimitive(HTTP_CONTENT_TYPE, primitiveHttpContentType);
    }

    private String primitiveContentLocation;
    public String getPrimitiveContentLocation() { return primitiveContentLocation; }
    public void setPrimitiveContentLocation(String primitiveContentLocation) {
        this.primitiveContentLocation = primitiveContentLocation;
        setPrimitive(CONTENT_LOCATION, primitiveContentLocation);
    }

    private String primitiveRequestIdentifier;
    public String getPrimitiveRequestIdentifier() { return primitiveRequestIdentifier; }
    public void setPrimitiveRequestIdentifier(String primitiveRequestIdentifier) {
        this.primitiveRequestIdentifier = primitiveRequestIdentifier;
        setPrimitive(REQUEST_IDENTIFIER, primitiveRequestIdentifier);
    }

    public ResponsePrimitive() {
        super();
    }

    public void setRSC(String rsc, String content) {
        setPrimitiveResponseStatusCode(rsc);
        setPrimitiveContent(JsonUtils.put(new JSONObject(), "error", content).toString());
        setPrimitiveHttpContentType(Onem2m.ContentType.APP_VND_RES_JSON);
        setPrimitiveContentFormat(Onem2m.ContentFormat.JSON);
    }

    private boolean useHierarchicalAddressing;
    public void setUseHierarchicalAddressing(boolean ha) {
        this.useHierarchicalAddressing = ha;
    }
    public boolean useHierarchicalAddressing() {
        return this.useHierarchicalAddressing;
    }

    // the original resourceContent used to return content based on result content requested
    protected BaseResource baseResource;
    public void setBaseResource(BaseResource rc) {
        this.baseResource = rc;
    }
    public BaseResource getBaseResource() {
        return this.baseResource;
    }

    private JSONObject jsonResourceContent;
    public void setJsonResourceContent(String jsonResourceContentString) {
        try {
            this.jsonResourceContent = new JSONObject(jsonResourceContentString);
        } catch (JSONException e) {
            LOG.error("Invalid JSON {}", jsonResourceContentString, e);
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }
    public void setJsonResourceContent(JSONObject jsonResourceContent) {
        this.jsonResourceContent = jsonResourceContent;
    }
    public JSONObject getJsonResourceContent() {
        return this.jsonResourceContent;
    }

    /**
     * @return JSONObject built from basic primitive fields
     */
    public JSONObject toJson() {
        JSONObject responseJson = new JSONObject();
        JsonUtils.put(
                responseJson, ResponsePrimitive.CONTENT,
                this.getPrimitive(ResponsePrimitive.CONTENT));
        JsonUtils.put(
                responseJson, ResponsePrimitive.RESPONSE_STATUS_CODE,
                this.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE));
        JsonUtils.put(
                responseJson, ResponsePrimitive.REQUEST_IDENTIFIER,
                this.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER));
        return responseJson;
    }
}
