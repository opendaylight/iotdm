/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.rx;

import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.plugins.IotDMPluginHttpRequest;
import org.opendaylight.iotdm.onem2m.plugins.IotDMPluginHttpResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Implements complete handling logic for HTTP RxRequests.
 * The handling is divided into few methods according to extended
 * RxRequest class.
 */
public class Onem2mHttpRxRequest extends Onem2mProtocolRxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpRxRequest.class);

    /* Let's keep this data protected so they can be accessed by
     * Child classes
     */
    protected final IotDMPluginHttpRequest request;
    protected final IotDMPluginHttpResponse response;
    protected final HttpServletRequest httpRequest;
    protected final HttpServletResponse httpResponse;
    protected final Onem2mService onem2mService;
    protected final SecurityLevel securityLevel;

    protected Onem2mRequestPrimitiveClientBuilder clientBuilder = null;
    protected ResponsePrimitive onem2mResponse = null;

    public Onem2mHttpRxRequest(@Nonnull final IotDMPluginHttpRequest request,
                               @Nonnull final IotDMPluginHttpResponse response,
                               @Nonnull final Onem2mService onem2mService,
                               final SecurityLevel securityLevel) {

        this.request = request;
        this.response = response;
        this.httpRequest = request.getHttpRequest();
        this.httpResponse = response.getHttpResponse();
        this.onem2mService = onem2mService;
        this.securityLevel = securityLevel;
    }

    @Override
    protected boolean preprocessRequest() {
        return true;
    }

    public static String resolveContentFormat(String contentType) {
        if (contentType.contains("json")) {
            return Onem2m.ContentFormat.JSON;
        } else if (contentType.contains("xml")) {
            return Onem2m.ContentFormat.XML;
        }
        return null;
    }

    private String parseContentTypeForResourceType(String contentType) {

        String split[] = contentType.trim().split(";");
        if (split.length != 2) {
            return null;
        }
        return split[1];
    }

    protected void prepareErrorResponse(String message, int statusCode) {
        httpResponse.setStatus(statusCode);
        try {
            httpResponse.getWriter().println(message);
            httpResponse.setContentType("text/json;charset=utf-8");
        } catch (IOException e) {
            LOG.error("Failed to write error message: {}", message);
        }
        Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
    }

    @Override
    protected boolean translateRequestToOnem2m() {
        clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
        String headerValue;
        clientBuilder.setProtocol(Onem2m.Protocol.HTTP);

        Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS);

        String contentType = httpRequest.getContentType();
        if (contentType == null) contentType = "json";
        contentType = contentType.toLowerCase();
        String contentFormat = resolveContentFormat(contentType);

        if (null != contentFormat) {
            clientBuilder.setContentFormat(contentFormat);
        } else {
            prepareErrorResponse("Unsupported media type: " + contentType, HttpServletResponse.SC_NOT_ACCEPTABLE);
            return false;
        }

        clientBuilder.setTo(Onem2m.translateUriToOnem2m(httpRequest.getRequestURI()));

        // pull fields out of the headers
        headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_ORIGIN);
        if (headerValue != null) {
            clientBuilder.setFrom(headerValue);
        }
        headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_RI);
        if (headerValue != null) {
            clientBuilder.setRequestIdentifier(headerValue);
        }
        headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_NM);
        if (headerValue != null) {
            clientBuilder.setName(headerValue);
        }
        headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_GID);
        if (headerValue != null) {
            clientBuilder.setGroupRequestIdentifier(headerValue);
        }
        headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_RTU);
        if (headerValue != null) {
            clientBuilder.setResponseType(headerValue);
        }
        headerValue = httpRequest.getHeader(Onem2m.HttpHeaders.X_M2M_OT);
        if (headerValue != null) {
            clientBuilder.setOriginatingTimestamp(headerValue);
        }

        // the contentType string can have ty=val attached to it so we should handle this case
        Boolean resourceTypePresent = false;
        String contentTypeResourceString = parseContentTypeForResourceType(contentType);
        if (contentTypeResourceString != null) {
            resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(contentTypeResourceString);
        }
        String method = httpRequest.getMethod().toLowerCase();
        // look in query string if didnt find it in contentType header
        if (!resourceTypePresent) {
            resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(httpRequest.getQueryString());
        } else {
            clientBuilder.parseQueryStringIntoPrimitives(httpRequest.getQueryString());
        }
        if (resourceTypePresent && !method.contentEquals("post")) {
            prepareErrorResponse("Specifying resource type not permitted.", HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
        String cn = request.getPayLoad();
        if (cn != null && !cn.contentEquals("")) {
            clientBuilder.setPrimitiveContent(cn);
        }

        switch (method) {
            case "get":
                clientBuilder.setOperationRetrieve();
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_RETRIEVE);
                break;
            case "post":
                if (resourceTypePresent) {
                    clientBuilder.setOperationCreate();
                    Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_CREATE);
                } else {
                    clientBuilder.setOperationNotify();
                    Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_NOTIFY);
                }
                break;
            case "put":
                clientBuilder.setOperationUpdate();
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_UPDATE);
                break;
            case "delete":
                clientBuilder.setOperationDelete();
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_DELETE);
                break;
            default:
                prepareErrorResponse("Unsupported method type: " + method, HttpServletResponse.SC_NOT_IMPLEMENTED);
                return false;
        }

        return true;
    }

    @Override
    protected boolean processRequest() {
        // invoke the service request
        Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
        onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService, securityLevel);
        return true;
    }

    private int mapCoreResponseToHttpResponse(HttpServletResponse httpResponse, String rscString) {

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

    @Override
    protected boolean translateResponseFromOnem2m() {
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

            try {
                httpResponse.getWriter().println(content);
            } catch (IOException e) {
                prepareErrorResponse("Failed to write response content", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return false;
            }
        } else {
            httpResponse.setStatus(httpRSC);
        }
        if (rscString.charAt(0) =='2') {
            Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_OK);
        } else {
            Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
        }

        String ct = onem2mResponse.getPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE);
        if (ct != null) {
            httpResponse.setContentType(ct);
        }
        String cl = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT_LOCATION);
        if (cl != null) {
            httpResponse.setHeader("Content-Location", Onem2m.translateUriFromOnem2m(cl));
        }
        return true;
    }

    @Override
    protected void respond() {
        // The httpResponse has already been filled by response data
    }
}
