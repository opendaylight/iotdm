/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing;

import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.Onem2mCoapClient;

/**
 * Implements the COAP router plugin request factory.
 */
public class Onem2mCoapRouterRequestFactory implements Onem2mCoapRouterRequestAbstractFactory {

    private final boolean secureConnection;

    public Onem2mCoapRouterRequestFactory(final boolean secureConnection) {
        this.secureConnection = secureConnection;
    }

    @Override
    public Onem2mCoapRouterRequest createCoapRouterRequest(RequestPrimitive request, String nextHopUrl,
                                                           Onem2mCoapClient client) {
        return new Onem2mCoapRouterRequest(request, nextHopUrl, client, secureConnection);
    }
}
