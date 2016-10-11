/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins.channels.coap;

import org.eclipse.californium.core.coap.*;
import org.eclipse.californium.core.network.Exchange;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRequest;

import javax.annotation.Nonnull;
import java.util.*;

public class IotdmPluginCoapRequest implements IotdmPluginRequest<Exchange> {
    private final Exchange exchange;
    private Request coapRequest = null;
    private String payload = null;

    IotdmPluginCoapRequest(@Nonnull final Exchange exchange) {
        this.exchange = exchange;
        this.coapRequest = exchange.getRequest();
    }

    @Override
    public String getOnem2mOperation() {
        CoAP.Code code = coapRequest.getCode();
        switch(code) {
            case GET:
                return Onem2m.Operation.RETRIEVE;
            case POST:
                return Onem2m.Operation.CREATE;
            case PUT:
                return Onem2m.Operation.UPDATE;
            case DELETE:
                return Onem2m.Operation.DELETE;
            default:
                return null;
        }
    }

    @Override
    public String getOnem2mUri() {
        return Onem2m.translateUriToOnem2m(coapRequest.getOptions().getUriPathString());
    }

    @Override
    public String getUrl() {
        return coapRequest.getOptions().getUriPathString();
    }

    @Override
    public String getMethod() {
        return coapRequest.getCode().name().toLowerCase();
    }

    @Override
    public String getPayLoad() {
        if (Objects.isNull(payload)) {
            payload = Optional.ofNullable(coapRequest.getPayloadString()).orElse("");
        }

        return payload;
    }

    void setPayLoad(String payload) {
        this.payload = payload;
    }

    @Override
    public String getContentType() {
        return Onem2m.CoapContentFormat.map2String.get(coapRequest.getOptions().getContentFormat());
    }

    @Override
    public Exchange getOriginalRequest() {
        return exchange;
    }

    public OptionSet getOptions() {
        return coapRequest.getOptions();
    }
}
