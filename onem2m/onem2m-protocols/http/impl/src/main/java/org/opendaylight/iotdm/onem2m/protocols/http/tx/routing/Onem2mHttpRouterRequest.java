/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx.routing;

import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpSchemes;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxRequest;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Optional;

public class Onem2mHttpRouterRequest extends Onem2mProtocolTxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpRouterRequest.class);

    protected final RequestPrimitive request;
    protected final String nextHopUrl;
    protected final Onem2mHttpClient client;
    protected final boolean secureConnection;

    protected ContentExchange ex = null;
    protected ResponsePrimitive response = null;

    public Onem2mHttpRouterRequest(@Nonnull final RequestPrimitive request,
                                   @Nonnull final String nextHopUrl,
                                   @Nonnull final Onem2mHttpClient client,
                                   final boolean secureConnection) {
        this.request = request;
        this.nextHopUrl = nextHopUrl;
        this.client = client;
        this.secureConnection = secureConnection;
    }

    public ResponsePrimitive getResponse() {
        return response;
    }

    @Override
    protected boolean preprocessRequest() {
        // Nothing to do here
        return true;
    }

    /**
     * Creates query string for the HTTP request
     * @param request Request primitive including primitive parameters
     * @return The http query string
     */
    protected String createRequestQueryString(RequestPrimitive request) {
        StringBuilder ret = new StringBuilder();
        String value = null;

        for (String param : Onem2m.queryStringParameters) {
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
    protected String createRequestContentTypeString(RequestPrimitive request) {
        StringBuilder ret = new StringBuilder();
        String value = null;

        switch(request.getPrimitiveContentFormat()) {
            case Onem2m.ContentFormat.JSON:
                ret.append(Onem2m.ContentType.APP_VND_RES_JSON);
                break;
            case Onem2m.ContentFormat.XML:
                ret.append(Onem2m.ContentType.APP_VND_RES_XML);
                break;
            default:
                LOG.error("Unsupported content format: {}", request.getPrimitive(RequestPrimitive.CONTENT_FORMAT));
                return ret.toString();
        }

        for (String param : Onem2m.queryStringParameters) {
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
    protected ContentExchange createContentExchangeReq(RequestPrimitive request, String hostURL) {

        ContentExchange ex = new ContentExchange(true);

        try {
            // set method and url
            ex.setMethod(translateOperationToMethod(request.getPrimitiveOperation()));

            // set URL properly
            URL host = new URL(hostURL);
            String url = Onem2m.translateUriFromOnem2m(request.getPrimitiveTo());
            // add query string
            url += createRequestQueryString(request);

            ex.setAddress(new Address(host.getHost(), host.getPort()));
            ex.setURI(url);

            // set headers
            //TODO add all headers
            ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_ORIGIN, request.getPrimitiveFrom());
            ex.setRequestHeader("Host", hostURL);
            ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_RI, request.getPrimitiveRequestIdentifier());
            // ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_NM, request.getPrimitive(RequestPrimitive.NAME));
            ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_GID,
                    request.getPrimitive(RequestPrimitive.GROUP_REQUEST_IDENTIFIER));
            ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_OT,
                    request.getPrimitive(RequestPrimitive.ORIGINATING_TIMESTAMP));

            // set content
            if (null != request.getPrimitiveContent()) {
                ex.setRequestContentSource(new ByteArrayInputStream(request.getPrimitiveContent().getBytes()));
                ex.setRequestContentType(createRequestContentTypeString(request));
            }

            // use HTTPS scheme in case of secure connection
            if (this.secureConnection) {
                ex.setScheme(HttpSchemes.HTTPS);
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
    protected ResponsePrimitive createResponseFailed(RequestPrimitive request) {
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
    protected ResponsePrimitive createResponseUnreachable(RequestPrimitive request) {
        ResponsePrimitive response = new ResponsePrimitive();
        response.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                request.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));
        response.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE, "Not reachable");

        return response;
    }

    private String translateOperationToMethod(Integer operation) {
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

    @Override
    protected boolean translateRequestFromOnem2m() {
        ex = createContentExchangeReq(request, nextHopUrl);
        if(null == ex) {
            this.response = createResponseFailed(this.request);
            return false;
        }
        return true;
    }

    @Override
    protected boolean sendRequest() {
        try {
            client.send(ex);
            int state = HttpExchange.STATUS_START;
            try {
                state = ex.waitForDone();
            } catch (InterruptedException e) {
                LOG.error("Request forwarding interrupted: {}", e);
                this.response = createResponseFailed(request);
                return false;
            }

            switch(state) {
                case HttpExchange.STATUS_COMPLETED:
                    break;

                case HttpExchange.STATUS_EXCEPTED:
                case HttpExchange.STATUS_EXPIRED:
                    LOG.trace("Failed to forward request, exchange state: {}", state);
                    this.response = createResponseUnreachable(request);
                    return false;

                default:
                    LOG.error("Unexpected request send result: {}", state);
                    this.response = createResponseFailed(request);
                    return false;

            }
        } catch (IOException e) {
            LOG.error("Failed to send request to nextHop: {}", nextHopUrl);
            this.response = createResponseFailed(request);
            return false;
        }

        // exchange passed
        return true;
    }

    @Override
    protected boolean translateResponseToOnem2m() {
        response = new ResponsePrimitive();
        HttpFields fields = ex.getResponseFields();
        String value = null;

        // Set values from HTTP header
        if (null != fields) {
            value = fields.getStringField(Onem2m.HttpHeaders.X_M2M_RI);
            if (null != value) {
                response.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER, value);
            } else {
                LOG.trace("Response without X_M2M_RI header");
                this.response = createResponseUnreachable(request);
                return false;
            }

            value = fields.getStringField(Onem2m.HttpHeaders.X_M2M_RSC);
            if (null != value) {
                response.setPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE, value);
            } else {
                LOG.trace("Response without X_M2M_RSC header");
                this.response = createResponseUnreachable(request);
                return false;
            }

            String contentType = fields.getStringField("Content-Type");
            if (contentType != null) {
                response.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE, contentType);
                Optional<String> maybeResolvedFormat = Onem2m.resolveContentFormat(contentType.toLowerCase());
                if(maybeResolvedFormat.isPresent()) {
                    response.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, maybeResolvedFormat.get());
                }
            }

            value = fields.getStringField("Content-Location");
            if (value != null) {
                response.setPrimitive(ResponsePrimitive.CONTENT_LOCATION, Onem2m.translateUriToOnem2m(value));
            }

            // TODO add next headers if needed
        }

        // set response content if exists
        try {
            value = ex.getResponseContent();
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to get response content: {}", e);
            response = createResponseUnreachable(request);
            return false;
        }

        if (null != value) {
            response.setPrimitive(ResponsePrimitive.CONTENT, value);
        }

        return true;
    }

    @Override
    protected void respondToOnem2mCore() {
        // nothing to do here
        return;
    }
}
