/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.websocket.rx;

import org.opendaylight.iotdm.onem2m.plugins.*;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.websocket.rev141210.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;

public class Onem2mWebsocketIotdmPlugin extends IotdmPlugin<IotdmPluginOnem2mBaseRequest, IotdmPluginOnem2mBaseResponse>
        implements Onem2mProtocolRxChannel<Onem2mWebsocketIotdmPluginConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mWebsocketIotdmPlugin.class);

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mRxRequestAbstractFactory<Onem2mWebsocketRxRequest, IotdmPluginOnem2mBaseRequest, IotdmPluginOnem2mBaseResponse> requestFactory;
    private final Onem2mService onem2mService;

    private SecurityLevel securityLevel = SecurityLevel.L0;
    private ServerConfig currentConfig = null;

    public Onem2mWebsocketIotdmPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                      @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mWebsocketRxRequest, IotdmPluginOnem2mBaseRequest, IotdmPluginOnem2mBaseResponse> requestFactory,
                                      @Nonnull final Onem2mService onem2mService) {
        super(Onem2mPluginManager.getInstance());
        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService = onem2mService;
    }

    @Override
    public String pluginName() {
        return "ws";
    }

    @Override
    public void start(Onem2mWebsocketIotdmPluginConfig configuration)
            throws IllegalArgumentException {
        if (Objects.isNull(configuration)) {
            throw new IllegalArgumentException("Starting Websocket server without configuration");
        }

        this.currentConfig = configuration;
        this.securityLevel = configuration.getServerSecurityLevel();

        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();
        mgr.registerPluginWebsocket(this, configuration.getServerPort(), Onem2mPluginManager.Mode.Exclusive, null);

        LOG.info("Started Websocket IoTDM plugin at port: {}",
                 configuration.getServerPort());
    }

    @Override
    public void close() {
        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();
        mgr.unregisterPlugin(this);

        LOG.info("Closed Websocket IoTDM plugin at port: {}",
                currentConfig.getServerPort());
    }

    @Override
    public void handle(IotdmPluginOnem2mBaseRequest request, IotdmPluginOnem2mBaseResponse response) {
        Onem2mWebsocketRxRequest rxRequest = requestFactory.createRxRequest(request, response, onem2mService, securityLevel);
        requestHandler.handleRequest(rxRequest);
    }
}
