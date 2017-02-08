/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap;

import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigException;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigValidator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.ServerConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.NotifierPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.RouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.CoapsConfig;

/**
 * Implements validation logic for Onem2mCoap protocol provider
 */
public class Onem2mCoapConfigurationValidator implements Onem2mProtocolConfigValidator {
    protected final ServerConfig serverConfig;
    protected final NotifierPluginConfig notifierConfig;
    protected final RouterPluginConfig routerConfig;
    protected final CoapsConfig secConfig;

    public Onem2mCoapConfigurationValidator(final ServerConfig serverConfig,
                                            final NotifierPluginConfig notifierConfig,
                                            final RouterPluginConfig routerConfig,
                                            final CoapsConfig secConfig) {
        this.serverConfig = serverConfig;
        this.notifierConfig = notifierConfig;
        this.routerConfig = routerConfig;
        this.secConfig = secConfig;
    }

    public void validateServerConfig() throws Onem2mProtocolConfigException {
        checkNotNull(serverConfig, "Server configuration not provided");
    }

    public void validateSecurityLevel() throws Onem2mProtocolConfigException {
        checkNotNull(serverConfig, "Server configuration not provided");

        checkCondition(serverConfig.getServerSecurityLevel() != SecurityLevel.L2,
                "Security level L2 is not supported by this module");
    }

    public void validateNotifierConfig() throws Onem2mProtocolConfigException {
        if (null != notifierConfig &&
            null != notifierConfig.isSecureConnection() &&
            notifierConfig.isSecureConnection()) {

            if ((null == notifierConfig.isUsePresharedKeys()) ||
                (false == notifierConfig.isUsePresharedKeys())) {
                checkNotNull(secConfig.getDtlsCertificatesConfig(),
                        "Secure connection enabled for notifier but TrustStore is not configured");
                checkNotNull(secConfig.getDtlsCertificatesConfig().getTrustStoreConfig(),
                        "Secure connection enabled for notifier but TrustStore is not configured");
                checkNotNull(secConfig.getDtlsCertificatesConfig().getTrustStoreConfig().getTrustedCertificates(),
                        "Trust store configuration without list of trusted certificates");
            } else {
                checkNotNull(secConfig.getDtlsPskRemoteCse(),
                        "Secure connection using PSK enabled for notifier but PSK is not configured");
                checkNotNull(secConfig.getDtlsPskRemoteCse().getCsePsk(),
                        "PSK list not configured");
            }

        }
    }

    public void validateRouterConfig() throws Onem2mProtocolConfigException {
        if (null != routerConfig &&
            null != routerConfig.isSecureConnection() &&
            routerConfig.isSecureConnection()) {

            checkNotNull(secConfig,
                    "Secure connection enabled for router plugin but parameters are not configured");
            if (null == routerConfig.isUsePresharedKeys() || false == routerConfig.isUsePresharedKeys()) {
                checkNotNull(secConfig.getDtlsCertificatesConfig(),
                        "Secure connection enabled for router but TrustStore is not configured");
                checkNotNull(secConfig.getDtlsCertificatesConfig().getTrustStoreConfig(),
                        "Secure connection enabled for router but TrustStore is not configured");
                checkNotNull(secConfig.getDtlsCertificatesConfig().getTrustStoreConfig().getTrustedCertificates(),
                        "Trust store configuration without list of trusted certificates");
            } else {
                checkNotNull(secConfig.getDtlsPskRemoteCse(),
                        "Secure connection using PSK enabled for router but PSK is not configured");
                checkNotNull(secConfig.getDtlsPskRemoteCse().getCsePsk(),
                        "PSK list not configured");
            }
        }
    }

    public void validate() throws Onem2mProtocolConfigException {
        this.validateServerConfig();
        this.validateSecurityLevel();
        this.validateNotifierConfig();
        this.validateRouterConfig();
    }

}
