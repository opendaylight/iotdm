/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.HttpsConfig;

public class Onem2mHttpClientConfiguration {
    protected final boolean secureConnection;
    protected final HttpsConfig secureConnectionConfig;

    public Onem2mHttpClientConfiguration(boolean secureConnection,
                                         HttpsConfig secureConnectionConfig) {
        this.secureConnection = secureConnection;
        this.secureConnectionConfig = secureConnectionConfig;
    }

    public boolean isSecureConnection() {
        return secureConnection;
    }

    public HttpsConfig getSecureConnectionConfig() {
        return secureConnectionConfig;
    }
}
