/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.websocket;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.websocket.rx.Onem2MWebsocketIotdmHandlerPlugin;
import org.opendaylight.iotdm.onem2m.protocols.websocket.rx.Onem2mWebsocketRxRequestFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.websocket.rev141210.Onem2mProtocolWebsocketProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mWebsocketProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mWebsocketProvider.class);
    protected final Onem2mProtocolWebsocketProviders configuration;

    protected Onem2mService onem2mService;

    protected Onem2MWebsocketIotdmHandlerPlugin onem2mWebsocketIotdmPlugin = null;

    public Onem2mWebsocketProvider(RpcProviderRegistry rpcRegistry,
                                   Onem2mProtocolWebsocketProviders configuration) {
        this.onem2mService = rpcRegistry.getRpcService(Onem2mService.class);
        this.configuration = configuration;
    }

    public void init() {
        try {
            onem2mWebsocketIotdmPlugin = new Onem2MWebsocketIotdmHandlerPlugin(new Onem2mProtocolRxHandler(),
                                                                               new Onem2mWebsocketRxRequestFactory(),
                                                                               onem2mService);
            onem2mWebsocketIotdmPlugin.start(this.configuration);
        } catch (Exception e) {
            LOG.error("Failed to start websocket server: {}", e);
        }
        LOG.info("Onem2mWebsocketProvider Session Initiated");
    }

    @Override
    public void close() {
        try {
            onem2mWebsocketIotdmPlugin.close();
        } catch (Exception e) {
            LOG.error("Failed to close websocket server: {}", e);
        }
        LOG.info("Onem2mWebsocketProvider Closed");
    }
}
