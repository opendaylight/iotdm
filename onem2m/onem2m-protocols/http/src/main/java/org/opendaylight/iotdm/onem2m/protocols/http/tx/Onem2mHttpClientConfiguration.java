/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx;

import org.opendaylight.iotdm.onem2m.protocols.http.Onem2mHttpSecureConnectionConfig;

public class Onem2mHttpClientConfiguration {
    protected final boolean secureConnection;
    protected final Onem2mHttpSecureConnectionConfig secureConnectionConfig;

    public Onem2mHttpClientConfiguration(boolean secureConnection,
                                         Onem2mHttpSecureConnectionConfig secureConnectionConfig) {
        this.secureConnection = secureConnection;
        this.secureConnectionConfig = secureConnectionConfig;
    }

    public boolean isSecureConnection() {
        return secureConnection;
    }

    public Onem2mHttpSecureConnectionConfig getSecureConnectionConfig() {
        return secureConnectionConfig;
    }
}
