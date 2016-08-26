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
 * Defines abstract factory for Router plugin TxRequests.
 */
public interface Onem2mHttpRouterRequestAbstractFactory {

    /**
     * Creates Router plugin HTTP TxRequest.
     * @param request The Onem2m request primitive to be sent.
     * @param nextHopUrl The URL of next hop.
     * @param client The HTTP client as TxChannel which is used
     *               to send the request.
     * @return Created HTTP router plugin request instance.
     */
    Onem2mHttpRouterRequest createHttpRouterRequest(RequestPrimitive request, String nextHopUrl,
                                                    Onem2mHttpClient client);
}
