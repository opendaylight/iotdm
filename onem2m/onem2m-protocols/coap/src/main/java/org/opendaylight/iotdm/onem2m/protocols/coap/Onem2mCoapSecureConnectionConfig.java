/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap;

import org.opendaylight.iotdm.onem2m.protocols.common.utils.IotdmProtocolConfigGetter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.*;

import java.util.List;

/**
 * Classes uses Java reflexions to allow conversions from
 * objects implementing the same attributes and related getter methods.
 * Can be used by modules extending the base CoAP provider and using
 * own configuration classes.
 * (This is useful because the configuration class is generated from yang model
 * but it doesn't implement any common interface.)
 */
public class Onem2mCoapSecureConnectionConfig extends CoapsConfig {
    public Onem2mCoapSecureConnectionConfig(Object config) {
        super();
        DtlsCertificatesConfig certCfg = new DtlsCertificatesConfig();
        KeyStoreConfig keystore = new KeyStoreConfig();
        TrustStoreConfig truststore = new TrustStoreConfig();

        Object dtlsConfig = null;
        Object kConfig = null;
        Object tConfig = null;

        dtlsConfig = IotdmProtocolConfigGetter.getAttribute(config, "getDtlsCertificatesConfig", Object.class);
        if (null != dtlsConfig) {
            kConfig = IotdmProtocolConfigGetter.getAttribute(dtlsConfig, "getKeyStoreConfig", Object.class);
            if (null != kConfig) {
                keystore.setKeyStoreFile(
                        IotdmProtocolConfigGetter.getAttribute(kConfig, "getKeyStoreFile", String.class));
                keystore.setKeyStorePassword(
                        IotdmProtocolConfigGetter.getAttribute(kConfig, "getKeyStorePassword", String.class));
                keystore.setKeyManagerPassword(
                        IotdmProtocolConfigGetter.getAttribute(kConfig, "getKeyManagerPassword", String.class));
                keystore.setKeyAlias(
                        IotdmProtocolConfigGetter.getAttribute(kConfig, "getKeyAlias", String.class));
                certCfg.setKeyStoreConfig(keystore);
            }

            tConfig = IotdmProtocolConfigGetter.getAttribute(dtlsConfig, "getTrustStoreConfig", Object.class);
            if (null != tConfig) {
                truststore.setTrustStoreFile(
                        IotdmProtocolConfigGetter.getAttribute(tConfig, "getTrustStoreFile", String.class));
                truststore.setTrustStorePassword(
                        IotdmProtocolConfigGetter.getAttribute(tConfig, "getTrustStorePassword", String.class));
                truststore.setTrustedCertificates(
                        IotdmProtocolConfigGetter.getAttribute(tConfig, "getTrustedCertificates", List.class));
                certCfg.setTrustStoreConfig(truststore);
            }

            this.setDtlsCertificatesConfig(certCfg);
        }

        DtlsPskLocalCseBase localPsk = new DtlsPskLocalCseBase();
        DtlsPskRemoteCse remotePsk = new DtlsPskRemoteCse();

        Object lPsk = null;
        Object rPsk = null;

        lPsk = IotdmProtocolConfigGetter.getAttribute(config, "getDtlsPskLocalCseBase", Object.class);
        if (null != lPsk) {
            localPsk.setCsePsk(IotdmProtocolConfigGetter.getAttribute(lPsk, "getCsePsk", List.class));
            this.setDtlsPskLocalCseBase(localPsk);
        }

        rPsk = IotdmProtocolConfigGetter.getAttribute(config, "getDtlsPskRemoteCse", Object.class);
        if (null != rPsk) {
            remotePsk.setCsePsk(IotdmProtocolConfigGetter.getAttribute(rPsk, "getCsePsk", List.class));
            this.setDtlsPskRemoteCse(remotePsk);
        }
    }
}
