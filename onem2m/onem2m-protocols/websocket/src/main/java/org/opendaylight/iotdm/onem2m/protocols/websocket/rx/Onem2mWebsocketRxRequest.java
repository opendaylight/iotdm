/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.websocket.rx;

import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.common.IotdmPluginOnem2mBaseRequest;
import org.opendaylight.iotdm.onem2m.commchannels.common.IotdmPluginOnem2mBaseResponse;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginManager;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxRequest;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Implements complete handling logic for Websocket RxRequests.
 * The handling is divided into few methods according to extended
 * RxRequest class.
 */
class Onem2mWebsocketRxRequest extends Onem2mProtocolRxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mWebsocketRxRequest.class);

    private final IotdmPluginOnem2mBaseRequest request;
    private final IotdmPluginOnem2mBaseResponse response;
    private final Onem2mService onem2mService;
    private final SecurityLevel securityLevel;
    private String rxPayloadPrimitive;
    private Onem2mRequestPrimitiveClientBuilder onem2mRxBuilder = null;
    private ResponsePrimitive onem2mResponse = null;

    Onem2mWebsocketRxRequest(@Nonnull final IotdmPluginOnem2mBaseRequest request,
                             @Nonnull final IotdmPluginOnem2mBaseResponse response,
                             @Nonnull final Onem2mService onem2mService,
                             final SecurityLevel securityLevel) {

        this.request = request;
        this.response = response;
        this.onem2mService = onem2mService;
        this.securityLevel = securityLevel;
    }

    @Override
    protected boolean preprocessRequest() {
        String contentType = Optional.ofNullable(request.getContentType()).orElse("json").toLowerCase();
        Optional<String> contentFormat = Onem2m.resolveContentFormat(contentType);

        if(!contentFormat.isPresent() || contentFormat.get().equals(Onem2m.ContentFormat.XML)) {
           response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST, "Not supported content format",
                                         null);
           return false;
        }
        rxPayloadPrimitive = Onem2m.getRqpJsonPrimitive(request.getOriginalRequest());
        return true;
    }

    @Override
    protected boolean translateRequestToOnem2m() {
        onem2mRxBuilder = new Onem2mRequestPrimitiveClientBuilder();
        onem2mRxBuilder.setProtocol(IotdmPluginManager.ProtocolWebsocket);
        if (! Onem2mProtocolUtils.processRequestPrimitiveFromJson(rxPayloadPrimitive, onem2mRxBuilder)) {
            LOG.error("Failed to process request JSON content");
            String rqi = onem2mRxBuilder.getPrimitiveValue(RequestPrimitive.REQUEST_IDENTIFIER);
            response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                          "Failed to process the JSON content",
                                          rqi);
            return false;
        }

        String error = Onem2mProtocolUtils.verifyRequestPrimitive(onem2mRxBuilder);
        if (null != error) {
            LOG.error("Request verification failed: {}", error);
            String rqi = onem2mRxBuilder.getPrimitiveValue(RequestPrimitive.REQUEST_IDENTIFIER);
            response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST, error, rqi);
            return false;
        }
        return true;
    }

    @Override
    protected boolean processRequest() {
        // invoke the service request
        Onem2mRequestPrimitiveClient onem2mRequest = onem2mRxBuilder.build();
        onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService, securityLevel);
        return true;
    }

    @Override
    protected boolean translateResponseFromOnem2m() {
        // build json payload from onem2m response primitive
        response.setFromResponsePrimitive(onem2mResponse);
        return true;
    }

    @Override
    protected void respond() {
        // The response has already been filled with response data
    }
}
