/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Common COAP client base class for COAP notifier and routing plugins.
 */
public abstract class Onem2mCoapClient implements Onem2mProtocolTxChannel<Onem2mCoapClientConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapClient.class);

    protected String pluginName = "coap";

    @Override
    public void start(Onem2mCoapClientConfiguration configuration)
            throws RuntimeException {
        //not needed to start as coap is on udp
    }

    @Override
    public void close() {
        //not needed to close as coap is on udp
    }

    /**
     * Executes given coap request.
     * @param rx Coap request including all data to be sent.
     * @throws IOException ioexception
     */
    public void send(Request rx) throws IOException {
        rx.send();
    }
}
