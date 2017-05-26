/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http.api;

import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mKeyStoreFileConfig;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmRxAppProtocolConfigration;

/**
 * Class stores HTTPS server configuration.
 */
public class HttpsServerConfiguration extends Onem2mKeyStoreFileConfig
    implements IotdmRxAppProtocolConfigration {
    protected boolean compareConfig(HttpsServerConfiguration configuration) {
        if (configuration == null) {
            return false;
        }

        return super.compareConfig(configuration);
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof HttpsServerConfiguration)) {
            return false;
        }

        HttpsServerConfiguration config = (HttpsServerConfiguration) o;
        return this.compareConfig(config);
    }
}
