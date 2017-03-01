/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins.channels.coap;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static java.util.Objects.nonNull;


public class IotdmPluginCoapResponse implements IotdmPluginResponse {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmPluginCoapResponse.class);

    private String payload = null;
    private OptionSet options = new OptionSet();
    private CoAP.ResponseCode coapRSC = null;

    /**
     * Make sure you filled out return code and options before building response.
     * @return coap response
     * @throws IllegalArgumentException if no all required fields are filled
     */
    public Response buildCoapResponse() {
        if(nonNull(options) && !options.asSortedList().isEmpty() && nonNull(coapRSC)) {
            Response response = new Response(coapRSC);
            response.setPayload(payload);
            response.setOptions(options);
            return response;
        }
        else {
            throw new IllegalArgumentException("IotdmPluginCoapResponse: coap options or returnCode undefined.");
        }
    }

    /**
     * @param returnCode The coap return code value to be set.
     */
    @Override
    public void setReturnCode(int returnCode) {
        this.coapRSC = CoAP.ResponseCode.valueOf(returnCode);
    }

    @Override
    public void setResponsePayload(@Nonnull String responsePayload) {
        this.payload = responsePayload;
    }

    @Override
    public void setContentType(@Nonnull String contentType) {
        this.options.setContentFormat(Onem2m.CoapContentFormat.map2Int.get(contentType));
    }

    public void setOptions(OptionSet options) {
        this.options = options;
    }

    public void prepareErrorResponse(String onem2mRsc, @Nonnull String message) {
        this.coapRSC = mapCoreResponseToCoapResponse(onem2mRsc);
        this.payload = JsonUtils.put(new JSONObject(), "error", message).toString();
        this.options.setContentFormat(Onem2m.CoapContentFormat.APPLICATION_JSON);
        this.options.addOption(new Option(Onem2m.CoapOption.ONEM2M_RSC, Integer.parseInt(onem2mRsc)));
    }

    public void setRequestId(String id) {
        options.addOption(new Option(Onem2m.CoapOption.ONEM2M_RQI, id));
    }

    private void fromOnem2mResponseToCoap(ResponsePrimitive onem2mResponse) {
        // the content is already in the required format ...
        String content = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
        CoAP.ResponseCode coapRSC = mapCoreResponseToCoapResponse(rscString);
        // return the request id in the return option
        String rqi = onem2mResponse.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER);
        if (nonNull(rqi)) {
            this.setRequestId(rqi);
        }
        // put the onem2m response code into the RSC option and return it too
        options.addOption(new Option(Onem2m.CoapOption.ONEM2M_RSC, Integer.parseInt(rscString)));

        // prepare response to be sent
        this.setReturnCode(coapRSC.value);

        // set content and content format if exist
        if (nonNull(content)) {
            this.setResponsePayload(content);
            String contentFormat = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT_FORMAT);
            if (nonNull(contentFormat)) {
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
        if (nonNull(contentLocation) && (!contentLocation.isEmpty())) {
            options.setLocationPath(Onem2m.translateUriFromOnem2m(contentLocation));
        }
    }

    /**
     *
     * @param onem2mResponse The Onem2m response primitive.
     * @return true when finished without error
     */
    public boolean setFromResponsePrimitive(ResponsePrimitive onem2mResponse) {
        this.fromOnem2mResponseToCoap(onem2mResponse);
        return true;
    }

    private static CoAP.ResponseCode mapCoreResponseToCoapResponse(String rscString) {

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
            case Onem2m.ResponseStatusCode.NOT_ACCEPTABLE:
                return CoAP.ResponseCode.NOT_ACCEPTABLE;
        }
        return CoAP.ResponseCode.BAD_REQUEST;
    }
}

