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
 * Class implements handling of received requests wrapped by RxRequest instance.
 */
public class Onem2mProtocolRxHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mProtocolRxHandler.class);

    /**
     * Methods of RxRequest are called in correct order and result of every method
     * is verified. The respond method is called in case of failure of particular method
     * and it is expected that all data needed to send response are already set even in case
     * of failure at particular step.
     * @param request The RxRequest including all data needed to handle received request.
     */
    public void handleRequest(Onem2mProtocolRxRequest request) {
        boolean result = request.preprocessRequest();
        if (! result) {
            LOG.trace("Handling break at: Preprocess request");
            request.respond();
            return;
        }

        result = request.translateRequestToOnem2m();
        if (! result) {
            LOG.trace("Handling break at: Translate request to Onem2m");
            request.respond();
            return;
        }

        result = request.processRequest();
        if (! result) {
            LOG.trace("Handling break at: Process request");
            request.respond();
            return;
        }

        result = request.translateResponseFromOnem2m();
        if (! result) {
            LOG.trace("Handling break at: Translate request from Onem2m");
            request.respond();
            return;
        }

        request.respond();
    }
}
