/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpChannelProviderService;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierService;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigException;
import org.opendaylight.iotdm.onem2m.protocols.http.rx.Onem2MHttpBaseIotdmHandlerPlugin;
import org.opendaylight.iotdm.onem2m.protocols.http.rx.Onem2mHttpRxRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClientConfiguration;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction.Onem2mHttpNotifierPlugin;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction.Onem2mHttpNotifierRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.routing.Onem2mHttpRouterPlugin;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.routing.Onem2mHttpRouterRequestFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.Onem2mProtocolHttpProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.HttpsConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.NotifierPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.RouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.http.protocol.provider.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpProvider.class);

    protected final Onem2mService onem2mService;
    protected final Onem2mHttpChannelProviderService channelProviderService;

    protected Onem2MHttpBaseIotdmHandlerPlugin onem2mHttpBaseIotdmPlugin = null;
    protected Onem2mHttpRouterPlugin routerPlugin = null;
    protected Onem2mHttpNotifierPlugin notifierPlugin = null;

    protected final Onem2mProtocolHttpProviders configuration;
    protected final ServerConfig serverConfig;
    protected final NotifierPluginConfig notifierConfig;
    protected final RouterPluginConfig routerConfig;
    protected final HttpsConfig secConfig;

    public Onem2mHttpProvider(final RpcProviderRegistry rpcRegistry,
                              final Onem2mProtocolHttpProviders config,
                              final Onem2mHttpChannelProviderService channelProviderService)
        throws Onem2mProtocolConfigException {

        // Validate the configuration
        Onem2mHttpConfigurationValidator validator =
            new Onem2mHttpConfigurationValidator(config.getServerConfig(), config.getNotifierPluginConfig(),
                                                 config.getRouterPluginConfig(), config.getHttpsConfig());
        try {
            validator.validate();
        } catch (Onem2mProtocolConfigException e) {
            LOG.error("Invalid configuration passed: {}", e);
            throw e;
        }

        this.onem2mService = rpcRegistry.getRpcService(Onem2mService.class);
        this.serverConfig = config.getServerConfig();
        this.notifierConfig = config.getNotifierPluginConfig();
        this.routerConfig = config.getRouterPluginConfig();
        this.secConfig = config.getHttpsConfig();
        this.configuration = config;
        this.channelProviderService = channelProviderService;
    }

    public void init() {

        try {
            onem2mHttpBaseIotdmPlugin = new Onem2MHttpBaseIotdmHandlerPlugin(new Onem2mProtocolRxHandler(),
                                                                             new Onem2mHttpRxRequestFactory(),
                                                                             onem2mService, configuration,
                                                                             channelProviderService);
            onem2mHttpBaseIotdmPlugin.start();
        } catch (Exception e) {
            LOG.error("Failed to start HTTP server: {}", e);
        }

        try {
            boolean secureConnection = false;
            if (null != this.routerConfig && null != this.routerConfig.isSecureConnection()) {
                secureConnection = this.routerConfig.isSecureConnection();
            }
            Onem2mHttpClientConfiguration cfg = new Onem2mHttpClientConfiguration(secureConnection,
                                                                                  this.secConfig);

            routerPlugin = new Onem2mHttpRouterPlugin(new Onem2mProtocolTxHandler(),
                                                      new Onem2mHttpRouterRequestFactory(secureConnection),
                                                      cfg);
            routerPlugin.start();
            Onem2mRouterService.getInstance().pluginRegistration(routerPlugin);
        } catch (Exception e) {
            LOG.error("Failed to start router plugin: {}", e);
        }

        try {
            boolean secureConnection = false;
            if (null != this.notifierConfig && null != this.notifierConfig.isSecureConnection()) {
                secureConnection = this.notifierConfig.isSecureConnection();
            }
            Onem2mHttpClientConfiguration cfg = new Onem2mHttpClientConfiguration(secureConnection,
                                                                                  this.secConfig);
            notifierPlugin = new Onem2mHttpNotifierPlugin(new Onem2mProtocolTxHandler(),
                                                          new Onem2mHttpNotifierRequestFactory(secureConnection),
                                                          cfg);
            notifierPlugin.start();
            Onem2mNotifierService.getInstance().pluginRegistration(notifierPlugin);
        } catch (Exception e) {
            LOG.error("Failed to start notifier plugin: {}", e);
        }
        LOG.info("Onem2mHttpProvider instance {}: Initialized", this.configuration.getHttpProviderInstanceName());
    }

    @Override
    public void close() {
        try {
            onem2mHttpBaseIotdmPlugin.close();
        } catch (Exception e) {
            LOG.error("Failed to close http server: {}", e);
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
        LOG.info("Onem2mHttpProvider instance {}: Closed", this.configuration.getHttpProviderInstanceName());
    }
}
