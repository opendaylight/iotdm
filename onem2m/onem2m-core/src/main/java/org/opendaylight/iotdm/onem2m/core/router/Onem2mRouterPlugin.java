/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.router;


import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;

/**
 * This interface should be implemented by protocol plugin in order to
 * provide functionality for the Onem2mRouterService.
 */
public interface Onem2mRouterPlugin {

    /**
     * Return's unique name of the protocol supported by the plugin.
     * Method should return string which is used in URLs.
     * @return Protocol name
     */
    String getRouterPluginName();

    /**
     * Sends request and waits for response which is returned. Method must be
     * implemented as thread safe.
     * @param request Onem2m request
     * @param nextHopUrl URL of the next hop where the request will be
     *                   forwarded to
     * @return Onem2m response
     */
    ResponsePrimitive sendRequestBlocking(RequestPrimitive request, String nextHopUrl);

}
