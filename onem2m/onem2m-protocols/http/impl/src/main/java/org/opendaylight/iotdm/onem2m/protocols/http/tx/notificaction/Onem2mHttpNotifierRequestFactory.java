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
 * Implements the HTTP Notifier request factory.
 */
public class Onem2mHttpNotifierRequestFactory implements Onem2mHttpNotifierRequestAbstractFactory {

    private final boolean secureConnection;

    public Onem2mHttpNotifierRequestFactory(final boolean secureConnection) {
        this.secureConnection = secureConnection;
    }

    @Override
    public Onem2mHttpNotifierRequest createHttpNotifierRequest(String url, String payload, String cseBaseId,
                                                               Onem2mHttpClient client) {
        return new Onem2mHttpNotifierRequest(url, payload, cseBaseId, client, secureConnection);
    }
}
