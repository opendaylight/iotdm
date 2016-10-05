/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins.channels.http;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Implementation of class wrapping original HTTP response which is send
 * back to the client.
 */
public class IotdmPluginHttpResponse implements IotdmPluginResponse {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginHttpResponse.class);

    protected final HttpServletResponse httpResponse; // The original HTTP response

    public IotdmPluginHttpResponse(HttpServletResponse httpResponse) {
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
        } catch (java.io.IOException e) {
            LOG.error("Failed to write HTTP response payload: {}", e);
        }
    }

    @Override
    public void setContentType(String contentType) {
        if (null != contentType) {
            this.httpResponse.setContentType(contentType);
        }
    }

    @Override
    public void setHeader(String name, String value) {
        if (null != name && null != value) {
            this.httpResponse.setHeader(name, value);
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (null != name && null != value) {
            this.httpResponse.addHeader(name, value);
        }
    }

    protected static int mapCoreResponseToHttpResponse(HttpServletResponse httpResponse, String rscString) {

        httpResponse.setHeader(Onem2m.HttpHeaders.X_M2M_RSC, rscString);
        switch (rscString) {
            case Onem2m.ResponseStatusCode.OK:
                return HttpServletResponse.SC_OK;
            case Onem2m.ResponseStatusCode.CREATED:
                return HttpServletResponse.SC_CREATED;
            case Onem2m.ResponseStatusCode.CHANGED:
                return HttpServletResponse.SC_OK;
            case Onem2m.ResponseStatusCode.DELETED:
                return HttpServletResponse.SC_OK;

            case Onem2m.ResponseStatusCode.NOT_FOUND:
                return HttpServletResponse.SC_NOT_FOUND;
            case Onem2m.ResponseStatusCode.ACCESS_DENIED:
                return HttpServletResponse.SC_FORBIDDEN;
            case Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED:
                return HttpServletResponse.SC_METHOD_NOT_ALLOWED;
            case Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE:
                return HttpServletResponse.SC_BAD_REQUEST;
            case Onem2m.ResponseStatusCode.CONFLICT:
                return HttpServletResponse.SC_CONFLICT;

            case Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR:
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            case Onem2m.ResponseStatusCode.NOT_IMPLEMENTED:
                return HttpServletResponse.SC_NOT_IMPLEMENTED;
            case Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE:
                return HttpServletResponse.SC_NOT_FOUND;
            case Onem2m.ResponseStatusCode.ALREADY_EXISTS:
                return HttpServletResponse.SC_FORBIDDEN;
            case Onem2m.ResponseStatusCode.TARGET_NOT_SUBSCRIBABLE:
                return HttpServletResponse.SC_FORBIDDEN;
            case Onem2m.ResponseStatusCode.NON_BLOCKING_REQUEST_NOT_SUPPORTED:
                return HttpServletResponse.SC_NOT_IMPLEMENTED;

            case Onem2m.ResponseStatusCode.INVALID_ARGUMENTS:
                return HttpServletResponse.SC_BAD_REQUEST;
            case Onem2m.ResponseStatusCode.INSUFFICIENT_ARGUMENTS:
                return HttpServletResponse.SC_BAD_REQUEST;
        }
        return HttpServletResponse.SC_BAD_REQUEST;
    }

    /**
     * Sets response parameters describing error.
     * @param httpResponse The original HTTP response
     * @param message Error message string
     * @param statusCode Response status code
     */
    public static void prepareErrorResponse(HttpServletResponse httpResponse,
                                            String message, int statusCode) {
        httpResponse.setStatus(statusCode);
        try {
            httpResponse.getWriter().println(message);
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

        int httpRSC = mapCoreResponseToHttpResponse(httpResponse, rscString);
        if (content != null) {
            httpResponse.setStatus(httpRSC);

            String ct = onem2mResponse.getPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE);
            if (ct != null) {
                httpResponse.setContentType(ct);
            }

            try {
                httpResponse.getWriter().println(content);
            } catch (IOException e) {
                prepareErrorResponse(httpResponse,
                                     "Failed to write response content", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return false;
            }
        } else {
            httpResponse.setStatus(httpRSC);
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

