/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class implements handling of requests to be sent.
 */
public class Onem2mProtocolTxHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mProtocolTxHandler.class);

    /**
     * Methods of TxRequest are called in correct order and result of every method
     * is verified. The respondToOnem2mCore() method is called in case of failure of
     * particular method and it is expected that all data needed to send response are
     * already set even in case of failure at particular step.
     * @param request The TxRequest including all data needed to handle received request.
     */
    public void handle(Onem2mProtocolTxRequest request) {
        boolean result = request.preprocessRequest();
        if (! result) {
            LOG.trace("Handling break at: Preprocess request");
            request.respondToOnem2mCore();
            return;
        }

        result = request.translateRequestFromOnem2m();
        if (! result) {
            LOG.trace("Handling break at: Translate from Onem2m");
            request.respondToOnem2mCore();
            return;
        }

        result = request.sendRequest();
        if (! result) {
            LOG.trace("Handling break at: Send request");
            request.respondToOnem2mCore();
            return;
        }

        result = request.translateResponseToOnem2m();
        if (! result) {
            LOG.trace("Handling break at: Translate to Onem2m");
            request.respondToOnem2mCore();
            return;
        }

        request.respondToOnem2mCore();
    }
}
