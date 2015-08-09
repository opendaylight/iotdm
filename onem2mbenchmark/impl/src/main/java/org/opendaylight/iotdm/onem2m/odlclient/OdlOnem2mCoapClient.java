/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.odlclient;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdlOnem2mCoapClient {

    private static final Logger LOG = LoggerFactory.getLogger(OdlOnem2mCoapClient.class);

    private CoapClient coapClient;

    public OdlOnem2mCoapClient(String uri) {
        coapClient = new CoapClient(uri);
        coapClient.useCONs();
    }
    public CoapResponse sendRequest(OdlOnem2mCoapRequestPrimitive onem2mRequest) {
        return coapClient.advanced(onem2mRequest.coapRequest);
    }
}

