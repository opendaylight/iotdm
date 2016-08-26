/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierService;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;
import org.opendaylight.iotdm.onem2m.protocols.http.rx.Onem2mHttpBaseIotdmPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.http.rx.Onem2mHttpRxRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.http.rx.Onem2mHttpBaseIotdmPlugin;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClientConfiguration;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction.Onem2mHttpNotifierPlugin;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction.Onem2mHttpNotifierPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction.Onem2mHttpNotifierRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.routing.Onem2mHttpRouterPlugin;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.routing.Onem2mHttpRouterPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.routing.Onem2mHttpRouterRequestFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.NotifierPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.RouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpProvider.class);
    protected final Onem2mHttpBaseIotdmPluginConfig serverConfig;
    protected final Onem2mHttpNotifierPluginConfig notifierConfig;
    protected final Onem2mHttpRouterPluginConfig routerConfig;
    protected final Onem2mHttpSecureConnectionConfig secConfig;

    protected Onem2mService onem2mService;

    protected Onem2mHttpBaseIotdmPlugin onem2mHttpBaseIotdmPlugin = null;
    protected Onem2mHttpRouterPlugin routerPlugin = null;
    protected Onem2mHttpNotifierPlugin notifierPlugin = null;

    public Onem2mHttpProvider(Onem2mHttpBaseIotdmPluginConfig serverConfig,
                              Onem2mHttpNotifierPluginConfig notifierConfig,
                              Onem2mHttpRouterPluginConfig routerConfig,
                              Onem2mHttpSecureConnectionConfig secConfig) {
        this.serverConfig = serverConfig;
        this.notifierConfig = notifierConfig;
        this.routerConfig = routerConfig;
        this.secConfig = secConfig;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        onem2mService = session.getRpcService(Onem2mService.class);

        try {
            onem2mHttpBaseIotdmPlugin = new Onem2mHttpBaseIotdmPlugin(new Onem2mProtocolRxHandler(),
                                                                      new Onem2mHttpRxRequestFactory(),
                                                                      onem2mService,
                                                                      this.secConfig);
            onem2mHttpBaseIotdmPlugin.start(this.serverConfig);
        } catch (Exception e) {
            LOG.error("Failed to start HTTP server: {}", e);
        }

        try {
            boolean secureConnection = false;
            if (null != this.routerConfig && null != this.routerConfig.getSecureConnection()) {
                secureConnection = this.routerConfig.getSecureConnection();
            }
            Onem2mHttpClientConfiguration cfg = new Onem2mHttpClientConfiguration(secureConnection,
                                                                                  this.secConfig);

            routerPlugin = new Onem2mHttpRouterPlugin(new Onem2mProtocolTxHandler(),
                                                      new Onem2mHttpRouterRequestFactory(secureConnection));
            routerPlugin.start(cfg);
            Onem2mRouterService.getInstance().pluginRegistration(routerPlugin);
        } catch (Exception e) {
            LOG.error("Failed to start router plugin: {}", e);
        }

        try {
            boolean secureConnection = false;
            if (null != this.notifierConfig && null != this.notifierConfig.getSecureConnection()) {
                secureConnection = this.notifierConfig.getSecureConnection();
            }
            Onem2mHttpClientConfiguration cfg = new Onem2mHttpClientConfiguration(secureConnection,
                                                                                  this.secConfig);
            notifierPlugin = new Onem2mHttpNotifierPlugin(new Onem2mProtocolTxHandler(),
                                                          new Onem2mHttpNotifierRequestFactory(secureConnection));
            notifierPlugin.start(cfg);
            Onem2mNotifierService.getInstance().pluginRegistration(notifierPlugin);
        } catch (Exception e) {
            LOG.error("Failed to start notifier plugin: {}", e);
        }
        LOG.info("Onem2mHttpProvider Session Initiated");
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
        LOG.info("Onem2mHttpProvider Closed");
    }
}
