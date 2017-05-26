/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierService;
import org.opendaylight.iotdm.onem2m.protocols.coap.rx.Onem2MCoapBaseIotdmHandlerPlugin;
import org.opendaylight.iotdm.onem2m.protocols.coap.rx.Onem2mCoapRxRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.Onem2mCoapClientConfiguration;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification.Onem2mCoapNotifierPlugin;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification.Onem2mCoapNotifierRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing.Onem2mCoapRouterPlugin;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing.Onem2mCoapRouterRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.ServerConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.NotifierPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.RouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.protocol.provider.config.CoapsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.Onem2mProtocolCoapProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mCoapProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapProvider.class);

    protected Onem2mService onem2mService;

    protected Onem2MCoapBaseIotdmHandlerPlugin onem2mCoapBaseIotdmPlugin = null;
    protected Onem2mCoapRouterPlugin routerPlugin;
    protected Onem2mCoapNotifierPlugin notifierPlugin;

    protected final ServerConfig serverConfig;
    protected final NotifierPluginConfig notifierConfig;
    protected final RouterPluginConfig routerConfig;
    protected final CoapsConfig secConfig;
    protected final Onem2mProtocolCoapProviders configuration;

    public Onem2mCoapProvider(RpcProviderRegistry rpcRegistry,
                              Onem2mProtocolCoapProviders config) throws Onem2mProtocolConfigException {
        // Validate the CoAP configuration
        Onem2mCoapConfigurationValidator validator =
                new Onem2mCoapConfigurationValidator(config.getServerConfig(), config.getNotifierPluginConfig(),
                        config.getRouterPluginConfig(), config.getCoapsConfig());
        try {
            validator.validate();
        } catch (Onem2mProtocolConfigException e) {
            LOG.error("Invalid CoAP configuration passed: {}", e);
            throw e;
        }

        onem2mService = rpcRegistry.getRpcService(Onem2mService.class);
        this.serverConfig = config.getServerConfig();
        this.notifierConfig = config.getNotifierPluginConfig();
        this.routerConfig = config.getRouterPluginConfig();
        this.secConfig = config.getCoapsConfig();
        this.configuration = config;
    }

    public void init() {

        try {
            onem2mCoapBaseIotdmPlugin = new Onem2MCoapBaseIotdmHandlerPlugin(new Onem2mProtocolRxHandler(),
                                                                             new Onem2mCoapRxRequestFactory(),
                                                                             onem2mService,
                                                                             configuration,
                                                                             "coap(s)-base");
            onem2mCoapBaseIotdmPlugin.start();
        } catch (Exception e) {
            LOG.error("Failed to start COAP server: {}", e);
        }

        try {
            boolean secureConnection = false;
            boolean usePsk = false;
            if (null != this.routerConfig && null != this.routerConfig.isSecureConnection()) {
                secureConnection = this.routerConfig.isSecureConnection();
                if (null != this.routerConfig.isUsePresharedKeys()) {
                    usePsk = this.routerConfig.isUsePresharedKeys();
                }
            }

            Onem2mCoapClientConfiguration cfg =
                    new Onem2mCoapClientConfiguration(secureConnection, usePsk, secConfig);

            routerPlugin = new Onem2mCoapRouterPlugin(new Onem2mProtocolTxHandler(),
                                                      new Onem2mCoapRouterRequestFactory(false),
                                                      cfg);
            routerPlugin.start();
            Onem2mRouterService.getInstance().pluginRegistration(routerPlugin);
        } catch (Exception e) {
            LOG.error("Failed to start router plugin: {}", e);
        }

        try {
            boolean secureConnection = false;
            boolean usePsk = false;
            if (null != this.notifierConfig && null != this.notifierConfig.isSecureConnection()) {
                secureConnection = this.notifierConfig.isSecureConnection();
                if (null != this.routerConfig.isUsePresharedKeys()) {
                    usePsk = this.routerConfig.isUsePresharedKeys();
                }
            }
            Onem2mCoapClientConfiguration cfg =
                    new Onem2mCoapClientConfiguration(secureConnection, usePsk, this.secConfig);
            notifierPlugin = new Onem2mCoapNotifierPlugin(new Onem2mProtocolTxHandler(),
                                                          new Onem2mCoapNotifierRequestFactory(false),
                                                          cfg);
            notifierPlugin.start();
            Onem2mNotifierService.getInstance().pluginRegistration(notifierPlugin);
        } catch (Exception e) {
            LOG.error("Failed to start notifier plugin: {}", e);
        }
        LOG.info("Onem2mCoapProvider Session Initiated");
    }

    @Override
    public void close() {
        try {
            onem2mCoapBaseIotdmPlugin.close();
        } catch (Exception e) {
            LOG.error("Failed to close coap server: {}", e);
        }

        try {
            notifierPlugin.close();
        } catch (Exception e) {
            LOG.error("Failed to close notifier plugin: {}", e);
        }

        try {
            routerPlugin.close();
        } catch (Exception e) {
            LOG.error("Failed to close router plugin {}", e);
        }
        Onem2mRouterService.getInstance().unregister(this.routerPlugin);
        LOG.info("Onem2mCoapProvider Closed");
    }
}
