/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.rx;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmPluginCoapRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmPluginCoapResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static java.util.Objects.nonNull;

/**
 * Implements complete handling logic for Coap RxRequests.
 * The handling is divided into few methods according to extended
 * RxRequest class.
 */
public class Onem2mCoapRxRequest extends Onem2mProtocolRxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapRxRequest.class);

    /* Let's keep this data protected so they can be accessed by
     * Child classes
     */
    protected final IotdmPluginCoapRequest request;
    protected final IotdmPluginCoapResponse response;
    protected final Onem2mService onem2mService;
    protected final SecurityLevel securityLevel;

    protected Onem2mRequestPrimitiveClientBuilder clientBuilder = null;
    protected ResponsePrimitive onem2mResponse = null;

    public Onem2mCoapRxRequest(@Nonnull final IotdmPluginCoapRequest request,
                               @Nonnull final IotdmPluginCoapResponse response,
                               @Nonnull final Onem2mService onem2mService,
                               final SecurityLevel securityLevel) {

        this.request = request;
        this.response = response;
        this.onem2mService = onem2mService;
        this.securityLevel = securityLevel;
    }

    @Override
    protected boolean preprocessRequest() {
        return true;
    }

    @Override
    protected boolean translateRequestToOnem2m() {
        clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
        CoAP.Code code = request.getOriginalRequest().getRequest().getCode();

        clientBuilder.setProtocol(Onem2m.Protocol.COAP);

        Onem2mStats.getInstance().endpointInc(request.getOriginalRequest().getEndpoint().getAddress().getAddress().toString());
        Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS);

        if (request.getOptions().getContentFormat() == MediaTypeRegistry.APPLICATION_JSON) {
            clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
        } else if (request.getOptions().getContentFormat() == MediaTypeRegistry.APPLICATION_XML) {
            clientBuilder.setContentFormat(Onem2m.ContentFormat.XML);
        } else {
            clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
        }

        // Process all options
        boolean resourceTypePresent = false;
        boolean requestIdPresent = false;
        boolean originatorPresent = false;
        String resourceTypeOption = null;
        String resourceTypeQuery;

        for (Option opt : request.getOptions().asSortedList()) {
            switch (opt.getNumber()) {
                case Onem2m.CoapOption.ONEM2M_FR:
                    clientBuilder.setFrom(opt.getStringValue());
                    originatorPresent = true;
                    break;
                case Onem2m.CoapOption.ONEM2M_RQI:
                    clientBuilder.setRequestIdentifier(opt.getStringValue());
                    // Set request ID so it will be set also in case of error
                    response.setRequestId(opt.getStringValue());
                    requestIdPresent = true;
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
            }
        }

        // Originator and RequestID are mandatory parameters
        if (!requestIdPresent) {
            response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,"Request ID is missing.");
            Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
            return false;
        }

        if (!originatorPresent) {
            response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,"Request originator is missing.");
            Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
            return false;
        }

        // according to the spec, the uri query string can contain in short form, the
        // resourceType, responseType, result persistence,  Delivery Aggregation, Result Content,
        // M3 Boolean
        resourceTypePresent = clientBuilder.parseQueryStringIntoPrimitives(request.getOptions().getUriQueryString());
        if (resourceTypePresent && (nonNull(resourceTypeOption))) {
            // Resource type is set in query string and in the option as well,
            // verify if values are equal
            resourceTypeQuery = clientBuilder.getPrimitiveValue(RequestPrimitive.RESOURCE_TYPE);
            if (!resourceTypeOption.equals(resourceTypeQuery)) {
                LOG.error("Invalid request received, resource type set in option ({}) and query ({}) mismatch",
                        resourceTypeOption, resourceTypeQuery);
                response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                              "Resource type in option and query string mismatch");
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
                return false;
            }
        } else if (!resourceTypePresent && (null != resourceTypeOption)) {
            // resource type is not present in query string, use the value from option
            clientBuilder.setResourceType(resourceTypeOption);
            resourceTypePresent = true;
        }

        if (resourceTypePresent && code != CoAP.Code.POST) {
            response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                          "Specifying resource type not permitted.");
            Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
            return false;
        }

        // take the entire payload text and put it in the CONTENT field; it is the representation of the resource
        String cn = request.getPayLoad();
        if (nonNull(cn) && !cn.contentEquals("")) {
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
                response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST, "Unknown code: " + code);
                Onem2mStats.getInstance().inc(Onem2mStats.COAP_REQUESTS_ERROR);
                return false;
        }
        clientBuilder.setTo(request.getOptions().getUriPathString());

        return true;
    }

    @Override
    protected boolean processRequest() {
        // invoke the service request
        Onem2mRequestPrimitiveClient onem2mRequest = clientBuilder.build();
        onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService, securityLevel);
        return true;
    }

    @Override
    protected boolean translateResponseFromOnem2m() {
        String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
        if (rscString.charAt(0) =='2') {
            Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_OK);
        } else {
            Onem2mStats.getInstance().inc(Onem2mStats.HTTP_REQUESTS_ERROR);
        }
        return response.setFromResponsePrimitive(onem2mResponse);
    }

    @Override
    protected void respond() {
        // The response has already been filled with response data
    }
}
