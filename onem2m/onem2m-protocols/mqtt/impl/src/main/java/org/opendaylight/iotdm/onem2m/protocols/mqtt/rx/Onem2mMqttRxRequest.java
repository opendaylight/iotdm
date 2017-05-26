/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.rx;

import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClient;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.common.IotdmPluginOnem2mBaseRequest;
import org.opendaylight.iotdm.onem2m.commchannels.common.IotdmPluginOnem2mBaseResponse;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.Onem2mStats;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxRequest;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

import javax.annotation.Nonnull;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements complete handling logic for MQTT RxRequests.
 * The handling is divided into few methods according to extended
 * RxRequest class.
 */
public class Onem2mMqttRxRequest extends Onem2mProtocolRxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttRxRequest.class);

    /* Let's keep this data protected so they can be accessed by
     * Child classes
     */
    protected final IotdmPluginOnem2mBaseRequest request;
    protected final IotdmPluginOnem2mBaseResponse response;
    protected final Onem2mService onem2mService;
    protected final SecurityLevel securityLevel;

    protected Onem2mRequestPrimitiveClientBuilder clientBuilder = null;
    protected ResponsePrimitive onem2mResponse = null;
    private String rxPayloadPrimitive;

    public Onem2mMqttRxRequest(@Nonnull final IotdmPluginOnem2mBaseRequest request,
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
        Onem2mStats.getInstance().inc(Onem2mStats.MQTT_REQUESTS);

        Optional<String> contentFormat = Onem2m.resolveContentFormat(request.getContentType());
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
        clientBuilder = new Onem2mRequestPrimitiveClientBuilder();
        clientBuilder.setProtocol(Onem2m.Protocol.MQTT);
        if (! Onem2mProtocolUtils.processRequestPrimitiveFromJson(rxPayloadPrimitive, clientBuilder)) {
            LOG.error("Failed to process request JSON content");
            String rqi = clientBuilder.getPrimitiveValue(RequestPrimitive.REQUEST_IDENTIFIER);
            response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                          "Failed to process the JSON content", rqi);
            return false;
        }

        String error = Onem2mProtocolUtils.verifyRequestPrimitive(clientBuilder);
        if (null != error) {
            LOG.error("Request verification failed: {}", error);
            String rqi = clientBuilder.getPrimitiveValue(RequestPrimitive.REQUEST_IDENTIFIER);
            response.prepareErrorResponse(Onem2m.ResponseStatusCode.BAD_REQUEST, error, rqi);
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

    @Override
    protected boolean translateResponseFromOnem2m() {
        // build json payload from onem2m response primitive
        response.setFromResponsePrimitive(onem2mResponse);
        return true;
    }

    @Override
    protected void respond() {
        // The response has already been filled by response data
    }
}
