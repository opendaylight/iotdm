/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.*;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterPlugin;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierPlugin;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;

public class Onem2mCoapProvider extends CoapServer
                                implements Onem2mNotifierPlugin, Onem2mRouterPlugin,
                                           BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapProvider.class);
    private static final String PLUGINNAME = "coap";
    protected Onem2mService onem2mService;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        onem2mService = session.getRpcService(Onem2mService.class);
        Onem2mNotifierService.getInstance().pluginRegistration(this);
        Onem2mRouterService.getInstance().pluginRegistration(this);

        start();
        LOG.info("Onem2mCoapProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mCoapProvider Closed");
    }

    /**
     * Intercept the coap URL query.
     */
    @Override
    public Resource createRoot() {
        return new RootResource();
    }

    private class RootResource extends CoapResource {
        public RootResource() {
            super("OpenDaylight OneM2M CoAP Server");
        }

        @Override
        public Resource getChild(String name) {
            return this;
        }

        /**
         * The handler for the CoAP request
         * @param exchange coap parameters
         */
        @Override
        public void handleRequest(final Exchange exchange) {
            CoAP.Code code = exchange.getRequest().getCode();
            CoapExchange coapExchange = new CoapExchange(exchange, this);
            OptionSet options = coapExchange.advanced().getRequest().getOptions();

            // onem2m needs type = CON, ACK, RST - see binding spec
            //if (exchange.getRequest().getType() != CoAP.Type.CON) {
            //   coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Invalid CoAP type:" + exchange.getRequest().getType());
            //   return;
            //}
            Onem2mRequestPrimitiveClientBuilder clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
            String optionValue;

            clientBuilder.setProtocol(Onem2m.Protocol.COAP);

            Onem2mStats.getInstance().endpointInc(coapExchange.getSourceAddress().toString());
            Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS);

            if (options.getContentFormat() == MediaTypeRegistry.APPLICATION_JSON) {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
            } else if (options.getContentFormat() == MediaTypeRegistry.APPLICATION_XML) {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.XML);
            } else {
                clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);

                //coapExchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Unknown media type: " +
                //        options.getContentFormat());
                //Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);

                //return;
            }

            // Process all options
            boolean resourceTypePresent = false;
            String resourceTypeOption = null;
            String resourceTypeQuerry = null;
            for (Option opt : options.asSortedList()) {
                switch (opt.getNumber()) {
                    case Onem2m.CoapOption.ONEM2M_FR:
                        clientBuilder.setFrom(opt.getStringValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_RQI:
                        clientBuilder.setRequestIdentifier(opt.getStringValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_NM:
                        clientBuilder.setName(opt.getStringValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_OT:
                        clientBuilder.setOriginatingTimestamp(opt.getStringValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_RQET:
                        clientBuilder.setRequestExpirationTimestamp(opt.getStringValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_RSET:
                        clientBuilder.setResultExpirationTimestamp(opt.getStringValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_OET:
                        clientBuilder.setOperationExecutionTime(opt.getStringValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_EC:
                        //clientBuilder.setEventCategory(opt.getIntegerValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_GID:
                        clientBuilder.setGroupRequestIdentifier(opt.getStringValue());
                        break;
                    case Onem2m.CoapOption.ONEM2M_TY:
                        resourceTypeOption = String.valueOf(opt.getIntegerValue());
                        break;
                    default:
                        LOG.error("Unsupported CoAP option: {}", opt.getNumber());
                        break;
                }
            }

            // according to the spec, the uri query string can contain in short form, the
            // resourceType, responseType, result persistence,  Delivery Aggregation, Result Content,
            // M3 Boolean
            resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(options.getUriQueryString());
            if (resourceTypePresent && (null != resourceTypeOption)) {
                // Resource type is set in query string and in the option as well,
                // verify if values are equal
                resourceTypeQuerry = clientBuilder.getPrimitiveValue(RequestPrimitive.RESOURCE_TYPE);
                if (! resourceTypeOption.equals(resourceTypeQuerry)) {
                    LOG.error("Invalid request received, resource type set in option ({}) and query ({}) mismatch",
                              resourceTypeOption, resourceTypeQuerry);
                    coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST,
                                         "Resource type in option and query string mismatch");
                    Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
                    return;
                }
            } else if (!resourceTypePresent && (null != resourceTypeOption)) {
                // resource type is not present in query string, use the value from option
                clientBuilder.setResourceType(resourceTypeOption);
                resourceTypePresent = true;
            }

            if (resourceTypePresent && code != CoAP.Code.POST) {
                coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Specifying resource type not permitted.");
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
                return;
            }

            // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
            String cn = coapExchange.getRequestText().trim();
            if (cn != null && !cn.contentEquals("")) {
                clientBuilder.setPrimitiveContent(cn);
            }

            switch (code) {
                case GET:
                    clientBuilder.setOperationRetrieve();
                    Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_RETRIEVE);
                    break;

                case POST:
                    if (resourceTypePresent) {
                        clientBuilder.setOperationCreate();
                        Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_CREATE);
                    } else {
                        clientBuilder.setOperationNotify();
                        Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_NOTIFY);
                    }
                    break;

                case PUT:
                    clientBuilder.setOperationUpdate();
                    Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_UPDATE);
                    break;

                case DELETE:
                    clientBuilder.setOperationDelete();
                    Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_DELETE);
                    break;

                default:
                    coapExchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Unknown code: " + code);
                    Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
                    return;
            }

            clientBuilder.setTo(Onem2m.translateUriToOnem2m(options.getUriPathString())); // To/TargetURI
            // M3 clientBuilder.setTo(options.getUriPathString()); // To/TargetURI // M3

            Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);

            // now place the fields from the onem2m result response back in the coap fields, and send
            sendCoapResponseFromOnem2mResponse(coapExchange, onem2mResponse);

        }

        private void sendCoapResponseFromOnem2mResponse(CoapExchange exchange, ResponsePrimitive onem2mResponse) {
            OptionSet options = new OptionSet();

            // the content is already in the required format ...
            String content = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            CoAP.ResponseCode coapRSC = mapCoreResponseToCoapResponse(rscString);
            // return the request id in the return option
            String rqi = onem2mResponse.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER);
            if (rqi != null) {
                options.addOption(new Option(Onem2m.CoapOption.ONEM2M_RQI, rqi));
            }
            // put the onem2m response code into the RSC option and return it too
            options.addOption(new Option(Onem2m.CoapOption.ONEM2M_RSC, Integer.parseInt(rscString)));

            // prepare response to be sent
            Response response = new Response(coapRSC);

            // set content and content format if exist
            if (content != null) {
                response.setPayload(content);
                String contentFormat = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT_FORMAT);
                if (null != contentFormat) {
                    switch (contentFormat) {
                        case Onem2m.ContentFormat.JSON:
                            options.setContentFormat(Onem2m.CoapContentFormat.APP_VND_RES_JSON);
                            break;
                        case Onem2m.ContentFormat.XML:
                            options.setContentFormat(Onem2m.CoapContentFormat.APP_VND_RES_XML);
                            break;
                        default:
                            LOG.error("Unsupported content format in onem2m response: {}", contentFormat);
                            break;
                    }
                }
            }

            String contentLocation = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT_LOCATION);
            if (null != contentLocation && (! contentLocation.isEmpty())) {
                options.setLocationPath(Onem2m.translateUriFromOnem2m(contentLocation));
            }

            // use the prepared options in the response
            response.setOptions(options);
            exchange.respond(response);

            if (rscString.charAt(0) =='2') {
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_OK);
            } else {
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
            }

        }

        private CoAP.ResponseCode mapCoreResponseToCoapResponse(String rscString) {

            switch (rscString) {
                case Onem2m.ResponseStatusCode.OK:
                    return CoAP.ResponseCode.CONTENT;
                case Onem2m.ResponseStatusCode.CREATED:
                    return CoAP.ResponseCode.CREATED;
                case Onem2m.ResponseStatusCode.CHANGED:
                    return CoAP.ResponseCode.CHANGED;
                case Onem2m.ResponseStatusCode.DELETED:
                    return CoAP.ResponseCode.DELETED;

                case Onem2m.ResponseStatusCode.NOT_FOUND:
                    return CoAP.ResponseCode.NOT_FOUND;
                case Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED:
                    return CoAP.ResponseCode.METHOD_NOT_ALLOWED;
                case Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE:
                    return CoAP.ResponseCode.BAD_REQUEST;
                case Onem2m.ResponseStatusCode.CONFLICT:
                    return CoAP.ResponseCode.FORBIDDEN;

                case Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR:
                    return CoAP.ResponseCode.INTERNAL_SERVER_ERROR;
                case Onem2m.ResponseStatusCode.NOT_IMPLEMENTED:
                    return CoAP.ResponseCode.NOT_IMPLEMENTED;
                case Onem2m.ResponseStatusCode.ALREADY_EXISTS:
                    return CoAP.ResponseCode.BAD_REQUEST;
                case Onem2m.ResponseStatusCode.TARGET_NOT_SUBSCRIBABLE:
                    return CoAP.ResponseCode.FORBIDDEN;
                case Onem2m.ResponseStatusCode.NON_BLOCKING_REQUEST_NOT_SUPPORTED:
                    return CoAP.ResponseCode.INTERNAL_SERVER_ERROR;

                case Onem2m.ResponseStatusCode.INVALID_ARGUMENTS:
                    return CoAP.ResponseCode.BAD_REQUEST;
                case Onem2m.ResponseStatusCode.INSUFFICIENT_ARGUMENTS:
                    return CoAP.ResponseCode.BAD_REQUEST;
            }
            return CoAP.ResponseCode.BAD_REQUEST;
        }
    }

    // implement the Onem2mNotifierPlugin interface
    @Override
    public String getNotifierPluginName() {
        return this.PLUGINNAME;
    }

    @Override
    public void sendNotification(String url, String payload) {
        Request request = Request.newPost();
        request.setURI(url);
        request.setPayload(payload);
        request.send();
        LOG.debug("CoAP: Send notification uri: {}, payload: {}:", url, payload);
    }

    @Override
    public String getRouterPluginName() {
        return this.PLUGINNAME;
    }

    /**
     * Translates Onem2m request into CoAP requests.
     * Sends the request to the next hop and returns Onem2m response.
     * @param onem2mRequest The request to be sent.
     * @param nextHopUrl URL of the next hop where the request will be
     *                   forwarded to
     * @return
     */
    @Override
    public ResponsePrimitive sendRequestBlocking(RequestPrimitive onem2mRequest, String nextHopUrl) {
        ResponsePrimitive onem2mResponseFailed = new ResponsePrimitive();
        onem2mResponseFailed.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER,
            onem2mResponseFailed.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER));

        // Translate the request from Onem2m to CoAP
        Request coapRequest = createCoapRequest(onem2mRequest, nextHopUrl);
        if (null == coapRequest) {
            // return response primitive with BAD_REQUEST error
            onem2mResponseFailed.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Forwarding of request failed");
            return onem2mResponseFailed;
        }

        // Send the request and wait for response
        try {
            coapRequest.send();
            Response coapResponse = coapRequest.waitForResponse();
            if (null == coapResponse) {
                LOG.error("Forwarding of request failed, no response received");
                onem2mResponseFailed.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE,
                                            "Forwarding failed, no response");
                return onem2mResponseFailed;
            }

            // translate the response to onem2m response
            return translateResponseToOnem2m(coapResponse, onem2mResponseFailed);
        } catch (Exception e) {
            LOG.error("Forwarding of request failed: {}", e);
            onem2mResponseFailed.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE, "Forwarding failed");
            return onem2mResponseFailed;
        }
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

        String operation = onem2mRequest.getPrimitive(RequestPrimitive.OPERATION);
        if (null == operation) {
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
        String path = onem2mRequest.getPrimitive(RequestPrimitive.TO);
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
        if (null != onem2mRequest.getPrimitive(RequestPrimitive.CONTENT)) {
            String format =  onem2mRequest.getPrimitive(RequestPrimitive.CONTENT_FORMAT);
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
            request.setPayload(onem2mRequest.getPrimitive(RequestPrimitive.CONTENT));
        }

        // use the options
        request.setOptions(options);
        return request;
    }

    /**
     * Translates response from CoAP to Onem2m response primitive
     * @param response
     * @param onem2mResponseFailed
     * @return
     */
    private ResponsePrimitive translateResponseToOnem2m(@Nonnull Response response,
                                                        @Nonnull ResponsePrimitive onem2mResponseFailed) {
        ResponsePrimitive onem2mResponse = new ResponsePrimitive();

        OptionSet options = response.getOptions();
        if (null == options) {
            LOG.error("Invalid CoAP response without options");
            onem2mResponseFailed.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE,
                                        "Invalid response received from next hop");
            return onem2mResponseFailed;
        }

        // Walks all options and stores value according to the
        // definitions of the CoAP options
        for (Option opt : options.asSortedList()) {
            Onem2m.Onem2mCoapOptionDef optDef = Onem2m.getCoapOptionCoap(opt.getNumber());
            if (null == optDef) {
                continue;
            }

            if (optDef.valueIsString) {
                onem2mResponse.setPrimitive(optDef.onem2mId, opt.getStringValue());
            } else {
                onem2mResponse.setPrimitive(optDef.onem2mId, Integer.toString(opt.getIntegerValue()));
            }
        }

        // set content location if exists
        String contentLocation = options.getLocationPathString();
        if (null != contentLocation && (! contentLocation.isEmpty())) {
            onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT_LOCATION,
                                        Onem2m.translateUriToOnem2m(contentLocation));
        }

        // set content if exists
        String payload = response.getPayloadString();
        if (null != payload) {
            onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT, payload);

            // set content format
            switch (options.getContentFormat()) {
                case Onem2m.CoapContentFormat.APP_VND_RES_JSON:
                    onem2mResponse.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE,
                                                Onem2m.ContentType.APP_VND_RES_JSON);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, Onem2m.ContentFormat.JSON);
                    break;
                case Onem2m.CoapContentFormat.APP_VND_RES_XML:
                    onem2mResponse.setPrimitive(ResponsePrimitive.HTTP_CONTENT_TYPE,
                                                Onem2m.ContentType.APP_VND_RES_XML);
                    onem2mResponse.setPrimitive(ResponsePrimitive.CONTENT_FORMAT, Onem2m.ContentFormat.XML);
                    break;
                default:
                    LOG.trace("Unsupported CoAP content format in response from next hop: {}",
                              options.getContentFormat());
                    break;
            }
        }

        return onem2mResponse;
    }
}
