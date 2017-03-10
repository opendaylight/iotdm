/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing;

import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.Onem2mCoapClient;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;

public class Onem2mCoapRouterRequest extends Onem2mProtocolTxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapRouterRequest.class);

    protected final RequestPrimitive request;
    protected final String nextHopUrl;
    protected final Onem2mCoapClient client;
    protected final boolean secureConnection;
    //coap request/response
    private Response coapResponse;
    private Request coapRequest;

    protected ResponsePrimitive response = null;

    public Onem2mCoapRouterRequest(@Nonnull final RequestPrimitive request,
                                   @Nonnull final String nextHopUrl,
                                   @Nonnull final Onem2mCoapClient client,
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

    @Override
    protected boolean translateRequestFromOnem2m() {
        response = new ResponsePrimitive();
        response.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
                response.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));

        // Translate the request from Onem2m to CoAP
        coapRequest = createCoapRequest(request, nextHopUrl);
        if (null == coapRequest) {
            // return response primitive with BAD_REQUEST error
            response.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Forwarding of request failed");
            return false;
        }
        return true;
    }

    @Override
    protected boolean sendRequest() {
        // Send the request and wait for response
        try {
            client.send(coapRequest);
            coapResponse = coapRequest.waitForResponse();
            if (null == coapResponse) {
                LOG.error("Forwarding of request failed, no response received");
                response.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE,
                        "Forwarding failed, no response");
                return false;
            }
            return true;
        } catch (Exception e) {
            LOG.error("Forwarding of request failed: {}", e);
            response.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE, "Forwarding failed");
            return false;
        }
    }

    @Override
    protected boolean translateResponseToOnem2m() {
        // translate the response to onem2m response
        translateResponseToOnem2m(coapResponse);
        return true;
    }

    @Override
    protected void respondToOnem2mCore() {
        // nothing to do here
    }

    /**
     * Creates query string for the CoAP request
     * @param request Request primitive including primitive parameters
     * @return The coap query string
     */
    private String createRequestQueryString(RequestPrimitive request) {
        StringBuilder ret = new StringBuilder();
        String value = null;
        for (String param : Onem2m.queryStringParameters) {
            value = request.getPrimitive(param);
            if (null != value) {
                if (0 != ret.length()) {
                    ret.append("&");
                }
                ret.append(param).append("=").append(value);
            }
        }
        return ret.toString();
    }

    /**
     * Reads value of the onem2mParameter from the request primitive and
     * sets the value into respective CoAP option.
     * @param onem2mRequest
     * @param options
     * @param onem2mParameterName
     */
    private void setCoapOption(RequestPrimitive onem2mRequest, OptionSet options, String onem2mParameterName) {
        Onem2m.Onem2mCoapOptionDef optDef = Onem2m.getCoapOptionOnem2m(onem2mParameterName);
        if (null == optDef) {
            return;
        }

        if (optDef.valueIsString) {
            // value is string
            String value = onem2mRequest.getPrimitive(onem2mParameterName);
            if (null != value) {
                if (value.length() > optDef.max) {
                    LOG.error("{} parameter value ({}) is too long, max bytes: {}",
                            onem2mParameterName, value, optDef.max);
                    return;
                }
                options.addOption(new Option(optDef.coapId, value));
            }
        } else {
            // value is integer
            if (null == onem2mRequest.getPrimitive(onem2mParameterName)) {
                return;
            }

            int value = Integer.valueOf(onem2mRequest.getPrimitive(onem2mParameterName));
            if (0 > value || optDef.max < value) {
                LOG.error("Invalid value ({}), for option ()", value, onem2mParameterName);
                return;
            }

            options.addOption(new Option(optDef.coapId, value));
        }
    }

    /**
     * Translates the Onem2m request primitive into CoAP request.
     * @param onem2mRequest
     * @param nextHopUrl
     * @return
     */
    private Request createCoapRequest(RequestPrimitive onem2mRequest, String nextHopUrl) {
        Request request = null;

        Integer operation = onem2mRequest.getPrimitiveOperation();
        if (-1 == operation) {
            LOG.error("Onem2mRequest without operation set");
            return null;
        }

        // create new CoAP request according to operation type
        switch (operation) {
            case Onem2m.Operation.CREATE:
            case Onem2m.Operation.NOTIFY:
                request = Request.newPost();
                break;
            case Onem2m.Operation.RETRIEVE:
                request = Request.newGet();
                break;
            case Onem2m.Operation.UPDATE:
                request = Request.newPut();
                break;
            case Onem2m.Operation.DELETE:
                request = Request.newDelete();
                break;
            default:
                LOG.error("Invalid operation set in Onem2mRequest: {}", operation);
                return null;
        }

        // get next hop as URI
        URI uriNextHop = null;
        try {
            uriNextHop = new URI(nextHopUrl);
        } catch (URISyntaxException e) {
            LOG.error("Invalid next hop URL: {}, {}", nextHopUrl, e);
            return null;
        }
        String path = onem2mRequest.getPrimitiveTo();
        if (null == path) {
            LOG.error("Onem2m request without TO parameter");
            return null;
        }

        // get path to the resource in form used in CoAP
        path = Onem2m.translateUriFromOnem2m(path);

        // get query string
        String query = createRequestQueryString(onem2mRequest);

        // set all CoAP URI parts
        request.setURI(uriNextHop);
        OptionSet options = new OptionSet();
        options.setUriHost(uriNextHop.getHost());
        options.setUriPort(uriNextHop.getPort());
        options.setUriPath(path);
        if (null != query) {
            options.setUriQuery(query);
        }

        // set next options
        for (Onem2mPrimitive primitive : onem2mRequest.getPrimitivesList()) {
            setCoapOption(onem2mRequest, options, primitive.getName());
        }

        // set content if exists
        if (null != onem2mRequest.getPrimitiveContent()) {
            String format =  onem2mRequest.getPrimitiveContentFormat();
            if (null == format) {
                LOG.error("Onem2m request with content but without content format specified");
                return null;
            }

            // set content format according to operation type and Onem2m content format
            switch (operation) {
                case Onem2m.Operation.CREATE:
                case Onem2m.Operation.RETRIEVE:
                case Onem2m.Operation.UPDATE:
                case Onem2m.Operation.DELETE:
                    switch (format) {
                        case Onem2m.ContentFormat.JSON:
                            options.setContentFormat(Onem2m.CoapContentFormat.APP_VND_RES_JSON);
                            break;
                        case Onem2m.ContentFormat.XML:
                            options.setContentFormat(Onem2m.CoapContentFormat.APP_VND_RES_XML);
                            break;
                        default:
                            LOG.error("Unexpected content resource format: {}", format);
                            return null;
                    }
                    break;
                case Onem2m.Operation.NOTIFY:
                    switch (format) {
                        case Onem2m.ContentFormat.JSON:
                            options.setContentFormat(Onem2m.CoapContentFormat.APP_VND_NTFY_JSON);
                            break;
                        case Onem2m.ContentFormat.XML:
                            options.setContentFormat(Onem2m.CoapContentFormat.APP_VND_NTFY_XML);
                            break;
                        default:
                            LOG.error("Unexpected content notify format: {}", format);
                            return null;
                    }
                    break;
                default:
                    // this should never happen
                    LOG.error("Onem2m request processing internal error");
                    return null;
            }

            // set the content
            request.setPayload(onem2mRequest.getPrimitiveContent());
        }

        // use the options
        request.setOptions(options);
        return request;
    }

    /**
     * Translates response from CoAP to Onem2m response primitive
     * @param coapResponse
     * @return
     */
    private boolean translateResponseToOnem2m(@Nonnull Response coapResponse) {
        response = new ResponsePrimitive();

        OptionSet options = coapResponse.getOptions();
        if (null == options) {
            LOG.error("Invalid CoAP response without options");
            response.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE,
                    "Invalid response received from next hop");
            return false;
        }

        // Walks all options and stores value according to the
        // definitions of the CoAP options
        for (Option opt : options.asSortedList()) {
            Onem2m.Onem2mCoapOptionDef optDef = Onem2m.getCoapOptionCoap(opt.getNumber());
            if (null == optDef) {
                continue;
            }

            if (optDef.valueIsString) {
                response.setPrimitive(optDef.onem2mId, opt.getStringValue());
            } else {
                response.setPrimitive(optDef.onem2mId, Integer.toString(opt.getIntegerValue()));
            }
        }

        // set content location if exists
        String contentLocation = options.getLocationPathString();
        if (null != contentLocation && (! contentLocation.isEmpty())) {
            response.setPrimitive(ResponsePrimitive.CONTENT_LOCATION,
                    Onem2m.translateUriToOnem2m(contentLocation));
        }

        // set content if exists
        String payload = coapResponse.getPayloadString();
        if (null != payload) {
            response.setPrimitive(ResponsePrimitive.CONTENT, payload);

            // set content format
            switch (options.getContentFormat()) {
                case Onem2m.CoapContentFormat.APP_VND_RES_JSON:
                    response.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE,
                            Onem2m.ContentType.APP_VND_RES_JSON);
                    response.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, Onem2m.ContentFormat.JSON);
                    break;
                case Onem2m.CoapContentFormat.APP_VND_RES_XML:
                    response.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE,
                            Onem2m.ContentType.APP_VND_RES_XML);
                    response.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, Onem2m.ContentFormat.XML);
                    break;
                default:
                    LOG.trace("Unsupported CoAP content format in response from next hop: {}",
                            options.getContentFormat());
                    break;
            }
        }

        return true;
    }
}
