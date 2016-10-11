/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.core.router.Onem2mRouterService;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierService;
import org.opendaylight.iotdm.onem2m.protocols.coap.rx.Onem2mCoapBaseIotdmPlugin;
import org.opendaylight.iotdm.onem2m.protocols.coap.rx.Onem2mCoapBaseIotdmPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.coap.rx.Onem2mCoapRxRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.Onem2mCoapClientConfiguration;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification.Onem2mCoapNotifierPlugin;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification.Onem2mCoapNotifierPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification.Onem2mCoapNotifierRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing.Onem2mCoapRouterPlugin;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing.Onem2mCoapRouterPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing.Onem2mCoapRouterRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mCoapProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapProvider.class);
    private final Onem2mCoapBaseIotdmPluginConfig serverConfig;

    private Onem2mCoapBaseIotdmPlugin onem2mCoapBaseIotdmPlugin = null;
    protected Onem2mCoapNotifierPluginConfig notifierConfig = null;
    protected Onem2mCoapRouterPluginConfig routerConfig = null;
    private Onem2mCoapNotifierPlugin notifierPlugin;
    private Onem2mCoapRouterPlugin routerPlugin;

    public Onem2mCoapProvider(Onem2mCoapBaseIotdmPluginConfig serverConfig, Onem2mCoapNotifierPluginConfig notifierConfig, Onem2mCoapRouterPluginConfig routerConfig) {
        this.serverConfig = serverConfig;
        this.notifierConfig = notifierConfig;
        this.routerConfig = routerConfig;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        Onem2mService onem2mService = session.getRpcService(Onem2mService.class);

        try {
            onem2mCoapBaseIotdmPlugin = new Onem2mCoapBaseIotdmPlugin(new Onem2mProtocolRxHandler(),
                    new Onem2mCoapRxRequestFactory(),
                    onem2mService);
            onem2mCoapBaseIotdmPlugin.start(this.serverConfig);
        } catch (Exception e) {
            LOG.error("Failed to start COAP server: {}", e);
        }

        try {
//            boolean secureConnection = false;
//            if (null != this.routerConfig && null != this.routerConfig.getSecureConnection()) {
//                secureConnection = this.routerConfig.getSecureConnection();
//            }
            Onem2mCoapClientConfiguration cfg = new Onem2mCoapClientConfiguration(); //TODO: empty configuration yet

            routerPlugin = new Onem2mCoapRouterPlugin(new Onem2mProtocolTxHandler(),
                                                      new Onem2mCoapRouterRequestFactory(false));
            routerPlugin.start(cfg);
            Onem2mRouterService.getInstance().pluginRegistration(routerPlugin);
        } catch (Exception e) {
            LOG.error("Failed to start router plugin: {}", e);
        }

        try {
//            boolean secureConnection = false;
//            if (null != this.notifierConfig && null != this.notifierConfig.getSecureConnection()) {
//                secureConnection = this.notifierConfig.getSecureConnection();
//            }
            Onem2mCoapClientConfiguration cfg = new Onem2mCoapClientConfiguration(); //TODO: empty configuration yet
            notifierPlugin = new Onem2mCoapNotifierPlugin(new Onem2mProtocolTxHandler(),
                                                          new Onem2mCoapNotifierRequestFactory(false));
            notifierPlugin.start(cfg);
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
