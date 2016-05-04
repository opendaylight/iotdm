/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.io.ByteArrayBuffer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterPlugin;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierPlugin;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpProvider implements Onem2mNotifierPlugin, Onem2mRouterPlugin,
                                           BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpProvider.class);
    protected Onem2mService onem2mService;
    private Server server;
    private final int PORT = 8282;
    private HttpClient client;
    private final String PLUGIN_NAME = "http";

    @Override
    public void onSessionInitiated(ProviderContext session) {
        onem2mService = session.getRpcService(Onem2mService.class);
        Onem2mNotifierService.getInstance().pluginRegistration(this);
        Onem2mRouterService.getInstance().pluginRegistration(this);

        try {
            server.start();
            client.start();
        } catch (Exception e) {
            LOG.info("Exception: {}", e.toString());
        }
        LOG.info("Onem2mHttpProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        server.stop();
        client.stop();
        LOG.info("Onem2mHttpProvider Closed");
    }

    public Onem2mHttpProvider() {
        server = new Server(PORT);
        server.setHandler(new MyHandler());
        client = new HttpClient();

    }

    private String resolveContentFormat(String contentType) {
        if (contentType.contains("json")) {
            return Onem2m.ContentFormat.JSON;
        } else if (contentType.contains("xml")) {
            return Onem2m.ContentFormat.XML;
        }
        return null;
    }

    private String translateUriToOnem2m(String httpUri) {

        if (-1 != httpUri.indexOf("/~/")) {
            return httpUri.replaceFirst("/~/", "/");
        }

        if (-1 != httpUri.indexOf("/_/")) {
            return httpUri.replaceFirst("/_/", "//");
        }

        if (httpUri.startsWith("/")) {
            return httpUri.substring(1);
        }

        return httpUri;
    }

    private String translateUriToHttp(String onem2mUri) {
        if (onem2mUri.startsWith("//")) {
            return onem2mUri.replaceFirst("//", "/_/");
        }

        if (onem2mUri.startsWith("/")) {
            return onem2mUri.replaceFirst("/", "/~/");
        }

        return "/" + onem2mUri;
    }

    public class MyHandler extends AbstractHandler {

        /**
         * The handler for the HTTP requesst
         * @param target target
         * @param baseRequest  request
         * @param httpRequest  request
         * @param httpResponse  response
         * @throws IOException
         * @throws ServletException
         */
        @Override
        public void handle(String target, Request baseRequest,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse) throws IOException, ServletException {

            Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
            String headerValue;

            clientBuilder.setProtocol(Onem2m.Protocol.HTTP);

            Onem2mStats.getInstance().endpointInc(baseRequest.getRemoteAddr());
            Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS);

            String contentType = httpRequest.getContentType();
            if (contentType == null) contentType = "json";
            contentType = contentType.toLowerCase();
            String contentFormat = resolveContentFormat(contentType);

            if (null != contentFormat) {
                clientBuilder.setContentFormat(contentFormat);
            } else {
                httpResponse.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                httpResponse.getWriter().println("Unsupported media type: " + contentType);
                httpResponse.setContentType("text/json;charset=utf-8");
                baseRequest.setHandled(true);
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
                return;
            }

            clientBuilder.setTo(translateUriToOnem2m(httpRequest.getRequestURI()));

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
                httpResponse.getWriter().println("Specifying resource type not permitted.");
                Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
                return;
            }

            // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
            String cn = IOUtils.toString(baseRequest.getInputStream()).trim();
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
                    httpResponse.getWriter().println("Unsupported method type: " + method);
                    httpResponse.setContentType("text/json;charset=utf-8");
                    baseRequest.setHandled(true);
                    Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
                    return;
            }

            // invoke the service request
            Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);

            // now place the fields from the onem2m result response back in the http fields, and send
            sendHttpResponseFromOnem2mResponse(httpResponse, onem2mResponse);

            httpResponse.addHeader("Access-Control-Allow-Origin", "*");
            httpResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");

            baseRequest.setHandled(true);
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
            String cl = onem2mResponse.getPrimitive(ResponsePrimitive.HTTP_CONTENT_LOCATION);
            if (cl != null) {
                httpResponse.setHeader("Content-Location", cl);
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
    }

    // implement the Onem2mNotifierPlugin interface
    @Override
    public String getNotifierPluginName() {
        return this.PLUGIN_NAME;
    }

    /**
     * HTTP notifications will be set out to subscribers interested in resources from the tree where they have have hung
     * onem2m subscription resources
     * @param url where do i send this onem2m notify message
     * @param payload contents of the notification
     */
    @Override
    public void sendNotification(String url, String payload) {
        ContentExchange ex = new ContentExchange();
        ex.setURL(url);
        ex.setRequestContentSource(new ByteArrayInputStream(payload.getBytes()));
        ex.setRequestContentType(Onem2m.ContentType.APP_VND_NTFY_JSON);
        Integer cl = payload != null ?  payload.length() : 0;
        ex.setRequestHeader("Content-Length", cl.toString());
        ex.setMethod("post");
        LOG.debug("HTTP: Send notification uri: {}, payload: {}:", url, payload);
        try {
            client.send(ex);
        } catch (IOException e) {
            LOG.error("Dropping notification: uri: {}, payload: {}", url, payload);
        }
    }

    private String parseContentTypeForResourceType(String contentType) {

        String split[] = contentType.trim().split(";");
        if (split.length != 2) {
            return null;
        }
        return split[1];
    }

    @Override
    public String getRouterPluginName() { return this.PLUGIN_NAME; }

    /**
     * Implements method of Onem2mRouterPlugin, sends request to the host specified by nextHopUrl.
     * @param request The request to be sent.
     * @param nextHopUrl The URL of the next hop, is used as value of Host header.
     * @return Response to he request is returned.
     */
    @Override
    public ResponsePrimitive sendRequestBlocking(RequestPrimitive request, String nextHopUrl) {

        ContentExchange ex = createContentExchangeReq(request, nextHopUrl);
        try {
            client.send(ex);
            int state = HttpExchange.STATUS_START;
            try {
                state = ex.waitForDone();
            } catch (InterruptedException e) {
                LOG.error("Request forwarding interrupted: {}", e);
                return createResponseFailed(request);
            }

            switch(state) {
                case HttpExchange.STATUS_COMPLETED:
                    break;

                case HttpExchange.STATUS_EXCEPTED:
                case HttpExchange.STATUS_EXPIRED:
                    LOG.trace("Failed to forward request, exchange state: {}", state);
                    return createResponseUnreachable(request);

                default:
                    LOG.error("Unexpected request send result: {}", state);
                    return createResponseFailed(request);

            }
        } catch (IOException e) {
            LOG.error("Failed to send request to nextHop: {}", nextHopUrl);
            return createResponseFailed(request);
        }

        // Translate the HTTP response to Onem2m response primitive and return
        return createResponseFromHttp(ex, request);
    }

    /**
     * Creates query string for the HTTP request
     * @param request Request primitive including primitive parameters
     * @return The http query string
     */
    private String createRequestQueryString(RequestPrimitive request) {
        StringBuilder ret = new StringBuilder();
        String value = null;

        for (String param : RequestPrimitive.primitiveQueryStringParameters) {
            value = request.getPrimitive(param);
            if (null != value) {
                if (0 == ret.length()) {
                    ret.append("?");
                } else {
                    ret.append("&");
                }
                ret.append(param).append("=").append(value);
            }
        }

        return ret.toString();
    }

    /**
     * Cretes Content-Type header value string.
     * @param request Request primitive including primitive parameters
     * @return value of the Content-Type header
     */
    private String createRequestContentTypeString(RequestPrimitive request) {
        StringBuilder ret = new StringBuilder();
        String value = null;

        switch(request.getPrimitive(RequestPrimitive.CONTENT_FORMAT)) {
            case Onem2m.ContentFormat.JSON:
                ret.append(Onem2m.ContentType.APP_VND_REQ_JSON);
                break;
            case Onem2m.ContentFormat.XML:
                ret.append(Onem2m.ContentType.APP_VND_REQ_XML);
                break;
            default:
                LOG.error("Unsupported content format: {}", request.getPrimitive(RequestPrimitive.CONTENT_FORMAT));
                return ret.toString();
        }

        for (String param : RequestPrimitive.primitiveQueryStringParameters) {
            value = request.getPrimitive(param);
            if (null != value) {
                ret.append(";").append(param).append("=").append(value);
            }
        }

        return ret.toString();
    }

    /**
     * Prepares instance of ContentExchange to be used for sending the request.
     * @param request The Onem2m request
     * @param hostURL The URL of the destination host
     * @return Prepared ContentExchange instance
     */
    private ContentExchange createContentExchangeReq(RequestPrimitive request, String hostURL) {

        ContentExchange ex = new ContentExchange(true);

        try {
            // set method and url
            ex.setMethod(translateOperationToMethod(request.getPrimitive(RequestPrimitive.OPERATION)));

            // set URL properly
            URL host = new URL(hostURL);
            String url = translateUriToHttp(request.getPrimitive(RequestPrimitive.TO));
            // add query string
            url += createRequestQueryString(request);

            ex.setAddress(new Address(host.getHost(), host.getPort()));
            ex.setURI(url);

            // set headers
            //TODO add all headers
            ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_ORIGIN, request.getPrimitive(RequestPrimitive.FROM));
            ex.setRequestHeader("Host", hostURL);
            ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_RI, request.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
            // ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_NM, request.getPrimitive(RequestPrimitive.NAME));
            ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_GID,
                    request.getPrimitive(RequestPrimitive.GROUP_REQUEST_IDENTIFIER));
            ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_OT,
                    request.getPrimitive(RequestPrimitive.ORIGINATING_TIMESTAMP));

            // set content
            if (null != request.getPrimitive(RequestPrimitive.CONTENT)) {
                ex.setRequestContentSource(new ByteArrayInputStream(
                        request.getPrimitive(RequestPrimitive.CONTENT).getBytes()));
                ex.setRequestContentType(createRequestContentTypeString(request));
            }
        } catch (Exception e) {
            LOG.error("Failed to create HTTP request from Onem2mRequest: {}", e);
            return null;
        }

        return ex;
    }

    /**
     * Creates Onem2m response in case of internal error
     * @param request Onem2m request
     * @return Onem2m response with error status code
     */
    private ResponsePrimitive createResponseFailed(RequestPrimitive request) {
        ResponsePrimitive response = new ResponsePrimitive();
        response.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                request.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
        response.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Forwarding of HTTP request failed");

        return response;
    }

    /**
     * Creates Onem2m response in case of unreachable target
     * @param request Onem2m request
     * @return Onem2m response with error status code
     */
    private ResponsePrimitive createResponseUnreachable(RequestPrimitive request) {
        ResponsePrimitive response = new ResponsePrimitive();
        response.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                request.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
        response.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE, "Not reachable");

        return response;
    }

    /**
     * Translates HTTP response to Onem2m response
     * @param ex The instance of ContentExchange including received response
     * @param request The original Onem2m request. Is used to create response
     *                with error status code in case of unsuccessful
     *                translation of the received HTTP response
     * @return Onem2m response
     */
    private ResponsePrimitive createResponseFromHttp(ContentExchange ex, RequestPrimitive request) {
        ResponsePrimitive response = new ResponsePrimitive();
        HttpFields fields = ex.getResponseFields();
        String value = null;

        // Set values from HTTP header
        if (null != fields) {
            value = fields.getStringField(Onem2m.HttpHeaders.X_M2M_RI);
            if (null != value) {
                response.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER, value);
            } else {
                LOG.trace("Response without X_M2M_RI header");
                return createResponseUnreachable(request);
            }

            value = fields.getStringField(Onem2m.HttpHeaders.X_M2M_RSC);
            if (null != value) {
                response.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, value);
            } else {
                LOG.trace("Response without X_M2M_RSC header");
                return createResponseUnreachable(request);
            }

            String contentType = fields.getStringField("Content-Type");
            if (contentType != null) {
                value = contentType.toLowerCase();
                value = resolveContentFormat(value);
            }

            if (null != value) {
                response.setPrimitive(RequestPrimitive.CONTENT_FORMAT, value);
            }

            if (contentType != null) {
                response.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE, contentType);
            }

            value = fields.getStringField("Content-Location");
            if (value != null) {
                response.setPrimitive(ResponsePrimitive.HTTP_CONTENT_LOCATION, value);
            }

            // TODO add next headers if needed
        }

        // set response content if exists
        try {
            value = ex.getResponseContent();
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to get response content: {}", e);
            return createResponseUnreachable(request);
        }

        if (null != value) {
            response.setPrimitive(ResponsePrimitive.CONTENT, value);
        }

        return response;
    }

    private String translateOperationToMethod(String operation) {
        switch (operation) {
            case Onem2m.Operation.CREATE:
            case Onem2m.Operation.NOTIFY:
                return "post";
            case Onem2m.Operation.RETRIEVE:
                return "get";
            case Onem2m.Operation.DELETE:
                return "delete";
            case Onem2m.Operation.UPDATE:
                return "put";
            default:
                throw new IllegalArgumentException("Operation " + operation + " not supported by HTTP plugin");
        }
    }
}
