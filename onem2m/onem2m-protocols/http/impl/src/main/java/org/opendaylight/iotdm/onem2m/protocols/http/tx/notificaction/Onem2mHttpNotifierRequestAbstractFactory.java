/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction;

import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClient;

/**
 * Defines abstract factory for HTTP Notifier TxRequests
 */
public interface Onem2mHttpNotifierRequestAbstractFactory {

    /**
     * Creates HTTP Notifier TxRequest which can be handled by TxHandler.
     * @param url The destination URL where the request will be sent.
     * @param payload The payload to send.
     * @param client Onem2mHttpClient as TxChannel which will be used to
     *               send the request.
     * @param cseBaseId The CSE-ID of the cseBase sending the notification.
     * @return Created HTTP notifier request.
     */
    Onem2mHttpNotifierRequest createHttpNotifierRequest(String url, String payload, String cseBaseId,
                                                        Onem2mHttpClient client);
}
