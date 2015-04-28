/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mResponsePrimitiveClient {

    private String responseStatusCode;
    private String requestIdentifier;
    private String content;
    private boolean success = false;

    public static final String RESPONSE_STATUS_CODE = "rsc";
    public static final String REQUEST_IDENTIFIER = "rqi";
    public static final String CONTENT = "pc";
    public static final String TO = "to";
    public static final String FROM = "fr";
    public static final String ORIGINATING_TIMESTAMP = "ot";
    public static final String RESULT_EXPIRATION_TIMESTAMP = "rset";
    public static final String EVENT_CATEGORY = "ec";

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mResponsePrimitiveClient.class);

    public Onem2mResponsePrimitiveClient(ResponsePrimitive onem2mResponse) {


        requestIdentifier = onem2mResponse.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER);
        responseStatusCode = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
        success = responseStatusCode.charAt(0) == '2' ? true : false;
        content = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
    }

    public boolean responseOk() {
        return success;
    }
    public String getContent() {
        return this.content;
    }
    public String getRequestIdentifier() {
        return this.requestIdentifier;
    }
    public String getResponseStatusCode() {
        return this.responseStatusCode;
    }
}

