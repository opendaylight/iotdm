/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.odlclient;

import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdlOnem2mCoapRequestPrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(OdlOnem2mCoapRequestPrimitive.class);

    Request coapRequest;
    OptionSet optionsSet;
    String uriQueryString;

    public OdlOnem2mCoapRequestPrimitive() {
        optionsSet = new OptionSet();
        uriQueryString = "";
    }

}

