/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http;

import org.opendaylight.iotdm.onem2m.protocols.common.utils.IotdmProtocolConfigGetter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.HttpsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.KeyStoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.TrustStoreConfig;

/**
 * Classes uses Java reflexions to allow conversions from
 * objects implementing the same attributes and related getter methods.
 * Can be used by modules extending the base HTTP provider and using
 * own configuration classes.
 * (This is useful because the configuration class is generated from yang model
 * but it doesn't implement any common interface.)
 */
public class Onem2mHttpSecureConnectionConfig extends HttpsConfig {
    public Onem2mHttpSecureConnectionConfig(Object config) {
        super();
        KeyStoreConfig keystore = new KeyStoreConfig();
        TrustStoreConfig truststore = new TrustStoreConfig();
        Object kConfig = null;
        Object tConfig = null;

        kConfig = IotdmProtocolConfigGetter.getAttribute(config, "getKeyStoreConfig", Object.class);
        if (null != kConfig) {
            keystore.setKeyStoreFile(
                    IotdmProtocolConfigGetter.getAttribute(kConfig, "getKeyStoreFile", String.class));
            keystore.setKeyStorePassword(
                    IotdmProtocolConfigGetter.getAttribute(kConfig, "getKeyStorePassword", String.class));
            keystore.setKeyManagerPassword(
                    IotdmProtocolConfigGetter.getAttribute(kConfig, "getKeyManagerPassword", String.class));
            this.setKeyStoreConfig(keystore);
        }

        tConfig = IotdmProtocolConfigGetter.getAttribute(config, "getTrustStoreConfig", Object.class);
        if (null != tConfig) {
            truststore.setTrustStoreFile(
                    IotdmProtocolConfigGetter.getAttribute(tConfig, "getTrustStoreFile", String.class));
            truststore.setTrustStorePassword(
                    IotdmProtocolConfigGetter.getAttribute(tConfig, "getTrustStorePassword", String.class));
            this.setTrustStoreConfig(truststore);
        }
    }
}
