/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.rx;

import org.opendaylight.iotdm.onem2m.commchannels.coap.IotdmCoapsConfigBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.coap.IotdmPluginCoapRequest;
import org.opendaylight.iotdm.onem2m.commchannels.coap.IotdmPluginCoapResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginManager;
import org.opendaylight.iotdm.plugininfra.commchannels.common.IotdmHandlerPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurable;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.commchannels.coap.rev170519.coaps.psk.config.CsePsk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.plugin.configuration.PluginSpecificConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.security.config.dtls.certificates.config.KeyStoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.CoapProtocolProviderConfig;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.plugin.configuration.plugin.specific.configuration.Onem2mCoapCoapsConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class Onem2MCoapBaseIotdmHandlerPlugin
    implements IotdmHandlerPlugin<IotdmPluginCoapRequest, IotdmPluginCoapResponse>,
               IotdmPluginConfigurable,
               Onem2mProtocolRxChannel {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2MCoapBaseIotdmHandlerPlugin.class);
    private final String pluginName;

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mRxRequestAbstractFactory<Onem2mCoapRxRequest, IotdmPluginCoapRequest, IotdmPluginCoapResponse> requestFactory;
    private final Onem2mService onem2mService;
    private final CoapProtocolProviderConfig pluginConfig;

    private SecurityLevel securityLevel = SecurityLevel.L0;

    public Onem2MCoapBaseIotdmHandlerPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                            @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mCoapRxRequest, IotdmPluginCoapRequest, IotdmPluginCoapResponse> requestFactory,
                                            @Nonnull final Onem2mService onem2mService,
                                            @Nonnull final CoapProtocolProviderConfig coapConfig,
                                            @Nonnull final String pluginName) {
        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService  = onem2mService;
        this.pluginConfig   = coapConfig;
        this.pluginName     = pluginName;
    }

    @Override
    public String getPluginName() {
        return this.pluginName;
    }

    @Override
    public void start() throws RuntimeException {

        this.securityLevel = pluginConfig.getServerConfig().getServerSecurityLevel();

        IotdmPluginManager mgr = IotdmPluginManager.getInstance();

        // Check whether CoAP or CoAPs is used
        if ((null == pluginConfig.getServerConfig().isSecureConnection()) ||
            (false == pluginConfig.getServerConfig().isSecureConnection())) {
            try {
                mgr.registerPluginCoap(this, pluginConfig.getServerConfig().getServerPort().getValue(),
                                       IotdmPluginManager.Mode.Exclusive, null);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to register to PluginManager: {}", e);
            }
        } else {
            IotdmCoapsConfigBuilder builder = new IotdmCoapsConfigBuilder();
            if (null != pluginConfig.getCoapsConfig()) {
                boolean usePsk = false;
                if (null != pluginConfig.getServerConfig().isUsePresharedKeys()) {
                    usePsk = pluginConfig.getServerConfig().isUsePresharedKeys();
                }

                if (usePsk) {
                    builder.setUsePsk(true);
                    if (pluginConfig.getCoapsConfig().getDtlsPskLocalCseBase() == null) {
                        builder.setUseDefault(true);
                    } else {
                        HashMap<String, String> localPsk = new HashMap<>();
                        for (CsePsk psk : pluginConfig.getCoapsConfig().getDtlsPskLocalCseBase().getCsePsk()) {
                            localPsk.put(psk.getCseId(), psk.getPsk());
                        }
                        builder.setPresharedKeys(localPsk);
                    }
                } else {
                    builder.setUsePsk(false);
                    if (pluginConfig.getCoapsConfig().getDtlsCertificatesConfig() == null ||
                            pluginConfig.getCoapsConfig().getDtlsCertificatesConfig().getKeyStoreConfig() == null) {
                        builder.setUseDefault(true);
                    } else {
                        KeyStoreConfig kConf =
                                pluginConfig.getCoapsConfig().getDtlsCertificatesConfig().getKeyStoreConfig();
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
                mgr.registerPluginCoaps(this, pluginConfig.getServerConfig().getServerPort().getValue(),
                                        IotdmPluginManager.Mode.Exclusive, null, builder);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to register at PluginManager: {}", e);
            }
        }

        LOG.info("Started COAP Base IoTDM plugin at port: {}, security level: {}",
                 pluginConfig.getServerConfig().getServerPort().getValue(),
                 pluginConfig.getServerConfig().getServerSecurityLevel());
    }

    @Override
    public void close() {
        IotdmPluginManager mgr = IotdmPluginManager.getInstance();
        mgr.unregisterIotdmPlugin(this);

        LOG.info("Closed COAP Base IoTDM plugin at port: {}, security level: {}",
                pluginConfig.getServerConfig().getServerPort().getValue(),
                pluginConfig.getServerConfig().getServerSecurityLevel());
    }

    @Override
    public void handle(IotdmPluginCoapRequest request, IotdmPluginCoapResponse response) {
        Onem2mCoapRxRequest rxRequest =
                requestFactory.createRxRequest(request,
                        response,
                        onem2mService, securityLevel);
        requestHandler.handleRequest(rxRequest);
    }

    @Override
    public PluginSpecificConfiguration getRunningConfig() {
        return new Onem2mCoapCoapsConfigBuilder()
                .setCoapsConfig(pluginConfig.getCoapsConfig())
                .setServerConfig(pluginConfig.getServerConfig())
                .setNotifierPluginConfig(pluginConfig.getNotifierPluginConfig())
                .setRouterPluginConfig(pluginConfig.getRouterPluginConfig())
                .build();
    }
}
