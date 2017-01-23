/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.CoapsConfig;


/**
 * Configuration used by CoAP client.
 */
public class Onem2mCoapClientConfiguration {
    protected final boolean secureConnection;
    protected final CoapsConfig secureConnectionConfig;
    protected final boolean usePsk;

    public Onem2mCoapClientConfiguration(boolean secureConnection,
                                         boolean usePsk,
                                         CoapsConfig secureConnectionConfig) {
        this.secureConnection = secureConnection;
        this.secureConnectionConfig = secureConnectionConfig;
        this.usePsk = usePsk;
    }

    public boolean isUsePsk() {
        return usePsk;
    }

    public boolean isSecureConnection() {
        return secureConnection;
    }

    public CoapsConfig getSecureConnectionConfig() {
        return secureConnectionConfig;
    }
}
