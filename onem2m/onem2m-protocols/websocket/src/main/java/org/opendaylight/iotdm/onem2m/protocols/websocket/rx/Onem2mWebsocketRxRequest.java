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
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.websocket.IotdmPluginWebsocketRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.websocket.IotdmPluginWebsocketResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxRequest;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;

import static java.util.Objects.nonNull;

/**
 * Implements complete handling logic for Websocket RxRequests.
 * The handling is divided into few methods according to extended
 * RxRequest class.
 */
class Onem2mWebsocketRxRequest extends Onem2mProtocolRxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mWebsocketRxRequest.class);

    private final IotdmPluginWebsocketRequest request;
    private final IotdmPluginWebsocketResponse response;
    private final Onem2mService onem2mService;
    private final SecurityLevel securityLevel;
    private String rxPayloadPrimitive;
    private Onem2mRequestPrimitiveClientBuilder onem2mRxBuilder = null;
    private ResponsePrimitive onem2mResponse = null;

    Onem2mWebsocketRxRequest(@Nonnull final IotdmPluginWebsocketRequest request,
                             @Nonnull final IotdmPluginWebsocketResponse response,
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
           response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST, "Not supported content format");
            return false;
        }
        rxPayloadPrimitive = Onem2m.getRqpJsonPrimitive(request.getOriginalRequest());
        return true;
    }

    @Override
    protected boolean translateRequestToOnem2m() {
        onem2mRxBuilder = new Onem2mRequestPrimitiveClientBuilder();
        onem2mRxBuilder.setProtocol(Onem2mPluginManager.ProtocolWebsocket);
        String operation = Onem2mProtocolUtils.processRequestPrimitiveFromJson(rxPayloadPrimitive, onem2mRxBuilder);
        return nonNull(operation);
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
