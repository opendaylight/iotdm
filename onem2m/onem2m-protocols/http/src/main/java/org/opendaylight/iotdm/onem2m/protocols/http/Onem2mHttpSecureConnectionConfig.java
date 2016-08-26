/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http;

import org.opendaylight.iotdm.onem2m.protocols.common.utils.IoTdmProtocolConfigGetter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.HttpsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.KeyStoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.TrustStoreConfig;

public class Onem2mHttpSecureConnectionConfig extends HttpsConfig {
    public Onem2mHttpSecureConnectionConfig(Object config) {
        super();
        KeyStoreConfig keystore = new KeyStoreConfig();
        TrustStoreConfig truststore = new TrustStoreConfig();
        Object kConfig = null;
        Object tConfig = null;

        kConfig = IoTdmProtocolConfigGetter.getAttribute(config, "getKeyStoreConfig", Object.class);
        if (null != kConfig) {
            keystore.setKeyStoreFile(
                    IoTdmProtocolConfigGetter.getAttribute(kConfig, "getKeyStoreFile", String.class));
            keystore.setKeyStorePassword(
                    IoTdmProtocolConfigGetter.getAttribute(kConfig, "getKeyStorePassword", String.class));
            keystore.setKeyManagerPassword(
                    IoTdmProtocolConfigGetter.getAttribute(kConfig, "getKeyManagerPassword", String.class));
            this.setKeyStoreConfig(keystore);
        }

        tConfig = IoTdmProtocolConfigGetter.getAttribute(config, "getTrustStoreConfig", Object.class);
        if (null != tConfig) {
            truststore.setTrustStoreFile(
                    IoTdmProtocolConfigGetter.getAttribute(tConfig, "getTrustStoreFile", String.class));
            truststore.setTrustStorePassword(
                    IoTdmProtocolConfigGetter.getAttribute(tConfig, "getTrustStorePassword", String.class));
            this.setTrustStoreConfig(truststore);
        }
    }
}
