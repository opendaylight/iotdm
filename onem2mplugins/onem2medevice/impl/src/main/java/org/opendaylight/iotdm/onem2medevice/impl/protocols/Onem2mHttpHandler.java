/*
 * Copyright © 2016 Cisco Systems, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2medevice.impl.protocols;

import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.plugins.*;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.IotdmPluginHttpRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.IotdmPluginHttpResponse;
import org.opendaylight.iotdm.onem2medevice.impl.utils.Onem2mXmlUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author jkosmel
 * TODO: temporary copied from Onem2mHttpProvider
 */
public class Onem2mHttpHandler {
    private Onem2mService onem2mService;

    public Onem2mHttpHandler(Onem2mService om2mService) {
        onem2mService = om2mService;
    }

    public void handle(IotdmPluginRequest request, IotdmPluginResponse response){
        HttpServletRequest httpRequest = ((IotdmPluginHttpRequest)request).getOriginalRequest();
        HttpServletResponse httpResponse = ((IotdmPluginHttpResponse)response).getHttpResponse();

        Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
        String headerValue;

        clientBuilder.setProtocol(Onem2m.Protocol.HTTP);

        Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS);

        String contentType = request.getContentType(); //edevice: retrieve from IotDMPluginRequest to not have to change core
        if (contentType == null) contentType = "json";
        contentType = contentType.toLowerCase();
        if (contentType.contains("json")) {
            clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
        } else if (contentType.contains("xml")) {
            clientBuilder.setContentFormat(Onem2m.ContentFormat.XML);
        } else {
            httpResponse.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            try {
                httpResponse.getWriter().println("Unsupported media type: " + contentType);
            } catch (IOException e) {
                e.printStackTrace();
            }
            httpResponse.setContentType("text/json;charset=utf-8");

            Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
            return;
        }

        clientBuilder.setTo(httpRequest.getRequestURI());

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
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try {
                httpResponse.getWriter().println("Specifying resource type not permitted.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
            return;
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
                httpResponse.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
                try {
                    httpResponse.getWriter().println("Unsupported method type: " + method);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                httpResponse.setContentType("text/json;charset=utf-8");
                //baseRequest.setHandled(true);
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
                return;
        }

        // invoke the service request
        Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
        ResponsePrimitive onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService);

        if (httpRequest.getHeader("Accept").contains("xml")) { //TODO: temporary solution for edevice - convert payload to xml
            onem2mResponse.setPrimitive(
                    ResponsePrimitive.CONTENT,
                    Onem2mXmlUtils.jsonResponseToXml(
                            onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT),
                            onem2mRequest.getPrimitive(RequestPrimitive.RESULT_CONTENT)
                    )
            );
            onem2mResponse.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE, "text/xml");
        }

        // now place the fields from the onem2m result response back in the http fields, and send
        try {
            sendHttpResponseFromOnem2mResponse(httpResponse, onem2mResponse);
        } catch (IOException e) {
            e.printStackTrace();
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void sendHttpResponseFromOnem2mResponse(HttpServletResponse httpResponse,
                                                    ResponsePrimitive onem2mResponse) throws IOException {
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
            httpResponse.getWriter().println(content);
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

    private String parseContentTypeForResourceType(String contentType) {
        String split[] = contentType.trim().split(";");
        if (split.length != 2) {
            return null;
        }
        return split[1];
    }
}
