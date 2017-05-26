/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.commchannels.common;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of IotdmPluginResponse. It can maintain the basic onem2m response primitive in json format.
 */
public class IotdmPluginOnem2mBaseResponse extends Onem2mProtocolPluginResponse {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginOnem2mBaseResponse.class);

    private ResponsePrimitive response;

    private void initResponse() {
        if(Objects.isNull(response)) {
            response = new ResponsePrimitive();
        }
    }

    /**
     * Make sure you filled out return response primitive before building response.
     * @return json response message
     * @throws IllegalArgumentException if response primitive is not filled
     */
    public String buildWebsocketResponse() {
        if(Objects.nonNull(response)) {
            return response.toJson().toString();
        }
        else {
            throw new IllegalArgumentException("IotdmPluginOnem2mBaseResponse: response primitive undefined.");
        }
    }

    /**
     * set given onem2m RESPONSE_STATUS_CODE (rsc) to this response, initialize if needed
     * @param returnCode The onem2m return code value to be set.
     */
    @Override
    public void setReturnCode(int returnCode) {
        initResponse();
        response.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, String.valueOf(returnCode));
    }

    /**
     * set given responsePayload (pc) to this response, initialize if needed
     * @param responsePayload Payload as a string to be set.
     */
    @Override
    public void setResponsePayload(@Nonnull String responsePayload) {
        initResponse();
        response.setPrimitive(ResponsePrimitive.CONTENT, responsePayload);
    }

    /**
     * set given contentType to this response, initialize if needed
     * @param contentType Type of the content as string value.
     */
    @Override
    public void setContentType(@Nonnull String contentType) {
        initResponse();
        response.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, contentType);
    }

    /**
     * set given ResponsePrimitive to this response
     * @param onem2mResponse A Onem2m response primitive.
     * @return true when assigned to response
     */
    @Override
    public boolean setFromResponsePrimitive(ResponsePrimitive onem2mResponse) {
        this.response = onem2mResponse;
        return true;
    }

    /**
     * set basic response fields of ResponsePrimitive with error body
     * @param returnCode onem2m return code
     * @param message error message
     */
    public void prepareErrorResponse(@Nonnull String returnCode, @Nonnull String message,
                                     String requestId) {
        initResponse();
        response.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, returnCode);
        response.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, Onem2m.ContentFormat.JSON);
        response.setPrimitive(ResponsePrimitive.CONTENT, JsonUtils.put(new JSONObject(), "error", message).toString());
        if (null != requestId && ! requestId.isEmpty()) {
            response.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER, requestId);
        }
    }

    /**
     * build json error response
     * @param returnCode onem2m return code
     * @param message error message
     * @return json error response (ResponsePrimitive in json format)
     */
    public static String buildErrorResponse(@Nonnull String returnCode, @Nonnull String message) {
        ResponsePrimitive response = new ResponsePrimitive();
        response.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, returnCode);
        response.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, Onem2m.ContentFormat.JSON);
        response.setPrimitive(ResponsePrimitive.CONTENT, JsonUtils.put(new JSONObject(), "error", message).toString());
        return response.toJson().toString();
    }
}

