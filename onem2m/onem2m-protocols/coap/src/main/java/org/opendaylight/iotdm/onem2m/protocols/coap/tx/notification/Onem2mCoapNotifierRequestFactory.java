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
 * Implements the COAP Notifier request factory.
 */
public class Onem2mCoapNotifierRequestFactory implements Onem2mCoapNotifierRequestAbstractFactory {

    private final boolean secureConnection;

    public Onem2mCoapNotifierRequestFactory(final boolean secureConnection) {
        this.secureConnection = secureConnection;
    }

    @Override
    public Onem2mCoapNotifierRequest createCoapNotifierRequest(String url, String payload, String cseBaseId,
                                                               Onem2mCoapClient client) {
        return new Onem2mCoapNotifierRequest(url, payload, cseBaseId, client, secureConnection);
    }
}
