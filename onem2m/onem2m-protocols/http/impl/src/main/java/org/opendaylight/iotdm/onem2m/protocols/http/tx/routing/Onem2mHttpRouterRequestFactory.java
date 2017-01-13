/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx.routing;

import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClient;

/**
 * Implements the HTTP router plugin request factory.
 */
public class Onem2mHttpRouterRequestFactory implements Onem2mHttpRouterRequestAbstractFactory {

    private final boolean secureConnection;

    public Onem2mHttpRouterRequestFactory(final boolean secureConnection) {
        this.secureConnection = secureConnection;
    }

    @Override
    public Onem2mHttpRouterRequest createHttpRouterRequest(RequestPrimitive request, String nextHopUrl,
                                                           Onem2mHttpClient client) {
        return new Onem2mHttpRouterRequest(request, nextHopUrl, client, secureConnection);
    }
}
