/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.rx;

import org.opendaylight.iotdm.onem2m.plugins.IotdmPlugin;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginConfigurationBuilderFactory;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmCoapsConfigBuilder;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmPluginCoapRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.coap.IotdmPluginCoapResponse;
import org.opendaylight.iotdm.onem2m.protocols.coap.Onem2mCoapSecureConnectionConfig;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.CsePsk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.KeyStoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Objects;

public class Onem2mCoapBaseIotdmPlugin implements IotdmPlugin<IotdmPluginCoapRequest, IotdmPluginCoapResponse>,
                                                  Onem2mProtocolRxChannel {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapBaseIotdmPlugin.class);

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mRxRequestAbstractFactory<Onem2mCoapRxRequest, IotdmPluginCoapRequest, IotdmPluginCoapResponse> requestFactory;
    private final Onem2mService onem2mService;

    private SecurityLevel securityLevel = SecurityLevel.L0;
    private ServerConfig currentConfig = null;
    private Onem2mCoapSecureConnectionConfig secureConnectionConfig = null;

    public Onem2mCoapBaseIotdmPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                     @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mCoapRxRequest, IotdmPluginCoapRequest, IotdmPluginCoapResponse> requestFactory,
                                     @Nonnull final Onem2mService onem2mService,
                                     final Onem2mCoapSecureConnectionConfig secureConnectionConfig) {
        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService = onem2mService;
        this.secureConnectionConfig = secureConnectionConfig;
    }

    @Override
    public String getPluginName() {
        return "coap(s)";
    }

    @Override
    public void start() {
        // TODO use this when migrated to Blueprint
        return;
    }

//    @Override
    public void start(Onem2mCoapBaseIotdmPluginConfig configuration)
            throws RuntimeException {
        if (Objects.isNull(configuration)) {
            throw new IllegalArgumentException("Starting Coap base server without configuration");
        }

        this.currentConfig = configuration;
        this.securityLevel = configuration.getServerSecurityLevel();

        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();

        if (null == configuration.getSecureConnection() || false == configuration.getSecureConnection()) {
            try {
                mgr.registerPluginCoap(this, configuration.getServerPort(), Onem2mPluginManager.Mode.Exclusive, null);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to register to PluginManager: {}", e);
            }
        } else {
            IotdmCoapsConfigBuilder builder = IotdmPluginConfigurationBuilderFactory.getNewCoapsConfigBuilder();
            if (null != this.secureConnectionConfig) {
                boolean usePsk = false;
                if (null != configuration.getUsePresharedKeys()) {
                    usePsk = configuration.getUsePresharedKeys();
                }

                if (usePsk) {
                    builder.setUsePsk(true);
                    if (this.secureConnectionConfig.getDtlsPskLocalCseBase() == null) {
                        builder.setUseDefault(true);
                    } else {
                        HashMap<String, String> localPsk = new HashMap<>();
                        for (CsePsk psk : this.secureConnectionConfig.getDtlsPskLocalCseBase().getCsePsk()) {
                            localPsk.put(psk.getCseId(), psk.getPsk());
                        }
                        builder.setPresharedKeys(localPsk);
                    }
                } else {
                    builder.setUsePsk(false);
                    if (this.secureConnectionConfig.getDtlsCertificatesConfig() == null ||
                        this.secureConnectionConfig.getDtlsCertificatesConfig().getKeyStoreConfig() == null) {
                        builder.setUseDefault(true);
                    } else {
                        KeyStoreConfig kConf =
                                this.secureConnectionConfig.getDtlsCertificatesConfig().getKeyStoreConfig();
                        builder
                            .setKeyStoreFile(kConf.getKeyStoreFile())
                            .setKeyStorePassword(kConf.getKeyStorePassword())
                            .setKeyManagerPassword(kConf.getKeyManagerPassword())
                            .setKeyAlias(kConf.getKeyAlias());
                    }
                }
            } else {
                builder.setUseDefault(true);
            }

            try {
                mgr.registerPluginCoaps(this, configuration.getServerPort(),
                                        Onem2mPluginManager.Mode.Exclusive, null, builder);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to register at PluginManager: {}", e);
            }
        }

        LOG.info("Started COAP Base IoTDM plugin at port: {}, security level: {}",
                configuration.getServerPort(), configuration.getServerSecurityLevel());
    }

    @Override
    public void close() {
        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();
        mgr.unregisterIotdmPlugin(this);

        LOG.info("Closed COAP Base IoTDM plugin at port: {}, security level: {}",
                currentConfig.getServerPort(), currentConfig.getServerSecurityLevel());
    }

    @Override
    public void handle(IotdmPluginCoapRequest request, IotdmPluginCoapResponse response) {
        Onem2mCoapRxRequest rxRequest =
                requestFactory.createRxRequest(request,
                        response,
                        onem2mService, securityLevel);
        requestHandler.handleRequest(rxRequest);
    }
}
