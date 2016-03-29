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
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
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
    public static final String ORIGINATING_TIMESTAMP = "ot";
    public static final String RESULT_EXPIRATION_TIMESTAMP = "rset";
    public static final String EVENT_CATEGORY = "ec";
    public static final String HTTP_CONTENT_TYPE = "http_content_type";
    public static final String HTTP_CONTENT_LOCATION = "http_content_location";

    public ResponsePrimitive() {
        super();
    }

    public void setRSC(String rsc, String content) { //throws Onem2mRSCException {
        this.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, rsc);
        this.setPrimitive(ResponsePrimitive.CONTENT, JsonUtils.put(new JSONObject(), "error", content).toString());
        //throw new Onem2mRSCException();
    }

    private boolean useHierarchicalAddressing;
    public void setUseHierarchicalAddressing(boolean ha) {
        this.useHierarchicalAddressing = ha;
    }
    public boolean useHierarchicalAddressing() {
        return this.useHierarchicalAddressing;
    }

    // the original resourceContent used to return content based on result content requested
    protected ResourceContent resourceContent;
    public void setResourceContent(ResourceContent rc) {
        this.resourceContent = rc;
    }
    public ResourceContent getResourceContent() {
        return this.resourceContent;
    }

    // the original resourceContent used to return content based on result content requested
    protected boolean useM2MPrefix;
    public void setUseM2MPrefix(boolean useM2MPrefix) {
        this.useM2MPrefix = useM2MPrefix;
    }
    public boolean useM2MPrefix() {
        return this.useM2MPrefix;
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
}
