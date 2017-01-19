/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http;

import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigException;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigValidator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.HttpsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.NotifierPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.RouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.ServerConfig;

/*
 * NOTE: Although this validator is used by the Onem2mHttpProvider class only
 * we want to keep it in the impl module (bundle) because this way it can be
 * re-used in another implementation extending the base OneM2M HTTP implementation
 */

/**
 * Implements validation logic for Onem2mHttp protocol provider
 */
public class Onem2mHttpConfigurationValidator implements Onem2mProtocolConfigValidator {
    protected final ServerConfig serverConfig;
    protected final NotifierPluginConfig notifierConfig;
    protected final RouterPluginConfig routerConfig;
    protected final HttpsConfig secConfig;

    public Onem2mHttpConfigurationValidator(final ServerConfig serverConfig,
                                            final NotifierPluginConfig notifierConfig,
                                            final RouterPluginConfig routerConfig,
                                            final HttpsConfig secConfig) {
        this.serverConfig = serverConfig;
        this.notifierConfig = notifierConfig;
        this.routerConfig = routerConfig;
        this.secConfig = secConfig;
    }

    public void validateServerConfig() throws Onem2mProtocolConfigException {
        checkNotNull(serverConfig, "Server configuration not provided");

        // validate port number
        checkPortNumber(serverConfig.getServerPort());
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

            checkNotNull(secConfig, "Secure connection enabled for notifier but parameters are not configured");
            checkNotNull(secConfig.getTrustStoreConfig(),
                         "Secure connection enabled for notifier but TrustStore is not configured");
        }
    }

    public void validateRouterConfig() throws Onem2mProtocolConfigException {
        if (null != routerConfig && null != routerConfig.isSecureConnection() && routerConfig.isSecureConnection()) {
            checkNotNull(secConfig,
                         "Secure connection enabled for router plugin but parameters are not configured");
            checkNotNull(secConfig.getTrustStoreConfig(),
                         "Secure connection enabled for router plugin but TrustStore is not configured");
        }
    }

    public void validate() throws Onem2mProtocolConfigException {
        this.validateServerConfig();
        this.validateSecurityLevel();
        this.validateNotifierConfig();
        this.validateRouterConfig();
    }
}
