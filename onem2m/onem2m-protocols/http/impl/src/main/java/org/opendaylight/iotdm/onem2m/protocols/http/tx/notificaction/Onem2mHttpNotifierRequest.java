/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.http.HttpSchemes;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxRequest;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolUtils;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Implements complete logic of the handling of notifications.
 * The handling is divided into several methods according to extended TxRequest.
 */
public class Onem2mHttpNotifierRequest extends Onem2mProtocolTxRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpNotifierRequest.class);

     // Let's keep this data protected so they can be accessed by child classes.
    protected final String url;
    protected final String payload;
    protected final String cseBaseId;
    protected final Onem2mHttpClient client;
    protected final boolean secureConnection;
    protected ContentExchange ex = null;

    public Onem2mHttpNotifierRequest(@Nonnull final String url,
                                     @Nonnull final String payload,
                                     final String cseBaseId,
                                     @Nonnull final Onem2mHttpClient client,
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
        ex = new ContentExchange();
        ex.setURL(url);
        ex.setRequestContentSource(new ByteArrayInputStream(payload.getBytes()));
        ex.setRequestContentType(Onem2m.ContentType.APP_VND_NTFY_JSON);
        Integer cl = payload != null ?  payload.length() : 0;
        ex.setRequestHeader("Content-Length", cl.toString());
        ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_ORIGIN, ("/"+ this.cseBaseId));
        ex.setRequestHeader(Onem2m.HttpHeaders.X_M2M_RI, Onem2mProtocolUtils.getNextRequestId());
        ex.setMethod("post");
        return true;
    }

    @Override
    protected boolean sendRequest() {
        if (this.secureConnection) {
            ex.setScheme(HttpSchemes.HTTPS);
        }

        LOG.debug("HTTP(S): Send notification uri: {}, payload: {}:", url, payload);
        try {
            client.send(ex);
        } catch (IOException e) {
            LOG.error("Dropping notification: uri: {}, payload: {}", url, payload);
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
        return;
    }
}
