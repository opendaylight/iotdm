/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mProtocolPluginResponse;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of class wrapping original HTTP response which is send
 * back to the client.
 */
public class IotdmPluginHttpResponse extends Onem2mProtocolPluginResponse {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginHttpResponse.class);

    private final HttpServletResponse httpResponse; // The original HTTP response

    IotdmPluginHttpResponse(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    @Override
    public void setReturnCode(int returnCode) {
        this.httpResponse.setStatus(returnCode);
    }

    @Override
    public void setResponsePayload(String responsePayload) {
        if (null == responsePayload) {
            return;
        }

        try {
            this.httpResponse.getWriter().println(responsePayload);
        } catch (IOException e) {
            LOG.error("Failed to write HTTP response payload: {}", e);
        }
    }

    @Override
    public void setContentType(String contentType) {
        if (null != contentType) {
            this.httpResponse.setContentType(contentType);
        }
    }

    public void setHeader(String name, String value) {
        if (null != name && null != value) {
            this.httpResponse.setHeader(name, value);
        }
    }

    public void addHeader(String name, String value) {
        if (null != name && null != value) {
            this.httpResponse.addHeader(name, value);
        }
    }

    public void setRequestId(String id) {
        if (null != id && ! id.isEmpty()) {
            this.httpResponse.addHeader(Onem2m.HttpHeaders.X_M2M_RI, id);
        }
    }

    protected static void setOnem2mHttpStatusCode(HttpServletResponse httpResponse, String rscString) {

        httpResponse.setHeader(Onem2m.HttpHeaders.X_M2M_RSC, rscString);
        int httpRsc = HttpServletResponse.SC_BAD_REQUEST;

        switch (rscString) {
            case Onem2m.ResponseStatusCode.OK:
                httpRsc = HttpServletResponse.SC_OK;
                break;
            case Onem2m.ResponseStatusCode.CREATED:
                httpRsc = HttpServletResponse.SC_CREATED;
                break;
            case Onem2m.ResponseStatusCode.CHANGED:
                httpRsc = HttpServletResponse.SC_OK;
                break;
            case Onem2m.ResponseStatusCode.DELETED:
                httpRsc = HttpServletResponse.SC_OK;
                break;
            case Onem2m.ResponseStatusCode.NOT_FOUND:
                httpRsc = HttpServletResponse.SC_NOT_FOUND;
                break;
            case Onem2m.ResponseStatusCode.ACCESS_DENIED:
                httpRsc = HttpServletResponse.SC_FORBIDDEN;
                break;
            case Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED:
                httpRsc = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
                break;
            case Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE:
                httpRsc = HttpServletResponse.SC_BAD_REQUEST;
                break;
            case Onem2m.ResponseStatusCode.CONFLICT:
                httpRsc = HttpServletResponse.SC_CONFLICT;
                break;
            case Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR:
                httpRsc = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                break;
            case Onem2m.ResponseStatusCode.NOT_IMPLEMENTED:
                httpRsc = HttpServletResponse.SC_NOT_IMPLEMENTED;
                break;
            case Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE:
                httpRsc = HttpServletResponse.SC_NOT_FOUND;
                break;
            case Onem2m.ResponseStatusCode.ALREADY_EXISTS:
                httpRsc = HttpServletResponse.SC_FORBIDDEN;
                break;
            case Onem2m.ResponseStatusCode.TARGET_NOT_SUBSCRIBABLE:
                httpRsc = HttpServletResponse.SC_FORBIDDEN;
                break;
            case Onem2m.ResponseStatusCode.NON_BLOCKING_REQUEST_NOT_SUPPORTED:
                httpRsc = HttpServletResponse.SC_NOT_IMPLEMENTED;
                break;
            case Onem2m.ResponseStatusCode.INVALID_ARGUMENTS:
                httpRsc = HttpServletResponse.SC_BAD_REQUEST;
                break;
            case Onem2m.ResponseStatusCode.INSUFFICIENT_ARGUMENTS:
                httpRsc = HttpServletResponse.SC_BAD_REQUEST;
                break;
            case Onem2m.ResponseStatusCode.NOT_ACCEPTABLE:
                httpRsc = HttpServletResponse.SC_NOT_ACCEPTABLE;
                break;
        }

        httpResponse.setStatus(httpRsc);
    }

    /**
     * Sets response parameters describing error.
     * @param httpResponse The original HTTP response
     * @param message Error message string
     * @param onem2mRsc Response status code
     */
    public static void prepareErrorResponse(HttpServletResponse httpResponse,
                                            String message, String onem2mRsc) {

        setOnem2mHttpStatusCode(httpResponse, onem2mRsc);
        try {
            httpResponse.getWriter().println(JsonUtils.put(new JSONObject(), "error", message).toString());
            httpResponse.setContentType("text/json;charset=utf-8");
        } catch (IOException e) {
            LOG.error("Failed to write error message: {}", message);
        }
        Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
    }

    /**
     * Fills original HTTP response with data from Onem2m response.
     * @param onem2mResponse The Onem2m response
     * @param httpResponse The original HTTP response
     * @return
     */
    public static boolean fromOnem2mResponseToHttp(ResponsePrimitive onem2mResponse,
                                                   HttpServletResponse httpResponse) {
        // the content is already in the required format ...
        String content = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
        String rqi = onem2mResponse.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER);
        if (rqi != null) {
            httpResponse.setHeader(Onem2m.HttpHeaders.X_M2M_RI, rqi);
        }

        setOnem2mHttpStatusCode(httpResponse, rscString);
        if (content != null) {
            String ct = onem2mResponse.getPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE);
            if (ct != null) {
                httpResponse.setContentType(ct);
            }

            try {
                httpResponse.getWriter().println(content);
            } catch (IOException e) {
                prepareErrorResponse(httpResponse,
                                     "Failed to write response content", Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR);
                return false;
            }
        }

        String cl = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT_LOCATION);
        if (cl != null) {
            httpResponse.setHeader("Content-Location", Onem2m.translateUriFromOnem2m(cl));
        }
        return true;
    }

    /**
     * Fills the stored original response with the data from Onem2m response.
     * @param onem2mResponse The Onem2m response primitive
     * @return
     */
    @Override
    public boolean setFromResponsePrimitive(ResponsePrimitive onem2mResponse) {
        return fromOnem2mResponseToHttp(onem2mResponse, this.httpResponse);
    }
}

