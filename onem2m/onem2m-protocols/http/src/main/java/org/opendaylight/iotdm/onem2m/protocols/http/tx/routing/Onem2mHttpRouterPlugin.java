/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx.routing;

import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterPlugin;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClient;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Onem2mHttpRouterPlugin extends Onem2mHttpClient implements Onem2mRouterPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpRouterPlugin.class);

    protected final Onem2mProtocolTxHandler onem2mTxHandler;
    protected final Onem2mHttpRouterRequestAbstractFactory requestFactory;

    public Onem2mHttpRouterPlugin(@Nonnull final Onem2mProtocolTxHandler onem2mTxHandler,
                                  @Nonnull final Onem2mHttpRouterRequestAbstractFactory requestFactory) {
        super();
        this.onem2mTxHandler = onem2mTxHandler;
        this.requestFactory = requestFactory;
    }

    @Override
    public String getRouterPluginName() { return this.pluginName; }

    /**
     * Implements method of Onem2mRouterPlugin, sends request to the host specified by nextHopUrl.
     * Method doesn't use any authentication method.
     * @param request The request to be sent.
     * @param nextHopUrl The URL of the next hop, is used as value of Host header.
     * @param cseBaseCseId The CSE-ID of cseBase sending the request.
     * @return Response to he request is returned.
     */
    @Override
    public ResponsePrimitive sendRequestBlocking(RequestPrimitive request, String nextHopUrl,
                                                 String cseBaseCseId) {
        Onem2mHttpRouterRequest req = requestFactory.createHttpRouterRequest(request, nextHopUrl, this);
        onem2mTxHandler.handle(req);
        return req.getResponse();
    }
}
