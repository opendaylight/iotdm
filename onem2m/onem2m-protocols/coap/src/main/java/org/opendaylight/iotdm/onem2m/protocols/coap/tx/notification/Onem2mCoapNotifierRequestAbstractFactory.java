/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification;

import org.opendaylight.iotdm.onem2m.protocols.coap.tx.Onem2mCoapClient;

/**
 * Defines abstract factory for COAP Notifier TxRequests
 */
public interface Onem2mCoapNotifierRequestAbstractFactory {

    /**
     * Creates COAP Notifier TxRequest which can be handled by TxHandler.
     * @param url The destination URL where the request will be sent.
     * @param payload The payload to send.
     * @param client Onem2mCoapClient as TxChannel which will be used to
     *               send the request.
     * @param cseBaseId The CSE-ID of the cseBase sending the notification.
     * @return Created COAP notifier request.
     */
    Onem2mCoapNotifierRequest createCoapNotifierRequest(String url, String payload, String cseBaseId,
                                                        Onem2mCoapClient client);
}
