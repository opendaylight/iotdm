/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification;

import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.Onem2mCoapClient;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxRequest;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Implements complete logic of the handling of notifications.
 * The handling is divided into several methods according to extended TxRequest.
 */
public class Onem2mCoapNotifierRequest extends Onem2mProtocolTxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapNotifierRequest.class);

     // Let's keep this data protected so they can be accessed by child classes.
    protected final String url;
    protected final String payload;
    protected final String cseBaseId;
    protected final Onem2mCoapClient client;
    protected final boolean secureConnection;

    public Onem2mCoapNotifierRequest(@Nonnull final String url,
                                     @Nonnull final String payload,
                                     final String cseBaseId,
                                     @Nonnull final Onem2mCoapClient client,
                                     final boolean secureConnection) {
        this.url = url;
        this.payload = payload;
        this.cseBaseId = cseBaseId;
        this.client = client;
        this.secureConnection = secureConnection;
    }

    @Override
    protected boolean preprocessRequest() {
        // nothing to do here
        return true;
    }

    @Override
    protected boolean translateRequestFromOnem2m() {
        //nothing to do here
        return true;
    }

    @Override
    protected boolean sendRequest() {
        Request request = Request.newPost();
        request.setURI(url);
        request.setPayload(payload);

        OptionSet options = new OptionSet();
        options.setContentFormat(Onem2m.CoapContentFormat.APP_VND_NTFY_JSON);
        options.addOption(new Option(Onem2m.CoapOption.ONEM2M_FR, "/" + cseBaseId));
        options.addOption(new Option(Onem2m.CoapOption.ONEM2M_RQI, Onem2mProtocolUtils.getNextRequestId()));
        request.setOptions(options);

        LOG.debug("CoAP: Send notification cseBaseId: {}, uri: {}, payload: {}:", url, payload, cseBaseId);
        try {
            client.send(request);
        } catch (IOException e) {
            LOG.error("Error occured when sending notification coap request.", e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean translateResponseToOnem2m() {
        // nothing to do here
        return true;
    }

    @Override
    protected void respondToOnem2mCore() {
        // nothing to do here
    }
}
