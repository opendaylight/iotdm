/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mResponsePrimitiveClient extends RequestPrimitive {

    private String to;
    private String from;
    private String responseStatusCode;
    private String originatingTimestamp;
    private String requestIdentifier;
    private String contentFormat;
    private String content;
    private String resultExpirationTimestamp;

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mResponsePrimitiveClient.class);

    public Onem2mResponsePrimitiveClient() {
        super();
    }

}

